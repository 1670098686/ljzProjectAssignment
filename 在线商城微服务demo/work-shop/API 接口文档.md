# 在线商城系统 API 接口文档

## 文档说明

### 基本信息
- **版本**：v1.0
- **协议**：HTTP/HTTPS
- **数据格式**：JSON
- **字符编码**：UTF-8

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 错误码说明
| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录/Token 失效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 库存不足 |
| 1002 | 订单已存在 |
| 1003 | 商品已下架 |
| 2001 | 用户已存在 |
| 2002 | 用户名或密码错误 |
| 3001 | 购物车为空 |
| 4001 | 评价已存在 |

---

## 一、用户服务 API

### 1.1 用户注册

**接口地址**：`POST /api/user/register`

**请求参数**：
```json
{
  "username": "string",
  "password": "string",
  "phone": "string"
}
```

**参数说明**：
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | string | 是 | 用户名（4-20 位） |
| password | string | 是 | 密码（6-20 位） |
| phone | string | 是 | 手机号（11 位） |

**响应示例**：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": "10001",
    "username": "zhangsan",
    "createTime": "2026-04-10 10:00:00"
  }
}
```

**异常响应**：
```json
{
  "code": 2001,
  "message": "用户已存在",
  "data": null
}
```

---

### 1.2 用户登录

**接口地址**：`POST /api/user/login`

**请求参数**：
```json
{
  "username": "string",
  "password": "string"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": "10001",
    "username": "zhangsan",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expireTime": "2026-04-11 10:00:00"
  }
}
```

**异常响应**：
```json
{
  "code": 2002,
  "message": "用户名或密码错误",
  "data": null
}
```

---

### 1.3 用户退出

**接口地址**：`POST /api/user/logout`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "退出成功",
  "data": null
}
```

---

### 1.4 修改个人信息

**接口地址**：`PUT /api/user/profile`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "nickname": "string",
  "avatar": "string",
  "phone": "string",
  "email": "string"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "修改成功",
  "data": {
    "userId": "10001",
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "http://example.com/avatar/10001.jpg",
    "phone": "138****1234",
    "email": "zhangsan@example.com"
  }
}
```

---

### 1.5 获取用户信息

**接口地址**：`GET /api/user/profile`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "10001",
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "http://example.com/avatar/10001.jpg",
    "phone": "138****1234",
    "email": "zhangsan@example.com",
    "createTime": "2026-04-01 10:00:00"
  }
}
```

---

## 二、商品服务 API

### 2.1 商品列表查询

**接口地址**：`GET /api/product/list`

**请求参数**：
```
page: 1 (页码，默认 1)
pageSize: 10 (每页数量，默认 10)
categoryId: 1 (分类 ID，可选)
keyword: "手机" (关键词，可选)
sortBy: "price" (排序字段，可选)
sortOrder: "asc" (排序方式，可选：asc/desc)
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "productId": "1001",
        "name": "iPhone 15 Pro",
        "description": "苹果旗舰手机",
        "price": 7999.00,
        "originalPrice": 8999.00,
        "image": "http://example.com/product/1001.jpg",
        "categoryId": "1",
        "categoryName": "手机数码",
        "stock": 100,
        "sales": 5000,
        "status": 1
      }
    ]
  }
}
```

---

### 2.2 商品详情查询

**接口地址**：`GET /api/product/detail/{productId}`

**路径参数**：
| 参数名 | 类型 | 说明 |
|--------|------|------|
| productId | string | 商品 ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "productId": "1001",
    "name": "iPhone 15 Pro",
    "description": "苹果旗舰手机，A17 芯片，钛金属边框",
    "detail": "<div>商品详情 HTML</div>",
    "price": 7999.00,
    "originalPrice": 8999.00,
    "images": [
      "http://example.com/product/1001_1.jpg",
      "http://example.com/product/1001_2.jpg"
    ],
    "categoryId": "1",
    "categoryName": "手机数码",
    "brand": "Apple",
    "specifications": {
      "颜色": "深空黑色",
      "存储": "256GB",
      "屏幕": "6.1 英寸"
    },
    "stock": 100,
    "sales": 5000,
    "status": 1,
    "createTime": "2026-01-01 10:00:00",
    "updateTime": "2026-04-10 10:00:00"
  }
}
```

**异常响应**：
```json
{
  "code": 1003,
  "message": "商品已下架",
  "data": null
}
```

---

### 2.3 商品分类列表

**接口地址**：`GET /api/product/categories`

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "categories": [
      {
        "id": "1",
        "name": "电子产品"
      },
      {
        "id": "2",
        "name": "数码配件"
      },
      {
        "id": "3",
        "name": "智能设备"
      }
    ]
  }
}
```

