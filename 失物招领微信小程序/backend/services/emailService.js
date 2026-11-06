// 邮箱服务
const nodemailer = require('nodemailer');
const config = require('../config');

const transporter = nodemailer.createTransport({
  host: config.email.host,
  port: config.email.port,
  secure: true,
  auth: {
    user: config.email.user,
    pass: config.email.pass
  }
});

const emailService = {
  async sendVerificationCode(email, code) {
    const mailOptions = {
      from: config.email.from,
      to: email,
      subject: '校园失物招领 - 邮箱验证码',
      html: `
        <div style="padding: 20px; font-family: Arial, sans-serif;">
          <h2>邮箱验证码</h2>
          <p>您的验证码是：</p>
          <div style="font-size: 32px; font-weight: bold; color: #1890ff; letter-spacing: 5px; margin: 20px 0;">
            ${code}
          </div>
          <p>验证码有效期为 5 分钟，请尽快使用。</p>
          <p>如果这不是您本人的操作，请忽略此邮件。</p>
        </div>
      `
    };

    return transporter.sendMail(mailOptions);
  },

  async sendAuditNotification(email, title, content) {
    const mailOptions = {
      from: config.email.from,
      to: email,
      subject: `校园失物招领 - ${title}`,
      html: `<div style="padding: 20px; font-family: Arial, sans-serif;"><h2>${title}</h2><p>${content}</p></div>`
    };

    return transporter.sendMail(mailOptions);
  }
};

module.exports = emailService;
