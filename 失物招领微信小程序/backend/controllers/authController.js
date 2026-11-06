// 认证控制器
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');
const response = require('../utils/response');
const config = require('../config');
const User = require('../models/User');
const redisClient = require('../database/redis');

// 生成访问令牌
function generateAccessToken(userId, role) {
  return jwt.sign(
    { id: userId, role },
    config.jwt.secret,
    { expiresIn: config.jwt.expiresIn }
  );
}

// 生成刷新令牌
function generateRefreshToken(userId, role) {
  return jwt.sign(
    { id: userId, role, type: 'refresh' },
    config.jwt.secret,
    { expiresIn: config.jwt.refreshExpiresIn }
  );
}

exports.wxLogin = async (req, res) => {
  try {
    const { code, nickName, avatarUrl } = req.body;
    
    if (!code) {
      return response.error(res, 400, '缺少微信登录 code');
    }

    // 微信登录时，直接登录管理员账号
    // 通过邮箱查找管理员账号
    const user = await User.findByEmail('admin@example.com');
    
    if (!user) {
      return response.error(res, 404, '管理员账号不存在');
    }

    // 如果有新的昵称或头像，更新管理员信息
    if (nickName || avatarUrl) {
      await User.update(user.id, {
        nickname: nickName || user.nickname,
        avatar_url: avatarUrl || user.avatar_url
      });
      
      // 重新获取更新后的用户信息
      const updatedUser = await User.findById(user.id);
      if (updatedUser) {
        Object.assign(user, updatedUser);
      }
    }

    // 生成 token
    const accessToken = generateAccessToken(user.id, user.role);
    const refreshToken = generateRefreshToken(user.id, user.role);

    response.success(res, {
      accessToken,
      refreshToken,
      expiresIn: 7200,
      user: {
        id: user.id,
        role: user.role,
        email: user.email,
        emailVerified: user.email_verified === 1,
        nickName: user.nickname,
        avatarUrl: user.avatar_url
      }
    });
  } catch (error) {
    response.error(res, 500, `微信登录失败：${error.message}`);
  }
};

exports.sendEmailCode = async (req, res) => {
  try {
    const { email, purpose } = req.body;
    
    if (!email) {
      return response.error(res, 400, '缺少邮箱地址');
    }

    if (!purpose || !['REGISTER', 'RESET_PASSWORD'].includes(purpose)) {
      return response.error(res, 400, '用途参数错误');
    }

    // 演示模式：固定验证码 123456
    const code = '123456';
    
    // 存入 Redis
    const redisKey = `code:${email}:${purpose}`;
    await redisClient.set(redisKey, code, { EX: 300 });

    // 演示模式：不实际发送邮件
    console.log(`[演示模式] 邮箱 ${email} 的验证码是: ${code}`);

    response.success(res, { expireSeconds: 300, demoCode: code });
  } catch (error) {
    response.error(res, 500, `发送验证码失败：${error.message}`);
  }
};

exports.register = async (req, res) => {
  try {
    const { email, emailCode, password, wxCode, nickname } = req.body;
    
    if (!email || !password || !nickname) {
      return response.error(res, 400, '缺少必要参数');
    }

    if (nickname.length < 2) {
      return response.error(res, 400, '昵称至少2个字符');
    }

    // 演示模式：不强制验证邮箱码，任意邮箱都可注册
    // 验证邮箱验证码（可选）
    if (emailCode) {
      const redisKey = `code:${email}:REGISTER`;
      const savedCode = await redisClient.get(redisKey);
      
      if (!savedCode || savedCode !== emailCode) {
        // 演示模式：验证码错误也继续
        console.log(`[演示模式] 验证码验证通过或忽略`);
      }
    }

    // 演示模式：模拟 openid
    let openid = `register_openid_${email.substring(0, 10)}`;
    let unionid = `register_unionid_${email.substring(0, 10)}`;

    // 密码加密
    const hashedPassword = await bcrypt.hash(password, 10);
    
    // 创建用户 - 邮箱注册用户全部为普通用户
    const userId = await User.create({
      openid,
      unionid,
      email,
      password: hashedPassword,
      nickname: nickname,
      avatar_url: null,  // 明确设置为null
      role: 'USER'
    });

    // 清理验证码
    const redisKey = `code:${email}:REGISTER`;
    await redisClient.del(redisKey);

    response.success(res, { userId });
  } catch (error) {
    response.error(res, 500, `注册失败：${error.message}`);
  }
};

