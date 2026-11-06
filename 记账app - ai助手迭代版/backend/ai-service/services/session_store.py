"""
会话历史存储 —— 基于 SQLite 的轻量持久化

职责:
    1. 保存用户的所有 AI 对话会话
    2. 每个会话关联多条消息（user/ai 交替）
    3. 支持按用户列出会话、查询某个会话的完整消息链、删除会话

为什么用 SQLite:
    - Python 标准库自带 sqlite3，零依赖
    - 单文件，方便 Docker 挂载 / 数据备份
    - 单用户 AI 助手场景下写入量很小，性能完全够
"""
from __future__ import annotations

import json
import sqlite3
import threading
from contextlib import contextmanager
from datetime import datetime
from pathlib import Path
from typing import Any, Optional

from loguru import logger


# ======================================================================
# 表结构
# ======================================================================
SCHEMA = """
CREATE TABLE IF NOT EXISTS session (
    id           TEXT PRIMARY KEY,          -- session_id (同时也作为 LangGraph thread_id)
    user_id      TEXT NOT NULL,
    title        TEXT NOT NULL DEFAULT '',   -- 自动从第一条用户消息截取
    last_message TEXT NOT NULL DEFAULT '',   -- 最后一条消息摘要（方便列表预览）
    created_at   TEXT NOT NULL,
    updated_at   TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_session_user ON session(user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS message (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id    TEXT NOT NULL REFERENCES session(id) ON DELETE CASCADE,
    role          TEXT NOT NULL,             -- 'user' / 'ai'
    content       TEXT NOT NULL,
    created_at    TEXT NOT NULL,
    tool_calls    TEXT,                      -- AI 工具调用链（JSON 可选）
    reply_to_id   INTEGER,                   -- 可选: AI 回复的是哪条 user msg
    UNIQUE(session_id, role, content, created_at)  -- 防重复写入
);

CREATE INDEX IF NOT EXISTS idx_message_session ON message(session_id, id ASC);
"""