---

### 2.4 商品搜索

**接口地址**：`GET /api/product/search`

**请求参数**：
```
keyword: "手机" (关键词，必填)
page: 1 (页码)
pageSize: 10 (每页数量)
```

**响应示例**：同商品列表查询

---

## 三、库存服务 API

### 3.1 库存查询

**接口地址**：`GET /api/stock/{productId}`

**路径参数**：
| 参数名 | 类型 | 说明 |
|--------|------|------|
| productId | string | 商品 ID |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "productId": "1001",
    "stock": 100,
    "availableStock": 95,
    "lockedStock": 5,
    "updateTime": "2026-04-10 10:00:00"
  }
}
```

---

### 3.2 库存扣减（分布式事务）

**接口地址**：`POST /api/stock/deduct`

**请求参数**：
```json
{
  "orderId": "ORDER20260410001",
  "items": [
    {
      "productId": "1001",
      "quantity": 2
    },
    {
      "productId": "1002",
      "quantity": 1
    }
  ]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "库存扣减成功",
  "data": {
    "orderId": "ORDER20260410001",
    "deductTime": "2026-04-10 10:00:00"
  }
}
```

**异常响应**：
```json
{
  "code": 1001,
  "message": "库存不足",
  "data": {
    "productId": "1001",
    "required": 2,
    "available": 1
  }
}
```

---

### 3.3 库存回滚（分布式事务）

**接口地址**：`POST /api/stock/rollback`

**请求参数**：同库存扣减

**响应示例**：
```json
{
  "code": 200,
  "message": "库存回滚成功",
  "data": {
    "orderId": "ORDER20260410001",
    "rollbackTime": "2026-04-10 10:00:00"
  }
}
```

---

## 四、购物车服务 API

### 4.1 添加商品到购物车

**接口地址**：`POST /api/cart/add`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "productId": "1001",
  "quantity": 1
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "cartId": "CART10001",
    "userId": "10001",
    "productId": "1001",
    "productName": "iPhone 15 Pro",
    "productImage": "http://example.com/product/1001.jpg",
    "price": 7999.00,
    "quantity": 1,
    "checked": true,
    "createTime": "2026-04-10 10:00:00"
  }
}
```

---

### 4.2 修改购物车商品数量

**接口地址**：`PUT /api/cart/update`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "cartId": "CART10001",
  "quantity": 2
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "cartId": "CART10001",
    "productId": "1001",
    "quantity": 2,
    "totalPrice": 15998.00
  }
}
```

**异常响应**：
```json
{
  "code": 1001,
  "message": "库存不足",
  "data": null
}
```

---

### 4.3 删除购物车商品

**接口地址**：`DELETE /api/cart/{cartId}`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

### 4.4 查看购物车

**接口地址**：`GET /api/cart/list`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "10001",
    "cartItems": [
      {
        "cartId": "CART10001",
        "productId": "1001",
        "productName": "iPhone 15 Pro",
        "productImage": "http://example.com/product/1001.jpg",
        "price": 7999.00,
        "quantity": 2,
        "checked": true,
        "stock": 100,
        "totalPrice": 15998.00
      },
      {
        "cartId": "CART10002",
        "productId": "1002",
        "productName": "AirPods Pro 2",
        "productImage": "http://example.com/product/1002.jpg",
        "price": 1899.00,
        "quantity": 1,
        "checked": true,
        "stock": 50,
        "totalPrice": 1899.00
      }
    ],
    "summary": {
      "totalCount": 3,
      "checkedCount": 3,
      "totalPrice": 17897.00,
      "totalDiscount": 0.00,
      "finalPrice": 17897.00
    }
  }
}
```

---

### 4.5 清空购物车

**接口地址**：`DELETE /api/cart/clear`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "清空成功",
  "data": null
}
```

---

## 五、订单服务 API

### 5.1 提交订单（分布式事务）

**接口地址**：`POST /api/order/submit`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "addressId": "ADDR10001",
  "cartIds": ["CART10001", "CART10002"],
  "remark": "请尽快发货",
  "paymentType": "online"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "订单提交成功",
  "data": {
    "orderId": "ORDER20260410001",
    "orderNo": "20260410100001",
    "userId": "10001",
    "items": [
      {
        "productId": "1001",
        "productName": "iPhone 15 Pro",
        "price": 7999.00,
        "quantity": 2,
        "totalPrice": 15998.00
      },
      {
        "productId": "1002",
        "productName": "AirPods Pro 2",
        "price": 1899.00,
        "quantity": 1,
        "totalPrice": 1899.00
      }
    ],
    "totalAmount": 17897.00,
    "freight": 0.00,
    "finalAmount": 17897.00,
    "status": "PENDING_PAYMENT",
    "statusDesc": "待支付",
    "address": {
      "receiver": "张三",
      "phone": "138****1234",
      "detailAddress": "北京市朝阳区 xxx 街道"
    },
    "createTime": "2026-04-10 10:00:00",
    "paymentExpireTime": "2026-04-10 10:30:00"
  }
}
```

