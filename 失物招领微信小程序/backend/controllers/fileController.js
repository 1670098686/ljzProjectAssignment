// 文件上传控制器
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const response = require('../utils/response');
const config = require('../config');
const { formatImageUrl } = require('../utils/imageUrl');

// 配置 multer 存储
const multer = require('multer');

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../uploads/images');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueName = `${Date.now()}-${uuidv4()}${path.extname(file.originalname)}`;
    cb(null, uniqueName);
  }
});

const upload = multer({
  storage,
  limits: {
    fileSize: config.upload.maxFileSize
  },
  fileFilter: (req, file, cb) => {
    if (config.upload.allowedTypes.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('不支持的图片格式，请使用 jpg/png/webp'), false);
    }
  }
});

exports.uploadImage = (req, res) => {
  upload.single('file')(req, res, (err) => {
    if (err) {
      return response.error(res, 400, err.message);
    }

    if (!req.file) {
      return response.error(res, 400, '请选择要上传的图片');
    }

    // 构建相对路径，然后格式化
    const relativePath = `/uploads/images/${req.file.filename}`;
    const imageUrl = formatImageUrl(relativePath, req);

    response.success(res, {
      fileId: req.file.filename,
      url: imageUrl,
      width: null,
      height: null,
      size: req.file.size
    });
  });
};
