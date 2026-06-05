# 校园二手交易APP项目规则与架构设计

> **版本**：v3.1  **更新日期**：2025-12-23  **适用范围**：Flutter客户端 + Spring Boot 3.x服务端

**文档维护说明**：本文档基于实际项目代码结构编写，确保与项目实现保持一致。项目已完成基础功能、功能优化和扩展功能开发，当前处于商业化准备阶段。

**项目完成状态**：
- ✅ 基础功能：用户认证、商品管理、订单交易、消息沟通、文件上传、管理后台
- ✅ 功能优化：性能优化、用户体验、安全增强、稳定性、实时功能
- ✅ 扩展功能：收藏、举报、搜索优化、统计报表、推荐系统、购物车、商品评价、支付系统
- ✅ 举报处理功能：支持举报提交、管理后台处理、二次修改已处理举报、举报卡片导航到商品详情页
- 🔄 商业化准备：规划中，包含支付集成、运营工具、生态合作、国际化

## 项目概述

本项目是一个面向校园场景的二手交易平台，采用前后端分离架构，Android客户端基于Flutter开发，后端基于Spring Boot 3.x框架。项目已完成基础功能开发，具备完整的认证、商品、订单、消息、管理后台等功能模块。

## 技术选型

### 前端技术栈（Flutter）
| 技术领域 | 技术选型 | 说明 |
|---------|---------|------|
| 开发语言 | Dart | 现代化的面向对象编程语言，专为Flutter设计 |
| UI框架 | Flutter 3.x | 跨平台移动应用开发框架，支持热重载 |
| 状态管理 | Riverpod | 现代化的状态管理库，支持依赖注入 |
| 路由管理 | GoRouter | 声明式路由管理库，支持命名路由和嵌套路由 |
| 网络请求 | Dio | 强大的HTTP客户端，支持拦截器、取消请求等功能 |
| 本地存储 | shared_preferences | Flutter轻量级数据存储方案 |
| 图片处理 | cached_network_image | 支持图片缓存和加载状态管理 |
| JSON解析 | json_serializable | 自动生成JSON序列化/反序列化代码 |

### 后端技术栈（Spring Boot 3.x）
| 技术领域 | 技术选型 | 说明 |
|---------|---------|------|
| 开发语言 | Java 17 | 企业级应用开发标准语言，具备良好的性能和稳定性 |
| 后端框架 | Spring Boot 3.2.4 | 现代化微服务框架，支持快速开发和部署 |
| 数据库 | MySQL 8.0+ | 成熟的关系型数据库，支持事务处理和复杂查询 |
| 数据访问 | JPA + Spring Data JPA | 标准ORM框架，简化数据库操作 |
| 安全框架 | Spring Security 6.x | 企业级安全认证和授权框架 |
| API文档 | Swagger/OpenAPI 3 | 自动生成API文档，便于前后端协作 |
| 文件存储 | 本地文件系统 | 基于服务器的文件存储方案，支持文件上传和管理 |
| WebSocket | Spring WebSocket | 支持实时通信功能 |

### 数据库配置
- **数据库名**：ai
- **账号**：test
- **密码**：空
- **连接参数**：`useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`

## 系统架构设计

