# 项目规则

## 1. 项目定位

- 项目名称：校园失物招领小程序
- 架构模式：前后端分离
- 前端：微信小程序原生（WXML/WXSS/JavaScript）
- 后端：Node.js + Express
- 数据层：MySQL + Redis
- 文件存储：图片文件存储（本地或对象存储）
- 明确约束：不使用微信云开发作为主架构

## 2. 目标与边界

### 2.1 项目目标

- 提升校园失物找回效率
- 支持寻物与拾物发布
- 支持搜索、沟通、审核、举报与统计
- 建立安全、有序、可维护的校园失物招领平台

### 2.2 功能边界

- 仅围绕校园失物招领场景开发
- 发布信息必须先审后发
- 不实现与当前需求无关的复杂商业化功能（如支付、电商、推荐算法）

## 3. 角色与权限

### 3.1 角色定义

- USER：在校学生/教师，统一为普通用户角色
- ADMIN：管理员角色

### 3.2 用户权限

- 可发布寻物（LOST）和拾物（FOUND）信息
- 可搜索、查看、编辑本人未完成信息
- 可标记状态（找回/归还/取消）
- 可通过站内消息联系对方
- 可在详情页发起举报

### 3.3 管理员权限

- 审核信息（通过/驳回）
- 管理信息上下架与违规内容
- 管理用户状态
- 处理举报
- 查看统计数据

### 3.4 最小可见原则
- 管理员入口仅管理员可见

## 4. 核心业务规则

### 4.1 发布与审核

- 用户提交信息字段：名称、类型、时间、地点、特征、图片
- 新建信息默认：auditStatus = PENDING，bizStatus = ONGOING
- 仅 auditStatus = APPROVED 的信息可公开展示

### 4.2 审核状态流转

- PENDING -> APPROVED
- PENDING -> REJECTED
- APPROVED/REJECTED 不允许重复审核（无复审流程时）

### 4.3 业务状态流转

- LOST：ONGOING -> RESOLVED 或 CANCELLED
- FOUND：ONGOING -> RETURNED 或 CANCELLED
- 完成态（RESOLVED/RETURNED/CANCELLED）不可回退到 ONGOING

### 4.4 编辑规则

- 仅允许编辑本人未完成且未被管理员锁定的信息
- 编辑后重新进入待审核（auditStatus = PENDING）

### 4.5 沟通与举报

- 用户通过站内消息系统联系，不强制公开私密联系方式
- 详情页必须支持举报
- 举报由管理员处理，可执行下架、警告、封禁等动作

## 5. 页面与交互约束

### 5.1 必备页面

- 登录注册
- 首页
- 发布
- 详情
- 消息
- 个人中心
- 管理员后台

### 5.2 导航约束

- 底部导航固定：首页、发布、消息、我的
- 管理员后台入口位于个人中心，仅管理员可见

### 5.3 交互要求

- 表单实时校验
- 图片选择与预览
- 首页与消息页支持下拉刷新、上拉加载
- 错误提示清晰可理解

## 6. 非功能要求

### 6.1 性能

- 首页加载时间 <= 3 秒
- 用户操作响应 <= 1 秒
- 支持 1000 用户并发在线

### 6.2 安全

- 全链路 HTTPS
- 基于微信登录并结合学业邮箱验证
- 后端必须进行参数校验、权限校验、状态校验
- 防注入：参数化 SQL，禁止字符串拼接 SQL
- 对高风险接口进行限流（登录、验证码、消息发送）

### 6.3 可用性与兼容性

- 提供明确错误提示与处理引导
- 兼容主流微信版本与设备

### 6.4 可维护性

- 模块化目录结构
- 关键流程记录日志（建议包含 requestId）
- 保持接口、数据模型、状态流转一致

## 7. 技术实现约束

- 前端使用微信小程序原生技术，可使用 Vant Weapp
- 后端使用 Node.js + Express，优先 REST 风格接口
- MySQL 存储结构化数据，Redis 用于缓存
- 图片走文件存储，不放入数据库大字段
- 代码中仅在关键复杂逻辑处添加简洁注释

## 8. API 协同规则

### 8.1 基础约定

- 基础路径：/api/v1
- 统一响应结构：code/message/data/requestId/timestamp
- 时间格式：ISO8601
- 鉴权：Authorization: Bearer token

### 8.2 关键枚举

- itemType：LOST | FOUND
- auditStatus：PENDING | APPROVED | REJECTED
- bizStatus：ONGOING | RESOLVED | RETURNED | CANCELLED
- userRole：USER | ADMIN

### 8.3 MVP 最小联调接口

- 认证：/auth/wx-login、/auth/register、/auth/password-login
- 发布与展示：POST /items、GET /items、GET /items/{id}、PATCH /items/{id}/biz-status
- 个人中心：GET /users/me、GET /users/me/items
- 消息：POST /conversations、GET /conversations、POST /conversations/{id}/messages
- 审核：GET /admin/items/pending、PATCH /admin/items/{id}/audit
- 举报：POST /reports、GET /admin/reports

## 9. 开发输出规范

每次提交功能必须同步提供以下内容：

- 改动点说明
- 涉及的接口与数据字段
- 对应页面影响
- 测试点清单（正常、异常、权限）

若需求存在不确定项：

- 先提澄清问题
- 禁止凭空假设核心业务规则
- 若规则冲突，以需求规格说明书中最新明确条目为准

## 10. 测试与验收

### 10.1 功能测试覆盖

- 登录注册
- 发布
- 搜索
- 详情
- 消息
- 审核
- 举报
- 状态变更

### 10.2 性能测试覆盖

- 页面加载速度
- 并发访问
- 图片上传与加载

### 10.3 安全测试覆盖

- 权限控制
- 输入校验
- 防注入
- 隐私可见性

### 10.4 验收标准

- 核心功能可用
- 响应达标
- 数据读写正常
- 文档齐全