**异常响应**：
```json
{
  "code": 1001,
  "message": "库存不足，订单提交失败",
  "data": {
    "productId": "1001",
    "required": 2,
    "available": 1
  }
}
```

---

### 5.2 订单列表查询

**接口地址**：`GET /api/order/list`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```
page: 1
pageSize: 10
status: "PENDING_PAYMENT" (可选：PENDING_PAYMENT/PAID/SHIPPED/COMPLETED/CANCELLED)
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "orderId": "ORDER20260410001",
        "orderNo": "20260410100001",
        "totalAmount": 17897.00,
        "status": "PENDING_PAYMENT",
        "statusDesc": "待支付",
        "itemCount": 2,
        "createTime": "2026-04-10 10:00:00"
      }
    ]
  }
}
```

---

### 5.3 订单详情查询

**接口地址**：`GET /api/order/detail/{orderId}`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "ORDER20260410001",
    "orderNo": "20260410100001",
    "userId": "10001",
    "items": [
      {
        "productId": "1001",
        "productName": "iPhone 15 Pro",
        "productImage": "http://example.com/product/1001.jpg",
        "price": 7999.00,
        "quantity": 2,
        "totalPrice": 15998.00
      }
    ],
    "totalAmount": 17897.00,
    "freight": 0.00,
    "finalAmount": 17897.00,
    "status": "PENDING_PAYMENT",
    "statusDesc": "待支付",
    "address": {
      "receiver": "张三",
      "phone": "138****1234",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detailAddress": "xxx 街道"
    },
    "paymentType": "online",
    "paymentTime": null,
    "deliveryTime": null,
    "receiveTime": null,
    "createTime": "2026-04-10 10:00:00",
    "cancelTime": null
  }
}
```

---

### 5.4 支付订单（模拟）

**接口地址**：`POST /api/order/pay`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "orderId": "ORDER20260410001",
  "paymentType": "online"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "支付成功",
  "data": {
    "orderId": "ORDER20260410001",
    "paymentNo": "PAY20260410100001",
    "amount": 17897.00,
    "paymentTime": "2026-04-10 10:05:00",
    "status": "PAID"
  }
}
```

---

### 5.5 取消订单

**接口地址**：`POST /api/order/cancel`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "orderId": "ORDER20260410001",
  "reason": "不想要了"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "订单已取消",
  "data": {
    "orderId": "ORDER20260410001",
    "cancelTime": "2026-04-10 10:00:00",
    "status": "CANCELLED"
  }
}
```

---

### 5.6 确认收货

**接口地址**：`POST /api/order/confirm`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "orderId": "ORDER20260410001"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "确认收货成功",
  "data": {
    "orderId": "ORDER20260410001",
    "receiveTime": "2026-04-15 10:00:00",
    "status": "COMPLETED"
  }
}
```

---

## 六、收藏服务 API

### 6.1 收藏商品

**接口地址**：`POST /api/favorite/add`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "productId": "1001"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "收藏成功",
  "data": {
    "favoriteId": "FAV10001",
    "userId": "10001",
    "productId": "1001",
    "productName": "iPhone 15 Pro",
    "productImage": "http://example.com/product/1001.jpg",
    "price": 7999.00,
    "createTime": "2026-04-10 10:00:00"
  }
}
```

---

### 6.2 取消收藏

**接口地址**：`DELETE /api/favorite/{favoriteId}`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "取消收藏成功",
  "data": null
}
```

---

### 6.3 收藏列表查询

**接口地址**：`GET /api/favorite/list`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```
page: 1
pageSize: 10
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "favoriteId": "FAV10001",
        "productId": "1001",
        "productName": "iPhone 15 Pro",
        "productImage": "http://example.com/product/1001.jpg",
        "price": 7999.00,
        "stock": 100,
        "status": 1,
        "statusDesc": "在售",
        "createTime": "2026-04-10 10:00:00"
      }
    ]
  }
}
```

---

### 6.4 检查收藏状态

