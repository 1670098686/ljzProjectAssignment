// 配置文件
module.exports = {
  // 服务器配置
  server: {
    port: process.env.PORT || 3000,
    env: process.env.NODE_ENV || 'development',
    baseUrl: process.env.BASE_URL || 'http://localhost:3000'
  },

  // 数据库配置
  database: {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'lost_and_found',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
  },

  // Redis 配置
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: process.env.REDIS_PORT || 6379,
    password: process.env.REDIS_PASSWORD || ''
  },

  // JWT 配置
  jwt: {
    secret: process.env.JWT_SECRET || 'default_secret_change_in_production',
    expiresIn: process.env.JWT_EXPIRES_IN || '7d',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '30d'
  },

  // 文件上传配置
  upload: {
    path: process.env.UPLOAD_PATH || './uploads',
    maxFileSize: process.env.MAX_FILE_SIZE || 5 * 1024 * 1024, // 5MB
    allowedTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  },

  // 邮件配置
  email: {
    host: process.env.SMTP_HOST || 'smtp.qq.com',
    port: process.env.SMTP_PORT || 465,
    user: process.env.SMTP_USER || '',
    pass: process.env.SMTP_PASS || '',
    from: process.env.EMAIL_FROM || ''
  },

  // 微信小程序配置
  wechat: {
    appId: process.env.WX_APP_ID || '',
    appSecret: process.env.WX_APP_SECRET || ''
  },

  // 枚举值
  enums: {
    userRole: {
      USER: 'USER',
      ADMIN: 'ADMIN'
    },
    itemType: {
      LOST: 'LOST',
      FOUND: 'FOUND'
    },
    auditStatus: {
      PENDING: 'PENDING',
      APPROVED: 'APPROVED',
      REJECTED: 'REJECTED'
    },
    bizStatus: {
      ONGOING: 'ONGOING',
      RESOLVED: 'RESOLVED',
      RETURNED: 'RETURNED',
      CANCELLED: 'CANCELLED'
    },
    messageType: {
      TEXT: 'TEXT',
      IMAGE: 'IMAGE'
    },
    notificationType: {
      AUDIT: 'AUDIT',
      SYSTEM: 'SYSTEM',
      MESSAGE: 'MESSAGE'
    },
    reportStatus: {
      PENDING: 'PENDING',
      PROCESSED: 'PROCESSED',
      REJECTED: 'REJECTED'
    },
    auditAction: {
      APPROVE: 'APPROVE',
      REJECT: 'REJECT'
    }
  }
};