class SessionStore:
    """
    线程安全的 SQLite 会话存储

    使用示例:
        store = SessionStore()
        store.save_message("sess_1", "user", "帮我记账")
        store.save_message("sess_1", "ai", "好的，已记 ...")
        sessions = store.list_sessions("user_1")
        msgs = store.list_messages("sess_1")
    """

    _instance: Optional["SessionStore"] = None
    _instance_lock = threading.Lock()

    def __init__(self, db_path: str | Path | None = None):
        from config.settings import settings

        if db_path is None:
            db_path = Path(__file__).resolve().parent.parent / "data" / "sessions.db"
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)

        self._lock = threading.RLock()
        self._conn = sqlite3.connect(
            str(self.db_path),
            check_same_thread=False,  # 多线程共享
        )
        self._conn.row_factory = sqlite3.Row
        self._conn.execute("PRAGMA foreign_keys = ON")
        self._conn.execute("PRAGMA journal_mode = WAL")  # 读写并发更快
        self._init_schema()
        logger.info(f"[SessionStore] SQLite 已连接: {self.db_path}")

    # ------------------------------------------------------------------
    # 单例
    # ------------------------------------------------------------------
    @classmethod
    def get_instance(cls, db_path: str | Path | None = None) -> "SessionStore":
        with cls._instance_lock:
            if cls._instance is None:
                cls._instance = cls(db_path)
            return cls._instance

    # ------------------------------------------------------------------
    # 内部工具
    # ------------------------------------------------------------------
    def _init_schema(self) -> None:
        with self._lock:
            self._conn.executescript(SCHEMA)
            self._conn.commit()

    @contextmanager
    def _cursor(self):
        with self._lock:
            cur = self._conn.cursor()
            try:
                yield cur
                self._conn.commit()
            except Exception:
                self._conn.rollback()
                raise
            finally:
                cur.close()

    @staticmethod
    def _now_iso() -> str:
        return datetime.now().isoformat(timespec="seconds")

    @staticmethod
    def _truncate(text: str, max_len: int = 50) -> str:
        return text if len(text) <= max_len else text[: max_len - 1] + "…"

    # ------------------------------------------------------------------
    # 会话 (session) CRUD
    # ------------------------------------------------------------------
    def ensure_session(self, session_id: str, user_id: str) -> None:
        """保证 session 记录存在（首次写消息时自动创建）"""
        now = self._now_iso()
        with self._cursor() as cur:
            cur.execute(
                """
                INSERT OR IGNORE INTO session (id, user_id, title, last_message, created_at, updated_at)
                VALUES (?, ?, '', '', ?, ?)
                """,
                (session_id, user_id, now, now),
            )

    def update_session(self, session_id: str, *, title: str | None = None,
                       last_message: str | None = None) -> None:
        fields: list[str] = []
        params: list[Any] = []
        if title is not None:
            fields.append("title = ?")
            params.append(self._truncate(title, 30))
        if last_message is not None:
            fields.append("last_message = ?")
            params.append(self._truncate(last_message, 80))
        if not fields:
            return
        params.append(self._now_iso())
        params.append(session_id)
        with self._cursor() as cur:
            cur.execute(
                f"UPDATE session SET {', '.join(fields)}, updated_at = ? WHERE id = ?",
                params,
            )

    def list_sessions(self, user_id: str, limit: int = 100) -> list[dict]:
        """按 updated_at 倒序列出用户所有会话"""
        with self._cursor() as cur:
            cur.execute(
                """
                SELECT id, title, last_message, created_at, updated_at
                FROM session
                WHERE user_id = ?
                ORDER BY updated_at DESC
                LIMIT ?
                """,
                (user_id, limit),
            )
            return [dict(row) for row in cur.fetchall()]

    def delete_session(self, session_id: str) -> bool:
        with self._cursor() as cur:
            cur.execute("DELETE FROM session WHERE id = ?", (session_id,))
            # SQLite 级联自动删除 message
            return cur.rowcount > 0

    # ------------------------------------------------------------------
    # 消息 (message) CRUD
    # ------------------------------------------------------------------
    def save_message(
        self,
        session_id: str,
        role: str,  # 'user' / 'ai'
        content: str,
        user_id: str = "",  # 可选，用于 ensure_session 自动建会话
        tool_calls: list[dict] | None = None,
    ) -> None:
        """保存单条消息，并更新 session 标题 / 最后消息摘要"""
        if user_id:
            self.ensure_session(session_id, user_id)

        now = self._now_iso()
        tool_calls_json = json.dumps(tool_calls, ensure_ascii=False) if tool_calls else None

        with self._cursor() as cur:
            # 用 INSERT OR IGNORE 防重复（同 session + role + content + created_at 唯一定位）
            cur.execute(
                """
                INSERT OR IGNORE INTO message
                    (session_id, role, content, created_at, tool_calls)
                VALUES (?, ?, ?, ?, ?)
                """,
                (session_id, role, content, now, tool_calls_json),
            )

        # 更新 session 元数据
        if role == "user":
            # 第一条用户消息用作标题
            existing = self._get_session_field(session_id, "title")
            if not existing:
                self.update_session(session_id, title=self._truncate(content, 30))
        self.update_session(session_id, last_message=content)

    def _get_session_field(self, session_id: str, field: str) -> str | None:
        with self._cursor() as cur:
            cur.execute(f"SELECT {field} FROM session WHERE id = ?", (session_id,))
            row = cur.fetchone()
            return row[0] if row else None

    def list_messages(self, session_id: str) -> list[dict]:
        """按 id 升序返回某个会话的所有消息"""
        with self._cursor() as cur:
            cur.execute(
                """
                SELECT id, role, content, created_at, tool_calls
                FROM message
                WHERE session_id = ?
                ORDER BY id ASC
                """,
                (session_id,),
            )
            rows = cur.fetchall()

        result = []
        for row in rows:
            d = dict(row)
            if d.get("tool_calls"):
                try:
                    d["tool_calls"] = json.loads(d["tool_calls"])
                except Exception:
                    d["tool_calls"] = None
            result.append(d)
        return result

    # ------------------------------------------------------------------
    # 统计 / 清理
    # ------------------------------------------------------------------
    def get_session_count(self, user_id: str) -> int:
        with self._cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM session WHERE user_id = ?", (user_id,))
            return cur.fetchone()[0]


def get_store() -> SessionStore:
    """全局存储单例"""
    return SessionStore.get_instance()