### 整体架构
本项目采用经典的三层架构设计，确保系统具有良好的可扩展性和维护性：

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Flutter客户端  │ ←→ │   Spring Boot    │ ←→ │      MySQL      │
│                 │    │     后端API       │    │                 │
│ - Riverpod状态管理  │    │ - RESTful API    │    │ - 关系型数据库    │
│ - GoRouter路由管理  │    │ - WebSocket服务   │    │ - 事务支持       │
│ - Dio网络请求     │    │ - 业务逻辑处理    │    │ - 索引优化       │
│ - Material Design UI │  │ - 数据库操作      │    │ - 数据备份       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### 后端核心包结构
```
campus-trade-backend/
├── src/main/java/com/campus/trade/
│   ├── CampusTradeApplication.java      # 启动类
│   ├── annotation/                      # 自定义注解
│   │   └── OperationLog.java            # 操作日志注解
│   ├── aspect/                          # AOP切面
│   │   └── OperationLoggingAspect.java  # 操作日志切面
│   ├── common/                          # 通用类
│   │   ├── ApiResponse.java             # API响应封装
│   │   ├── PageMeta.java                # 分页元数据
│   │   └── PaginatedResponse.java       # 分页响应封装
│   ├── config/                          # 配置类
│   │   ├── InitialDataConfig.java       # 初始化数据配置
│   │   ├── SecurityConfig.java          # 安全配置
│   │   ├── WebSocketConfig.java         # WebSocket配置
│   │   └── WebConfig.java               # Web配置
│   ├── controller/                      # 控制器层
│   │   ├── admin/                       # 管理员控制器
│   │   │   ├── AdminCategoryController.java
│   │   │   ├── AdminDashboardController.java
│   │   │   ├── AdminModerationReportController.java
│   │   │   ├── AdminReportController.java
│   │   │   └── AdminUserController.java
│   │   ├── AuthController.java          # 认证相关接口
│   │   ├── CartController.java          # 购物车相关接口
│   │   ├── CategoryController.java      # 分类相关接口
│   │   ├── FavoriteController.java      # 收藏相关接口
│   │   ├── MessageController.java       # 消息相关接口
│   │   ├── OrderController.java         # 订单相关接口
│   │   ├── ProductController.java       # 商品相关接口
│   │   ├── ReportController.java        # 举报相关接口
│   │   └── UserController.java          # 用户相关接口
│   ├── dto/                             # 数据传输对象
│   │   ├── auth/                        # 认证相关DTO
│   │   ├── cart/                         # 购物车相关DTO
│   │   ├── category/                     # 分类相关DTO
│   │   ├── message/                      # 消息相关DTO
│   │   ├── order/                        # 订单相关DTO
│   │   ├── product/                      # 商品相关DTO
│   │   ├── report/                       # 举报相关DTO
│   │   └── user/                         # 用户相关DTO
│   ├── exception/                       # 异常处理
│   │   ├── BusinessException.java       # 业务异常
│   │   ├── ErrorCode.java               # 错误码定义
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   ├── model/                           # 数据模型
│   │   ├── entity/                      # 实体类
│   │   │   ├── BaseEntity.java          # 基础实体类
│   │   │   ├── CartItem.java            # 购物车实体
│   │   │   ├── Category.java            # 分类实体
│   │   │   ├── Favorite.java            # 收藏实体
│   │   │   ├── Message.java             # 消息实体
│   │   │   ├── Order.java               # 订单实体
│   │   │   ├── Product.java             # 商品实体
│   │   │   ├── Report.java              # 举报实体
│   │   │   └── User.java                # 用户实体
│   │   └── enums/                       # 枚举类型
│   │       ├── AccountStatus.java       # 账户状态
│   │       ├── OrderStatus.java         # 订单状态
│   │       ├── ProductStatus.java       # 商品状态
│   │       ├── ReportStatus.java        # 举报状态
│   │       ├── ReportTargetType.java    # 举报对象类型
│   │       └── UserRole.java            # 用户角色
│   ├── repository/                      # 数据访问层
│   │   ├── CartItemRepository.java      # 购物车数据操作
│   │   ├── CategoryRepository.java      # 分类数据操作
│   │   ├── FavoriteRepository.java      # 收藏数据操作
│   │   ├── MessageRepository.java       # 消息数据操作
│   │   ├── OrderRepository.java         # 订单数据操作
│   │   ├── ProductRepository.java       # 商品数据操作
│   │   ├── ReportRepository.java        # 举报数据操作
│   │   └── UserRepository.java          # 用户数据操作
│   ├── security/                        # 安全配置
│   │   ├── CustomUserDetails.java       # 自定义用户详情
│   │   ├── CustomUserDetailsService.java # 用户详情服务
│   │   ├── JwtAuthenticationFilter.java # JWT认证过滤器
│   │   └── JwtTokenProvider.java        # JWT工具类
│   ├── service/                         # 服务层
│   │   ├── impl/                        # 服务实现
│   │   │   ├── AuthServiceImpl.java     # 认证服务实现
│   │   │   ├── CartServiceImpl.java     # 购物车服务实现
│   │   │   ├── ProductServiceImpl.java  # 商品服务实现
│   │   │   ├── ReportServiceImpl.java   # 举报服务实现
│   │   │   └── UserServiceImpl.java     # 用户服务实现
│   │   ├── AuthService.java             # 认证服务接口
│   │   ├── CartService.java             # 购物车服务接口
│   │   ├── ProductService.java          # 商品服务接口
│   │   ├── ReportService.java           # 举报服务接口
│   │   └── UserService.java             # 用户服务接口
│   └── util/                            # 工具类
│       └── JsonListConverter.java       # JSON列表转换器
└── src/main/resources/
    ├── application.properties           # 配置文件
    └── static/upload/                   # 文件上传目录
```

