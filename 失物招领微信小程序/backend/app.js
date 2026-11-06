// 应用入口文件
const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const path = require('path');
const { v4: uuidv4 } = require('uuid');

// 加载环境变量
dotenv.config();

// 导入配置
const config = require('./config');

// 导入中间件
const errorHandler = require('./middlewares/errorHandler');
const logger = require('./middlewares/logger');

// 导入路由
const routes = require('./routes');

// 创建 Express 应用
const app = express();

// 静态文件服务（上传的图片）- 放在最前面，避免被其他中间件干扰
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 应用中间件
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 生成 requestId
app.use((req, res, next) => {
  res.requestId = req.headers['x-request-id'] || uuidv4();
  next();
});

app.use(logger);

// 注册路由
app.use('/api/v1', routes);

// 健康检查接口
app.get('/health', (req, res) => {
  res.status(200).json({
    message: 'ok',
    data: {
      status: 'healthy',
      timestamp: new Date().toISOString()
    },
    requestId: res.requestId,
    timestamp: new Date().toISOString()
  });
});

// 404 处理
app.use((req, res, next) => {
  // 如果是 /uploads 路径下的 404，返回简单文本
  if (req.path.startsWith('/uploads')) {
    return res.status(404).send('文件不存在');
  }
  
  // 其他返回 JSON
  res.status(404).json({
    message: '接口不存在',
    data: null,
    requestId: res.requestId,
    timestamp: new Date().toISOString()
  });
});

// 全局错误处理
app.use(errorHandler);

// 启动服务器
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`服务器启动在端口 ${PORT}`);
  console.log(`环境：${process.env.NODE_ENV || 'development'}`);
});

module.exports = app;