exports.passwordLogin = async (req, res) => {
  try {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return response.error(res, 400, '缺少邮箱或密码');
    }

    // 查找用户
    const user = await User.findByEmail(email);
    if (!user) {
      return response.error(res, 401, '邮箱或密码错误');
    }

    // 验证密码
    const validPassword = await bcrypt.compare(password, user.password);
    if (!validPassword) {
      return response.error(res, 401, '邮箱或密码错误');
    }

    // 检查用户状态
    if (user.status === 0) {
      return response.error(res, 403, '账号已被禁用');
    }

    // 生成 token
    const accessToken = generateAccessToken(user.id, user.role);
    const refreshToken = generateRefreshToken(user.id, user.role);

    response.success(res, {
      accessToken,
      refreshToken,
      expiresIn: 7200,
      user: {
        id: user.id,
        role: user.role,
        email: user.email,
        emailVerified: user.email_verified === 1,
        nickName: user.nickname,
        avatarUrl: user.avatar_url
      }
    });
  } catch (error) {
    response.error(res, 500, `登录失败：${error.message}`);
  }
};

exports.refreshToken = async (req, res) => {
  try {
    const { refreshToken } = req.body;
    
    if (!refreshToken) {
      return response.error(res, 400, '缺少刷新令牌');
    }

    // 验证刷新令牌
    const decoded = jwt.verify(refreshToken, config.jwt.secret);
    
    if (decoded.type !== 'refresh') {
      return response.error(res, 401, '无效的刷新令牌');
    }

    // 检查用户是否存在
    const user = await User.findById(decoded.id);
    if (!user || user.status === 0) {
      return response.error(res, 401, '用户不存在或已被禁用');
    }

    // 生成新的访问令牌
    const newAccessToken = generateAccessToken(user.id, user.role);

    response.success(res, {
      accessToken: newAccessToken,
      expiresIn: 7200
    });
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return response.error(res, 401, '刷新令牌已过期，请重新登录');
    }
    response.error(res, 401, '无效的刷新令牌');
  }
};

exports.getMe = async (req, res) => {
  try {
    const userId = req.user.id;
    
    const user = await User.findById(userId);
    if (!user) {
      return response.error(res, 404, '用户不存在');
    }

    response.success(res, {
      id: user.id,
      role: user.role,
      email: user.email,
      nickName: user.nickname,
      avatarUrl: user.avatar_url,
      signature: user.signature,
      phone: user.phone,
      createdAt: user.created_at
    });
  } catch (error) {
    response.error(res, 500, `获取用户信息失败：${error.message}`);
  }
};

exports.updateMe = async (req, res) => {
  try {
    const userId = req.user.id;
    const { nickName, avatarUrl, signature } = req.body;
    
    const updateData = {};
    if (nickName !== undefined) updateData.nickname = nickName;
    if (avatarUrl !== undefined) updateData.avatar_url = avatarUrl;
    if (signature !== undefined) updateData.signature = signature;

    await User.update(userId, updateData);
    
    const user = await User.findById(userId);

    response.success(res, {
      id: user.id,
      role: user.role,
      email: user.email,
      nickName: user.nickname,
      avatarUrl: user.avatar_url,
      signature: user.signature
    });
  } catch (error) {
    response.error(res, 500, `更新用户信息失败：${error.message}`);
  }
};

// 检查登录状态（不需要认证）
exports.checkLoginStatus = async (req, res) => {
  try {
    // 从授权头中获取token
    const authHeader = req.headers.authorization;
    let isLogin = false;
    let user = null;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      try {
        const jwt = require('jsonwebtoken');
        const config = require('../config');
        const decoded = jwt.verify(token, config.jwt.secret);
        
        // 验证用户是否存在
        const User = require('../models/User');
        const userData = await User.findById(decoded.id);
        
        if (userData && userData.status !== 0) {
          isLogin = true;
          user = {
            id: userData.id,
            role: userData.role
          };
        }
      } catch (e) {
        // token无效或过期
        isLogin = false;
      }
    }

    response.success(res, {
      isLogin,
      user
    });
  } catch (error) {
    // 出错时也返回未登录状态
    response.success(res, {
      isLogin: false
    });
  }
};

exports.getMyItems = async (req, res) => {
  try {
    const userId = req.user.id;
    const { itemType, auditStatus, bizStatus, page = 1, pageSize = 10 } = req.query;
    
    const Item = require('../models/Item');
    const ItemImage = require('../models/ItemImage');
    
    const result = await Item.list(parseInt(page), parseInt(pageSize), {
      userId,
      itemType: itemType || undefined,
      auditStatus: auditStatus || undefined,
      bizStatus: bizStatus || undefined,
      includePending: true,
      includeRejected: true
    });

    const formattedList = result.list.map(item => ({
      id: item.id,
      title: item.title,
      itemType: item.item_type,
      auditStatus: item.audit_status,
      bizStatus: item.biz_status,
      location: item.location,
      happenTime: item.happen_time,
      publishedAt: item.published_at,
      contactName: item.contact_name,
      contactPhone: item.contact_phone,
      contactWechat: item.contact_wechat
    }));

    response.page(res, formattedList, result.total, parseInt(page), parseInt(pageSize));
  } catch (error) {
    response.error(res, 500, `获取发布记录失败: ${error.message}`);
  }
};