### 前端核心目录结构
```
flutter_app/
├── lib/
│   ├── app.dart                         # 应用根组件
│   ├── main.dart                        # 应用入口
│   ├── core/                            # 核心功能
│   │   ├── config/                      # 配置
│   │   │   └── env.dart                 # 环境配置
│   │   ├── di/                          # 依赖注入
│   │   │   ├── di.dart                  # 依赖注入配置
│   │   │   └── providers.dart           # Riverpod providers
│   │   ├── network/                     # 网络相关
│   │   │   ├── api_response_interceptor.dart
│   │   │   ├── auth_interceptor.dart
│   │   │   ├── log_interceptor.dart
│   │   │   └── retry_interceptor.dart
│   │   ├── router/                      # 路由管理
│   │   │   ├── app_router.dart          # 路由配置
│   │   │   └── app_shell.dart           # 应用壳组件
│   │   └── theme/                       # 主题配置
│   │       └── app_theme.dart           # 应用主题
│   ├── data/                            # 数据层
│   │   ├── api/                         # API接口定义
│   │   ├── models/                      # 数据模型
│   │   └── repositories/                # 数据仓库
│   └── features/                        # 功能模块
│       ├── admin/                       # 管理员功能
│       │   ├── application/             # 业务逻辑
│       │   └── presentation/            # UI组件
│       │       └── admin_home_page.dart
│       ├── auth/                        # 认证功能
│       │   ├── application/             # 业务逻辑
│       │   └── presentation/            # UI组件
│       │       ├── login_page.dart
│       │       └── register_page.dart
│       ├── product/                     # 商品功能
│       │   ├── application/             # 业务逻辑
│       │   └── presentation/            # UI组件
│       │       ├── product_detail_page.dart
│       │       └── product_list_page.dart
│       ├── report/                      # 举报功能
│       │   ├── application/             # 业务逻辑
│       │   └── presentation/            # UI组件
│       │       ├── admin_report_management.dart
│       │       └── report_detail_page.dart
│       └── user/                        # 用户功能
│           ├── application/             # 业务逻辑
│           └── presentation/            # UI组件
│               └── profile_page.dart
└── pubspec.yaml                         # 依赖配置
```

### 实体类设计（基于实际代码）

#### User实体
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    private Long id;
    private String username;          // 用户名，唯一
    private String password;          // 密码（BCrypt加密）
    private String email;             // 邮箱，唯一
    private String phone;             // 手机号
    private String realName;          // 真实姓名
    private String school;            // 学校
    private String avatar;            // 头像URL
    private String contactInfo;       // 联系方式
    private UserRole role;            // 用户角色（STUDENT/TEACHER/STAFF）
    private AccountStatus status;     // 账户状态（ACTIVE/DISABLED）
    private boolean emailVerified;    // 邮箱验证状态
    private boolean deleteRequested;  // 注销请求标记
    private String deleteReason;      // 注销原因
    private LocalDateTime deleteScheduleTime; // 注销计划时间
    private LocalDateTime lastLogin;  // 最后登录时间
}
```

#### Product实体
```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    private Long id;
    private String title;             // 商品标题
    private String description;       // 商品描述
    private BigDecimal price;         // 商品价格
    private ProductCategory category; // 商品分类
    private List<String> images;      // 商品图片URL列表（JSON格式）
    private User seller;              // 卖家
    private ProductStatus status;     // 商品状态（ON_SALE/OFF_SALE/SOLD/DELETED）
    private AuditStatus auditStatus;  // 审核状态（PENDING/APPROVED/REJECTED）
    private Integer viewCount;        // 浏览量
    private Integer likeCount;        // 点赞数
    private String contactInfo;       // 联系方式
    private String location;          // 交易地点
    private String remark;            // 备注
}
```

#### Report实体
```java
@Entity
@Table(name = "reports")
public class Report extends BaseEntity {
    private Long id;
    private User reporter;            // 举报者
    private ReportTargetType targetType; // 举报对象类型
    private Long targetId;            // 举报对象ID
    private String targetSnapshot;    // 举报对象快照
    private String reason;            // 举报原因
    private String description;       // 举报描述
    private List<String> evidenceUrls; // 证据URL列表
    private ReportStatus status;      // 举报状态
    private String resolution;        // 处理结果
    private String handledBy;         // 处理人
    private LocalDateTime handledTime; // 处理时间
    private boolean autoFlagged;      // 是否自动标记
    private String autoReason;        // 自动标记原因
    private String contactInfo;       // 联系方式
}
```

## 安全架构设计

### 认证与授权
- **JWT认证**：使用JWT令牌进行用户身份认证
- **角色权限**：基于Spring Security的角色权限控制
- **接口权限**：使用`@PreAuthorize`注解进行方法级权限控制
- **密码加密**：使用BCrypt算法对用户密码进行加密存储

### 安全配置
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // 配置安全过滤器链
    // 配置密码编码器
    // 配置JWT认证
}
```

