# 校园失物招领小程序后端

基于 Node.js + Express + MySQL + Redis 的后端服务。

## 技术栈

- **框架**: Express.js
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.0+
- **认证**: JWT
- **文件上传**: Multer

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env` 并修改配置：

```bash
cp .env.example .env
```

必填配置项：
- `DB_HOST`: MySQL 主机地址
- `DB_USER`: MySQL 用户名
- `DB_PASSWORD`: MySQL 密码
- `DB_NAME`: 数据库名
- `REDIS_HOST`: Redis 主机地址
- `REDIS_PORT`: Redis 端口
- `WECHAT_APP_ID`: 微信小程序 AppID
- `WECHAT_APP_SECRET`: 微信小程序 AppSecret
- `JWT_SECRET`: JWT 密钥
- `EMAIL_HOST`: SMTP 邮件服务器
- `EMAIL_USER`: 发件邮箱
- `EMAIL_PASS`: 邮箱授权码

### 3. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 4. 启动服务

```bash
# 开发环境
npm run dev

# 生产环境
npm start
```

## API 文档

所有 API 遵循 RESTful 规范，基础路径为 `/api/v1`。

详细文档请参考项目根目录的 `docs/api.md`。

## 项目结构

```
backend/
├── app.js                    # 应用入口
├── config/                   # 配置文件
│   └── index.js
├── controllers/              # 控制器
│   ├── authController.js     # 认证控制器
│   ├── itemController.js     # 物品信息控制器
│   ├── fileController.js     # 文件上传控制器
│   ├── messageController.js  # 消息控制器
│   ├── reportController.js   # 举报控制器
│   └── adminController.js    # 管理员控制器
├── models/                   # 数据模型
│   ├── User.js
│   ├── Item.js
│   ├── ItemImage.js
│   ├── Conversation.js
│   ├── Message.js
│   ├── Notification.js
│   ├── Report.js
│   └── AuditLog.js
├── routes/                   # 路由
│   ├── index.js
│   ├── auth.js
│   ├── items.js
│   ├── files.js
│   ├── messages.js
│   ├── reports.js
│   └── admin.js
├── middlewares/              # 中间件
│   ├── auth.js               # 认证中间件
│   ├── errorHandler.js       # 错误处理中间件
│   └── logger.js             # 日志中间件
├── utils/                    # 工具函数
│   └── response.js           # 统一响应格式
├── services/                 # 业务服务
│   └── emailService.js       # 邮件服务
├── database/                 # 数据库连接
│   ├── connection.js         # MySQL 连接池
│   └── redis.js              # Redis 客户端
├── uploads/                  # 上传文件存储
│   └── images/
└── sql/                      # SQL 脚本
    └── init.sql              # 数据库初始化脚本
```

## 开发规范

### 响应格式

所有接口使用标准 HTTP 状态码，响应体格式：

```json
{
  "message": "ok",
  "data": {},
  "requestId": "uuid",
  "timestamp": "2026-05-05T10:00:00Z"
}
```

### 状态枚举

- **itemType**: `LOST` | `FOUND`
- **auditStatus**: `PENDING` | `APPROVED` | `REJECTED`
- **bizStatus**: `ONGOING` | `RESOLVED` | `RETURNED` | `CANCELLED`
- **userRole**: `USER` | `ADMIN`

## License

MIT
