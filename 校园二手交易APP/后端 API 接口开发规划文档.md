# 校园二手交易 APP · 后端 API 开发规划

> **版本**：v2.0  **发布日期**：2025-11-22  **维护人**：后端负责人 / 课程设计组

---

## 1. 目标与范围

- **目标**：在 4 周内交付一个稳定、易讲解、易扩展的 Spring Boot 后端，为 Android 客户端提供 REST API、JWT 鉴权、文件上传与基本审核能力。
- **范围**：认证与账户、商品、订单、消息、个人中心、管理员、文件上传七大模块，对应数据库 `users/products/orders/messages/admins`。
- **不含**：第三方支付、分布式部署、云对象存储、复杂推荐算法。

---

## 2. 架构与代码结构

```
com.campus.trade
├── CampusTradeApplication
├── config          # WebMvc / Swagger / ObjectMapper / HikariCP
├── security        # JwtAuthenticationFilter, SecurityConfig, JwtUtil
├── exception       # GlobalExceptionHandler, ErrorCode
├── controller      # 按模块拆分（Auth/User/Product/...）
├── service         # 业务层，聚合 Repository 与领域逻辑
├── repository      # Spring Data JPA 接口
├── model
│   ├── entity      # 与 db_schema.md.md 对齐
│   └── dto         # request/response 对象
└── file            # FileService, FileController
```

- **分层约定**：Controller 仅做参数校验和响应封装；业务逻辑进 Service；数据库访问集中在 Repository。
- **命名规范**：
  - Controller 命名 `XxxController`，路径使用复数：`/products`, `/orders`。
  - DTO 后缀 `Request` / `Response`。
  - 枚举使用大写字符串，与数据库/文档一致。

---

## 3. 技术选型与配置

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 语言 | Java 17 | 与 Android/教学场景兼容 |
| 框架 | Spring Boot 3.2.x | Web, Validation, Data JPA, Security |
| 安全 | Spring Security 6 + JWT | Header `Authorization: Bearer xx` |
| ORM | Spring Data JPA | `ddl-auto=validate`，实体与 `db_schema.md.md` 对齐 |
| 数据库 | MySQL 8.0+ | 库名 `ai`，账号 `test`，密码空 |
| 文档 | SpringDoc OpenAPI 3 | `/swagger-ui/index.html` 对接 `openapi.yaml` |
| 构建 | Maven Wrapper | `./mvnw clean package` |

`application.yml` 核心示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: test
    password: ""
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
jwt:
  secret: campus-trade-secret
  expiration: 7200