## 文件存储设计

### 存储架构
- **存储方式**：本地文件系统存储
- **存储路径**：`src/main/resources/static/upload/`
- **访问方式**：HTTP静态资源访问
- **文件类型**：图片（用户头像、商品图片、举报证据）

### 目录结构
```
static/upload/
├── users/                   # 用户相关文件
│   └── {user_id}/           # 按用户ID分目录
│       └── avatar/          # 用户头像目录
├── products/                # 商品相关文件
│   └── {product_id}/        # 按商品ID分目录
│       └── images/          # 商品图片目录
└── system/                  # 系统相关文件
    ├── logs/                # 日志文件
    └── temp/                # 临时文件
```

### 文件上传规则
- **最大文件大小**：头像≤2MB，商品图片≤5MB，举报证据≤10MB
- **支持格式**：jpg、jpeg、png、gif
- **命名规则**：`{type}_{id}_{timestamp}.{extension}`

## 开发规范

### 代码规范
- **命名规范**：遵循Dart/Java命名约定，使用驼峰命名法
- **包结构**：按功能模块划分包结构，保持层次清晰
- **注释规范**：重要方法和类需要添加注释
- **代码风格**：使用IDE默认格式化规则

### API设计规范
- **RESTful风格**：使用标准的HTTP方法和状态码
- **版本控制**：API路径包含版本号（如`/api/v1/`）
- **统一响应格式**：使用统一的API响应格式
- **请求参数**：使用DTO对象封装请求参数
- **响应数据**：使用DTO对象封装响应数据

### 数据库规范
- **表命名**：使用复数形式（如`users`、`products`）
- **字段命名**：使用下划线分隔（如`create_time`）
- **索引策略**：为常用查询字段创建索引
- **外键关系**：使用JPA关联注解定义外键关系

### 前端开发规范
- **组件设计**：遵循单一职责原则，组件功能模块化
- **状态管理**：使用Riverpod进行状态管理，遵循不可变数据原则
- **路由设计**：使用GoRouter进行路由管理，采用命名路由
- **网络请求**：使用Dio进行网络请求，实现统一的拦截器处理
- **错误处理**：统一处理网络错误和业务错误

## 部署与运维

### 本地开发环境
- **数据库**：MySQL 8.0+，使用Navicat Premium Lite 17管理
- **后端服务**：Spring Boot应用，默认端口8080
- **前端应用**：Flutter 3.x，使用Android Studio开发，支持模拟器和真机调试

### 生产环境建议
- **数据库**：MySQL主从复制，定期备份
- **应用服务器**：Tomcat或Docker容器化部署
- **文件存储**：考虑使用云存储服务（如阿里云OSS）
- **监控告警**：集成Spring Boot Actuator进行应用监控
- **日志管理**：使用ELK Stack进行日志收集和分析

## 技术栈优势

1. **跨平台开发**：Flutter框架支持一次开发，多平台部署（Android、iOS、Web、Desktop）
2. **开发效率高**：Flutter的热重载特性和Spring Boot的快速开发能力，显著提高开发效率
3. **状态管理清晰**：Riverpod提供了现代化的状态管理方案，使应用状态更加可控和可测试
4. **安全性强**：集成Spring Security提供完善的安全保障
5. **扩展性好**：模块化架构设计，支持后续功能扩展
6. **社区支持完善**：所选技术均有成熟的社区支持和丰富的文档资源

## 举报处理功能实现

### 核心流程
1. **举报提交**：用户在商品详情页或聊天界面提交举报
2. **自动标记**：系统根据关键词和举报次数自动标记高风险举报
3. **管理后台处理**：管理员在管理后台查看举报列表，点击举报卡片可跳转到商品详情页
4. **二次修改**：支持对已处理的举报进行二次修改
5. **结果通知**：处理结果通过消息推送通知给举报者

### 关键实现细节
- **举报卡片导航**：使用GoRouter的命名路由实现从举报卡片到商品详情页的导航
- **商品详情页返回按钮**：使用GoRouter的`context.pop()`实现返回功能
- **二次修改支持**：在`ReportServiceImpl`中移除了对已处理举报的修改限制
- **自动标记逻辑**：基于关键词匹配和举报次数进行自动标记

---

**文档维护说明**：本文档基于实际项目代码结构编写，确保与项目实现保持一致。如有重大架构变更，请及时更新本文档。