**接口地址**：`GET /api/favorite/check/{productId}`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isFavorite": true,
    "favoriteId": "FAV10001"
  }
}
```

---

## 七、评价服务 API

### 7.1 发表商品评价

**接口地址**：`POST /api/review/add`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "orderId": "ORDER20260410001",
  "productId": "1001",
  "rating": 5,
  "content": "非常好用，物流也快！",
  "images": [
    "http://example.com/review/img1.jpg",
    "http://example.com/review/img2.jpg"
  ]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "评价成功",
  "data": {
    "reviewId": "REV10001",
    "userId": "10001",
    "productId": "1001",
    "orderId": "ORDER20260410001",
    "rating": 5,
    "content": "非常好用，物流也快！",
    "images": [
      "http://example.com/review/img1.jpg",
      "http://example.com/review/img2.jpg"
    ],
    "createTime": "2026-04-10 10:00:00"
  }
}
```

**异常响应**：
```json
{
  "code": 4001,
  "message": "已评价，不可重复提交",
  "data": null
}
```

---

### 7.2 商品评价列表

**接口地址**：`GET /api/review/list`

**请求参数**：
```
productId: "1001" (商品 ID，必填)
page: 1
pageSize: 10
rating: 5 (评分筛选，可选：1-5)
hasImage: true (是否有图，可选)
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "summary": {
      "totalCount": 100,
      "averageRating": 4.8,
      "ratingDistribution": {
        "5": 80,
        "4": 15,
        "3": 3,
        "2": 1,
        "1": 1
      },
      "hasImageCount": 50
    },
    "list": [
      {
        "reviewId": "REV10001",
        "userId": "10001",
        "username": "张***三",
        "avatar": "http://example.com/avatar/10001.jpg",
        "rating": 5,
        "content": "非常好用，物流也快！",
        "images": [
          "http://example.com/review/img1.jpg",
          "http://example.com/review/img2.jpg"
        ],
        "createTime": "2026-04-10 10:00:00",
        "reply": null
      }
    ]
  }
}
```

---

### 7.3 用户评价列表（个人中心）

**接口地址**：`GET /api/review/user/list`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```
page: 1
pageSize: 10
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 10,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "reviewId": "REV10001",
        "productId": "1001",
        "productName": "iPhone 15 Pro",
        "productImage": "http://example.com/product/1001.jpg",
        "orderId": "ORDER20260410001",
        "orderNo": "20260410100001",
        "rating": 5,
        "content": "非常好用，物流也快！",
        "images": [
          "http://example.com/review/img1.jpg"
        ],
        "createTime": "2026-04-10 10:00:00"
      }
    ]
  }
}
```

---

### 7.4 可评价商品列表

**接口地址**：`GET /api/review/pending-list`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```
page: 1
pageSize: 10
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "orderId": "ORDER20260410001",
        "orderNo": "20260410100001",
        "productId": "1001",
        "productName": "iPhone 15 Pro",
        "productImage": "http://example.com/product/1001.jpg",
        "price": 7999.00,
        "quantity": 2,
        "receiveTime": "2026-04-15 10:00:00",
        "reviewed": false
      }
    ]
  }
}
```

---

## 八、个人中心服务 API

### 8.1 个人中心首页

**接口地址**：`GET /api/user/center`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "10001",
    "username": "zhangsan",
    "nickname": "张三",
    "avatar": "http://example.com/avatar/10001.jpg",
    "statistics": {
      "pendingPayment": 2,
      "pendingShipment": 1,
      "pendingReceive": 3,
      "pendingReview": 5,
      "favoriteCount": 20,
      "addressCount": 3
    }
  }
}
```

---

### 8.2 收货地址列表

**接口地址**：`GET /api/user/addresses`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "1",
      "name": "张三",
      "phone": "13800138000",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detail": "xxx 街道 xxx 号",
      "isDefault": true,
      "createTime": "2026-04-01 10:00:00"
    }
  ]
}
```

---

### 8.3 添加收货地址

**接口地址**：`POST /api/user/address`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "name": "张三",
  "phone": "13800138000",
  "province": "北京市",
  "city": "北京市",
  "district": "朝阳区",
  "detail": "xxx 街道 xxx 号",
  "isDefault": true
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "id": "1",
    "name": "张三",
    "phone": "13800138000",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detail": "xxx 街道 xxx 号",
    "isDefault": true
  }
}
```

---

### 8.4 修改收货地址

**接口地址**：`PUT /api/user/address`

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "id": "1",
  "name": "张三",
  "phone": "13800138000",
  "province": "北京市",
  "city": "北京市",
  "district": "朝阳区",
  "detail": "xxx 街道 xxx 号",
  "isDefault": true
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "修改成功",
  "data": {
    "id": "1",
    "name": "张三",
    "phone": "13800138000",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detail": "xxx 街道 xxx 号",
    "isDefault": true
  }
}
```

