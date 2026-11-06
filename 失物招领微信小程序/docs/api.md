# 校园失物招领小程序 - API 接口文档

## 1. 接口说明

### 1.1 基础信息

| 项目 | 值 |
|------|------|
| 基础路径 | `/api/v1` |
| 数据格式 | JSON（UTF-8） |
| 时间格式 | ISO 8601，例如 `2024-01-15T12:30:00.000Z` |
| 鉴权方式 | Bearer Token（需认证接口） |

### 1.2 统一响应格式

所有接口响应统一为以下格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "requestId": "uuid",
  "timestamp": "2024-01-15T12:30:00.000Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Number | HTTP 状态码，`200` 代表成功 |
| `message` | String | 接口处理说明 |
| `data` | Object/Array/Null | 业务数据 |
| `requestId` | String | 请求追踪 ID |
| `timestamp` | String | 响应时间（ISO 8601） |

### 1.3 分页响应格式

分页列表接口 `data` 中包含 `list`（列表）、`total`（总数）、`page`（当前页）、`pageSize`（页大小）。

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 1,
    "pageSize": 20
  },
  "requestId": "...",
  "timestamp": "..."
}
```

### 1.4 鉴权说明

需登录的接口必须在请求头中携带：

```
Authorization: Bearer <token>
```

管理员接口额外要求用户 `role = 'ADMIN'`。

### 1.5 统一错误码

| HTTP 状态码 | 说明 |
|-------------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 token 失效 |
| 403 | 无权限（如非管理员访问管理接口） |
| 404 | 资源不存在 |
| 409 | 状态冲突（如重复审核） |
| 500 | 服务器内部错误 |

---

## 2. 认证接口 `/auth`

### 2.1 微信登录 `POST /auth/wx-login`

无需鉴权。

**请求体**：
```json
{
  "code": "wx_js_code_xxxx",
  "userInfo": {
    "nickname": "小明",
    "avatarUrl": "https://..."
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | String | 是 | 微信登录 code |
| `userInfo.nickname` | String | 否 | 微信昵称 |
| `userInfo.avatarUrl` | String | 否 | 微信头像 |

**响应 data**：
```json
{
  "token": "eyJhbGc...",
  "user": {
    "id": 1,
    "nickname": "小明",
    "avatar": "https://...",
    "email": "xxx@school.edu.cn",
    "role": "USER",
    "emailVerified": true
  },
  "isNew": false
}
```

---

### 2.2 邮箱注册 `POST /auth/register`

无需鉴权。

**请求体**：
```json
{
  "email": "student@school.edu.cn",
  "password": "my_password",
  "code": "123456",
  "nickname": "小明"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 学业邮箱 |
| `password` | String | 是 | 密码（6-20 位） |
| `code` | String | 是 | 邮箱验证码 |
| `nickname` | String | 否 | 昵称 |

**响应 data**：
```json
{
  "token": "eyJhbGc...",
  "user": {
    "id": 5,
    "email": "student@school.edu.cn",
    "nickname": "小明",
    "role": "USER",
    "emailVerified": true
  }
}
```

---

### 2.3 邮箱密码登录 `POST /auth/password-login`

无需鉴权。

**请求体**：
```json
{
  "email": "student@school.edu.cn",
  "password": "my_password"
}
```

**响应 data**：同注册接口的 data。

---

### 2.4 发送邮箱验证码 `POST /auth/email-code/send`

无需鉴权。

**请求体**：
```json
{
  "email": "student@school.edu.cn",
  "purpose": "REGISTER"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱地址 |
| `purpose` | String | 是 | `REGISTER` / `RESET_PASSWORD` / `BIND_EMAIL` |

**响应 data**：`null`

---

### 2.5 刷新 Token `POST /auth/refresh-token`

无需鉴权。

**请求体**：
```json
{
  "token": "eyJhbGc..."
}
```

**响应 data**：
```json
{
  "token": "new_jwt_token"
}
```

---

### 2.6 获取当前用户 `GET /auth/me`

需登录（USER / ADMIN）。

**响应 data**：
```json
{
  "id": 1,
  "email": "student@school.edu.cn",
  "nickname": "小明",
  "avatar": "https://...",
  "phone": "13800138000",
  "signature": "乐于助人",
  "role": "USER",
  "emailVerified": true,
  "status": 1,
  "createdAt": "2024-01-10T08:00:00.000Z",
  "itemsCount": 5
}
```

---

### 2.7 更新当前用户信息 `PATCH /auth/me`

需登录。

**请求体**（至少包含一个字段）：
```json
{
  "nickname": "新昵称",
  "phone": "13900139000",
  "signature": "失物招领热心人",
  "avatarUrl": "https://..."
}
```

**响应 data**：更新后的用户对象（同 `/auth/me` 响应结构）。

---

### 2.8 获取我的发布列表 `GET /auth/me/items`

需登录。支持查询参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | Number | 1 | 页码 |
| `pageSize` | Number | 20 | 页大小 |
| `itemType` | String | - | `LOST` / `FOUND` |
| `auditStatus` | String | - | `PENDING` / `APPROVED` / `REJECTED` |
| `bizStatus` | String | - | `ONGOING` / `RESOLVED` / `RETURNED` / `CANCELLED` |

**响应 data**：分页列表（同 items 列表格式）。

---

### 2.9 检查登录状态 `GET /auth/check-status`

无需鉴权。一般用于前端预检。

---

## 3. 物品信息接口 `/items`

### 3.1 发布物品 `POST /items`

需登录。

**请求体**：
```json
{
  "itemType": "LOST",
  "title": "丢失了一个黑色双肩包",
  "category": "箱包",
  "happenTime": "2024-01-15T09:00:00.000Z",
  "location": "图书馆二楼自习区",
  "featureDesc": "黑色 Nike 双肩包，左侧口袋有白色污渍，内有一本高等数学教材",
  "contactName": "小明",
  "contactPhone": "13800138000",
  "contactWechat": "xiaoming_wx",
  "imageUrls": [
    "https://example.com/img1.jpg",
    "https://example.com/img2.jpg"
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `itemType` | String | 是 | `LOST`（寻物）/ `FOUND`（拾物） |
| `title` | String | 是 | 标题（最多 128 字符） |
| `category` | String | 是 | 分类（如：证件、电子产品、钥匙、箱包等） |
| `happenTime` | String | 是 | 发生时间（ISO 8601） |
| `location` | String | 是 | 地点（最多 128 字符） |
| `featureDesc` | String | 否 | 详细特征描述 |
| `contactName` | String | 否 | 联系人 |
| `contactPhone` | String | 否 | 联系电话 |
| `contactWechat` | String | 否 | 微信号 |
| `imageUrls` | Array\<String\> | 否 | 图片 URL 列表 |

**响应 data**：
```json
{
  "id": 10,
  "itemType": "LOST",
  "auditStatus": "PENDING",
  "bizStatus": "ONGOING"
}
```

> 创建后物品默认 `auditStatus = PENDING`，`bizStatus = ONGOING`，需管理员审核通过后才会在首页展示。

---

### 3.2 获取物品列表 `GET /items`

需登录。支持查询参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | Number | 1 | 页码 |
| `pageSize` | Number | 20 | 页大小 |
| `itemType` | String | - | `LOST` / `FOUND` |
| `keyword` | String | - | 搜索关键词（按标题+地点+描述） |
| `category` | String | - | 按分类筛选 |
| `sortBy` | String | `createdAt` | 排序字段 |
| `sortOrder` | String | `DESC` | `ASC` / `DESC` |

**响应 data（单条 item）**：
```json
{
  "id": 10,
  "userId": 1,
  "itemType": "LOST",
  "title": "黑色双肩包",
  "category": "箱包",
  "happenTime": "2024-01-15T09:00:00.000Z",
  "location": "图书馆二楼",
  "featureDesc": "黑色 Nike ...",
  "contactName": "小明",
  "contactPhone": "13800138000",
  "contactWechat": "xiaoming_wx",
  "auditStatus": "APPROVED",
  "bizStatus": "ONGOING",
  "viewCount": 42,
  "images": [
    { "id": 1, "url": "https://.../img1.jpg", "sort": 0 },
    { "id": 2, "url": "https://.../img2.jpg", "sort": 1 }
  ],
  "publisher": {
    "id": 1,
    "nickname": "小明",
    "avatar": "https://..."
  },
  "createdAt": "2024-01-15T10:15:00.000Z"
}
```

---

### 3.3 获取物品详情 `GET /items/{id}`

需登录。

**响应 data**：同列表单条 item 的完整结构（包含详情字段与图片列表和发布者信息）。

---

### 3.4 更新业务状态 `PATCH /items/{id}/biz-status`

需登录（仅本人或管理员）。

**请求体**：
```json
{
  "bizStatus": "RESOLVED"
}
```

| `bizStatus` 允许值 | 适用 itemType |
|-------------------|---------------|
| `RESOLVED` | LOST（已找到） |
| `RETURNED` | FOUND（已归还） |
| `CANCELLED` | LOST / FOUND（取消发布） |

**响应 data**：
```json
{
  "id": 10,
  "bizStatus": "RESOLVED"
}
```

---

### 3.5 更新物品（编辑本人未审核物品）`PUT /items/{id}`

需登录（仅本人且未被锁定）。

**请求体**：同 `POST /items`，允许部分字段覆盖。编辑成功后物品回到 `auditStatus = PENDING` 待审核。

---

## 4. 文件上传接口 `/files`

### 4.1 上传图片 `POST /files/upload`

需登录。`Content-Type: multipart/form-data`。

| 表单字段 | 类型 | 必填 | 说明 |
|---------|------|------|------|
| `file` | File | 是 | 图片文件（支持 jpg/png/gif，最大 5MB） |

**响应 data**：
```json
{
  "url": "https://example.com/uploads/xxx.jpg"
}
```

---

## 5. 消息与通知接口 `/messages`

### 5.1 创建或获取会话 `POST /messages/conversations`

需登录。

**请求体**：
```json
{
  "targetUserId": 5,
  "itemId": 10
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `targetUserId` | Number | 是 | 对方用户 ID（不能与当前用户相同） |
| `itemId` | Number | 是 | 关联物品 ID |

**响应 data**：
```json
{
  "conversationId": 3
}
```

---

### 5.2 获取会话列表 `GET /messages/conversations`

需登录。

| 查询参数 | 类型 | 默认值 |
|---------|------|--------|
| `page` | Number | 1 |
| `pageSize` | Number | 20 |

**响应 data（单条 conversation）**：
```json
{
  "id": 3,
  "itemId": 10,
  "otherUser": {
    "id": 5,
    "nickname": "拾主",
    "avatar": "https://..."
  },
  "lastMessage": "请问方便什么时候见面？",
  "lastMessageAt": "2024-01-16T10:30:00.000Z",
  "unreadCount": 2
}
```

---

### 5.3 获取未读聊天数量 `GET /messages/conversations/unread-count`

需登录。

**响应 data**：
```json
{
  "count": 5
}
```

---

### 5.4 获取会话详情 `GET /messages/conversations/{conversationId}`

需登录。

---

### 5.5 获取会话消息列表 `GET /messages/conversations/{conversationId}/messages`

需登录。

| 查询参数 | 类型 | 默认值 | 说明 |
|---------|------|--------|------|
| `cursor` | String | - | 分页游标（上次最后一条消息 ID） |
| `limit` | Number | 20 | 单次条数 |

**响应 data（单条 message）**：
```json
{
  "id": 42,
  "conversationId": 3,
  "senderId": 1,
  "type": "TEXT",
  "content": "你好，请问方便什么时候见面？",
  "imageUrl": null,
  "status": "SENT",
  "createdAt": "2024-01-16T10:30:00.000Z"
}
```

---

### 5.6 标记会话消息为已读 `PATCH /messages/conversations/{conversationId}/read`

需登录。

---

### 5.7 发送消息 `POST /messages/conversations/{conversationId}/messages`

需登录。

**请求体**：
```json
{
  "messageType": "TEXT",
  "content": "你好，我有空。"
}
```
或图片消息：
```json
{
  "messageType": "IMAGE",
  "imageUrl": "https://example.com/img.jpg"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `messageType` | String | 是 | `TEXT` / `IMAGE` |
| `content` | String | TEXT 类型需 | 文本消息内容 |
| `imageUrl` | String | IMAGE 类型需 | 图片 URL |

---

### 5.8 获取通知列表 `GET /messages/notifications`

需登录。

| 查询参数 | 类型 | 默认值 |
|---------|------|--------|
| `page` | Number | 1 |
| `pageSize` | Number | 20 |

**响应 data（单条通知）**：
```json
{
  "id": 1,
  "type": "AUDIT",
  "title": "您的发布已通过审核",
  "content": "您发布的「黑色双肩包」已通过审核，现已公开展示。",
  "isRead": false,
  "relatedId": 10,
  "createdAt": "2024-01-15T12:00:00.000Z"
}
```

---

### 5.9 获取未读通知数量 `GET /messages/notifications/unread-count`

需登录。

**响应 data**：
```json
{
  "count": 3
}
```

---

### 5.10 标记通知为已读 `PATCH /messages/notifications/read`

需登录。

**请求体**：
```json
{
  "notificationIds": [1, 2, 3]
}
```

---

## 6. 举报接口 `/reports`

### 6.1 提交举报 `POST /reports`

需登录。

**请求体**：
```json
{
  "itemId": 10,
  "reasonType": "SCAM",
  "reasonDetail": "该发布疑似诈骗，要求先转账",
  "evidenceUrls": ["https://.../evidence.jpg"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `itemId` | Number | 是 | 被举报物品 ID |
| `reasonType` | String | 是 | `SCAM`（诈骗）/ `AD`（广告）/ `SENSITIVE`（敏感）/ `OTHER`（其他） |
| `reasonDetail` | String | 否 | 详细说明 |
| `evidenceUrls` | Array\<String\> | 否 | 证据图片 |

**响应 data**：
```json
{
  "reportId": 7,
  "status": "PENDING"
}
```

---

### 6.2 获取我的举报记录 `GET /reports/my`

需登录。支持分页参数 `page`、`pageSize`。

---

## 7. 管理员接口 `/admin`

需登录 + 角色 `ADMIN`。

### 7.1 获取待审核物品列表 `GET /admin/items/pending`

| 查询参数 | 类型 | 默认值 |
|---------|------|--------|
| `page` | Number | 1 |
| `pageSize` | Number | 20 |
| `itemType` | String | - | `LOST` / `FOUND` |

---

### 7.2 获取已审核物品列表 `GET /admin/items/audited`

同上，额外支持 `auditStatus`、`startDate`、`endDate` 筛选。

---

### 7.3 获取物品详情 `GET /admin/items/{id}`

包含管理员视角的 `auditStatus`、`isLocked`、`visible`、`viewCount` 等字段。

---

### 7.4 审核物品 `PATCH /admin/items/{id}/audit`

**请求体**：
```json
{
  "action": "APPROVE",
  "reason": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | String | 是 | `APPROVE`（通过）/ `REJECT`（驳回） |
| `reason` | String | REJECT 需 | 驳回原因 |

---

### 7.5 上下架物品 `PATCH /admin/items/{id}/visibility`

**请求体**：
```json
{
  "visible": false
}
```

---

### 7.6 获取举报列表 `GET /admin/reports`

| 查询参数 | 类型 | 默认值 | 说明 |
|---------|------|--------|------|
| `page` | Number | 1 | - |
| `pageSize` | Number | 20 | - |
| `status` | String | - | `PENDING` / `PROCESSED` / `REJECTED` |

---

### 7.7 处理举报 `PATCH /admin/reports/{id}/handle`

**请求体**：
```json
{
  "action": "REMOVE",
  "handleResult": "已下架该物品"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | String | 是 | `REMOVE` / `REJECT` / `IGNORE` |
| `handleResult` | String | 否 | 处理结果说明 |

---

### 7.8 获取用户列表 `GET /admin/users`

支持分页 + 关键字（昵称/邮箱）筛选。

---

### 7.9 切换用户状态 `PATCH /admin/users/{id}/status`

用于封禁/解封普通用户。

---

### 7.10 获取统计数据 `GET /admin/statistics`

**响应 data**：
```json
{
  "totalItems": 120,
  "totalUsers": 350,
  "pendingItems": 12,
  "pendingReports": 5,
  "itemsByType": {
    "LOST": 80,
    "FOUND": 40
  },
  "itemsByStatus": {
    "ONGOING": 90,
    "RESOLVED": 20,
    "RETURNED": 8,
    "CANCELLED": 2
  },
  "recentActivity": [ ... ]
}
```

---

### 7.11 获取管理员待办提醒 `GET /admin/reminder-counts`

**响应 data**：
```json
{
  "pendingItems": 12,
  "pendingReports": 5
}
```

---

### 7.12 发送系统通知 `POST /admin/notifications/send`

**请求体**：
```json
{
  "targetUserId": 5,
  "title": "系统通知",
  "content": "您的账号存在异常操作提醒..."
}
```

---

## 8. 关键枚举速查表

### 8.1 物品类型 `itemType`

| 值 | 中文 |
|------|------|
| `LOST` | 寻物启事 |
| `FOUND` | 拾物招领 |

### 8.2 审核状态 `auditStatus`

| 值 | 说明 |
|------|------|
| `PENDING` | 待审核（首页不可见） |
| `APPROVED` | 审核通过（首页可见） |
| `REJECTED` | 审核驳回（不可见） |

### 8.3 业务状态 `bizStatus`

| 值 | 适用 | 说明 |
|------|------|------|
| `ONGOING` | LOST / FOUND | 进行中 |
| `RESOLVED` | LOST | 已找到 |
| `RETURNED` | FOUND | 已归还 |
| `CANCELLED` | LOST / FOUND | 已取消 |

### 8.4 用户角色 `role`

| 值 | 说明 |
|------|------|
| `USER` | 普通用户 |
| `ADMIN` | 管理员 |

### 8.5 消息类型 `type`

| 值 | 说明 |
|------|------|
| `TEXT` | 文本消息 |
| `IMAGE` | 图片消息 |

### 8.6 举报原因类型 `reasonType`

| 值 | 说明 |
|------|------|
| `SCAM` | 诈骗 |
| `AD` | 广告 |
| `SENSITIVE` | 敏感内容 |
| `OTHER` | 其他 |

### 8.7 举报处理动作 `action`

| 值 | 说明 |
|------|------|
| `REMOVE` | 下架物品 |
| `REJECT` | 驳回举报 |
| `IGNORE` | 忽略 |