```

---

## 4. 开发流程

1. **需求同步**：阅读《规格说明书》《API 文档》《数据库文档》，确认字段与流程一致。
2. **建模**：根据 `db_schema.md.md` 创建实体与枚举，编写 Flyway 脚本。
3. **接口开发顺序**（建议）：
   1. 认证（注册/登录/邮箱验证/密码）
   2. 文件上传（头像/商品图）
   3. 商品（发布/列表/详情/状态）
   4. 订单（创建/列表/状态流转）
   5. 消息（发送/列表/举报）
   6. 个人中心（资料/注销）
   7. 管理端（登录/审核/用户管理）
4. **代码评审**：每个模块完成后走一次 Pair Review，确保响应格式统一。
5. **测试**：服务自测 + Postman 集合（核心流程 + 异常场景）。
6. **打包交付**：`./mvnw clean package` → 生成 `campus-trade.jar` → 提供 README 与运行脚本。

---

## 5. 模块分解与责任

| 模块 | 负责人 | 依赖 | 说明 |
| --- | --- | --- | --- |
| Auth/User | A 同学 | Security/DB | 注册、登录、邮件状态、密码管理、`/users/me` |
| Product | B 同学 | Auth/File | 发布、列表、详情、状态、审核字段 |
| Order | C 同学 | Product | 下单、状态机、评价字段 |
| Message | D 同学 | User/Order | 私信、系统通知、举报 |
| Personal | A 同学 | Auth/Product | 资料管理、注销流程、我的商品 |
| Admin | B 同学 | Auth/Product | 管理员登录/注册、商品审核、用户状态 |
| File | 任意 | none | 本地文件存储、静态资源映射 |

> 如果人手不足，可按照 Sprint 拆分顺序串行开发。

---

## 6. 安全与质量控制

| 项 | 要求 |
| --- | --- |
| 鉴权 | JWT Filter 在 `SecurityFilterChain` 中配置，放行 `/auth/**`, `/admin/login`, `/v3/api-docs/**` |
| 授权 | `@PreAuthorize` 控制角色，普通用户 `hasRole('USER')`，管理员 `hasAnyRole('ADMIN','SUPER_ADMIN')` |
| 参数校验 | DTO 使用 `@Valid`，统一异常处理器转换为标准响应 |
| 日志 | 登录、下单、审核等关键操作记录 `userId`、`traceId`，禁止打印密码/Token |
| 错误码 | 使用 `ErrorCode` 枚举（`USER_ALREADY_EXISTS`, `PRODUCT_NOT_FOUND` 等），与 `api.md` 一致 |
| 文件上传 | 校验扩展名（jpg/png）、大小（≤5MB），生成唯一文件名，禁止目录穿越 |

---

## 7. 测试计划

| 场景 | 内容 |
| --- | --- |
| 单元测试 | Service 层状态流转（订单确认/取消、商品状态、账号注销） |
| 集成测试 | 使用 H2 或测试环境 MySQL，覆盖核心 API（注册、发布、下单、消息） |
| 接口测试 | Postman/Thunder Client 集合，包含 2xx/4xx/401/403 场景 |
| 手动回归 | Android 客户端串联“注册→发布→下单→聊天→完成”全链路 |
| 性能测试 | JMeter 对 `/products`、`/orders` 列表做 50 并发基准 |

---

## 8. 部署与交付

1. **运行方式**：`java -jar campus-trade.jar --spring.profiles.active=prod`。
2. **静态资源**：Spring MVC 映射 `/upload/** -> classpath:/static/upload/`。
3. **数据库初始化**：执行 Flyway 脚本或导入 `schema.sql`，并插入示例用户/管理员。
4. **演示数据**：
   - 超级管理员：admin / Admin123（登录 `/admin/login`）。
   - 普通用户：buyer01/seller01，已验证邮箱。
   - 样例商品、订单、消息各 1-2 条，方便答辩展示。

---

## 9. 里程碑

| 里程碑 | 时间 | 验收标准 |
| --- | --- | --- |
| M0 | 第 1 天 | 完成环境搭建、仓库初始化、文档阅读 |
| M1 | 第 5 天 | 认证 + 文件上传 + 商品发布/列表可运行 |
| M2 | 第 10 天 | 订单 + 消息模块打通，主流程能演示 |
| M3 | 第 15 天 | 管理端与个人中心完成，全部接口对齐文档 |
| M4 | 第 20 天 | 完成测试、打包、文档、演示稿 |

---

## 10. 风险与应对

| 风险 | 描述 | 对策 |
| --- | --- | --- |
| 需求变动 | 后期增加功能导致返工 | 所有变动先更新 `spec.md` 与 `api.md`，再开发 |
| 进度延迟 | 学业任务冲突 | 每周例会同步进展，必要时缩减选做功能 |
| 数据不一致 | 状态流转缺少校验 | Service 加事务，统一状态枚举，编写单测 |
| 文档不同步 | 接口/数据库变化未记录 | 变更后 24h 内同步四份文档（spec/api/db/openapi） |

---

## 11. 附录

- 相关文档：`spec.md`、`db_schema.md.md`、`api.md`、`openapi.yaml`。
- 工具建议：
  - IDE：IntelliJ IDEA / VS Code + Java 扩展。
  - 接口调试：Postman / Thunder Client。
  - 数据库：Navicat / VS Code MySQL 插件。
- 代码规范：使用 Checkstyle 或 IDE 自带格式化，提交前运行 `./mvnw spotless:apply`（如配置）。

> 本规划作为后端团队的执行手册。若团队规模缩减，可按模块串行开发；如需求升级，请更新版本号并记录变更摘要。