---

### 8.5 删除收货地址

**接口地址**：`DELETE /api/user/address/{id}`

**请求头**：
```
Authorization: Bearer {token}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

## 九、认证与授权

### 9.1 Token 说明

系统采用 JWT Token 进行身份认证：

1. **获取 Token**：用户登录成功后返回 Token
2. **Token 格式**：`Bearer {token}`
3. **Token 有效期**：24 小时
4. **Token 刷新**：Token 过期后需重新登录

### 9.2 需要认证的接口

以下接口需要在请求头中携带 Token：

- 所有购物车相关接口
- 所有订单相关接口
- 所有收藏相关接口
- 所有评价相关接口
- 个人中心相关接口
- 修改个人信息接口

### 9.3 认证失败处理

当 Token 无效或过期时，返回 401 错误：

```json
{
  "code": 401,
  "message": "未登录或 Token 已失效",
  "data": null
}
```

前端收到 401 响应后，应跳转至登录页面。

---

## 十、限流与降级

### 10.1 限流规则

通过 Nginx 配置限流规则：

- 单 IP 每秒请求数限制：10 次/秒
- 单用户每分钟请求数限制：60 次/分钟
- 下单接口限制：5 次/分钟

### 10.2 限流响应

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": null
}
```

### 10.3 服务降级

当微服务不可用时，返回友好提示：

```json
{
  "code": 503,
  "message": "服务暂时不可用，请稍后再试",
  "data": null
}
```

---

## 十一、安全说明

### 11.1 请求安全

1. 所有敏感接口必须使用 HTTPS
2. 请求参数需进行合法性校验
3. 防止 SQL 注入和 XSS 攻击
4. 敏感数据（密码、手机号）需加密传输

### 11.2 数据安全

1. 用户密码使用 BCrypt 加密存储
2. Token 使用 JWT 签名，防止篡改
3. 敏感操作需验证用户身份
4. 防止越权访问（用户只能访问自己的数据）

### 11.3 日志记录

以下操作需记录日志：

- 用户登录/退出
- 订单创建/支付/取消
- 修改个人信息
- 异常请求和错误

---

## 十二、接口调用示例

### 12.1 完整购物流程

```bash
# 1. 用户登录
POST /api/user/login
{
  "username": "zhangsan",
  "password": "123456"
}
# 获取 token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 2. 浏览商品
GET /api/product/list?page=1&pageSize=10

# 3. 查看商品详情
GET /api/product/detail/1001

# 4. 加入购物车
POST /api/cart/add
Headers: Authorization: Bearer {token}
{
  "productId": "1001",
  "quantity": 1
}

# 5. 查看购物车
GET /api/cart/list
Headers: Authorization: Bearer {token}

# 6. 提交订单
POST /api/order/submit
Headers: Authorization: Bearer {token}
{
  "addressId": "ADDR10001",
  "cartIds": ["CART10001"],
  "remark": "请尽快发货"
}

# 7. 支付订单
POST /api/order/pay
Headers: Authorization: Bearer {token}
{
  "orderId": "ORDER20260410001",
  "paymentType": "online"
}

# 8. 确认收货
POST /api/order/confirm
Headers: Authorization: Bearer {token}
{
  "orderId": "ORDER20260410001"
}

# 9. 发表评价
POST /api/review/add
Headers: Authorization: Bearer {token}
{
  "orderId": "ORDER20260410001",
  "productId": "1001",
  "rating": 5,
  "content": "非常好用！"
}
```

---

## 附录

### A. 订单状态枚举

| 状态码 | 状态名称 | 说明 |
|--------|----------|------|
| PENDING_PAYMENT | 待支付 | 订单已创建，等待支付 |
| PAID | 已支付 | 支付成功，等待发货 |
| SHIPPED | 已发货 | 商品已发出，等待收货 |
| COMPLETED | 已完成 | 已确认收货 |
| CANCELLED | 已取消 | 订单已取消 |

### B. 评价星级说明

| 星级 | 说明 |
|------|------|
| 5 星 | 好评 |
| 4 星 | 良好 |
| 3 星 | 中评 |
| 2 星 | 较差 |
| 1 星 | 差评 |

### C. 分页参数说明

所有列表接口支持分页，分页参数统一：

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| page | int | 1 | 页码（从 1 开始） |
| pageSize | int | 10 | 每页数量 |

---

**文档版本**：v1.0  
**最后更新**：2026-04-10  
**维护团队**：在线商城系统开发团队
