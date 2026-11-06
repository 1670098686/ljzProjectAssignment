-- 校园失物招领小程序实例数据

-- ==================== 图书馆数据 ====================

-- 图书馆 - 失物 - 证件卡片
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '校园卡丢失', '证件卡片', '2026-05-26 14:30:00', '图书馆', '卡号尾号3847，姓名张三，蓝色卡套', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-26 14:35:00');

-- 图书馆 - 失物 - 数码产品
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '黑色无线鼠标丢失', '数码产品', '2026-05-25 10:15:00', '图书馆二楼自习室', '罗技MX Anywhere 2S，底部有轻微划痕', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-25 10:20:00');

-- 图书馆 - 拾物 - 书籍文具
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'FOUND', '捡到高等数学教材', '书籍文具', '2026-05-27 09:00:00', '图书馆三楼阅览室', '同济大学出版社，封面有笔记', '管理员', '13800138001', 'APPROVED', 'ONGOING', '2026-05-27 09:05:00');

-- ==================== 教学楼数据 ====================

-- 教学楼 - 失物 - 生活用品
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '保温杯丢失', '生活用品', '2026-05-26 16:00:00', '教学楼A栋201教室', '象印牌，深蓝色，500ml', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-26 16:05:00');

-- 教学楼 - 拾物 - 证件卡片
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'FOUND', '捡到学生证', '证件卡片', '2026-05-27 11:30:00', '教学楼B栋走廊', '学号2023010001，姓名王五', '管理员', '13800138001', 'APPROVED', 'ONGOING', '2026-05-27 11:35:00');

-- 教学楼 - 失物 - 数码产品
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '蓝牙耳机丢失', '数码产品', '2026-05-24 08:45:00', '教学楼C栋阶梯教室', 'AirPods Pro，白色充电盒有刻字', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-24 08:50:00');

-- ==================== 食堂数据 ====================

-- 食堂 - 失物 - 服饰鞋帽
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '蓝色外套丢失', '服饰鞋帽', '2026-05-26 12:30:00', '一食堂二楼', '优衣库，L码，左胸有小logo', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-26 12:35:00');

-- 食堂 - 拾物 - 生活用品
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'FOUND', '捡到一把雨伞', '生活用品', '2026-05-27 18:00:00', '二食堂门口', '透明雨伞，粉色手柄', '管理员', '13800138001', 'APPROVED', 'ONGOING', '2026-05-27 18:05:00');

-- ==================== 宿舍区数据 ====================

-- 宿舍区 - 失物 - 数码产品
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '笔记本电脑充电器丢失', '数码产品', '2026-05-25 22:00:00', '5号楼宿舍楼下', '联想原装65W Type-C充电器', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-25 22:05:00');

-- 宿舍区 - 拾物 - 书籍文具
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'FOUND', '捡到英语四级词汇书', '书籍文具', '2026-05-27 07:30:00', '3号楼自习室', '新东方，封面有签名', '管理员', '13800138001', 'APPROVED', 'ONGOING', '2026-05-27 07:35:00');

-- ==================== 操场数据 ====================

-- 操场 - 失物 - 服饰鞋帽
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '运动水杯丢失', '生活用品', '2026-05-26 17:30:00', '操场看台', '橙色耐克水杯，750ml', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-26 17:35:00');

-- 操场 - 拾物 - 生活用品
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'FOUND', '捡到一副运动眼镜', '生活用品', '2026-05-27 16:00:00', '足球场边', '黑色镜框，蓝色镜腿', '管理员', '13800138001', 'APPROVED', 'ONGOING', '2026-05-27 16:05:00');

-- ==================== 校门口数据 ====================

-- 校门口 - 失物 - 证件卡片
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'LOST', '身份证丢失', '证件卡片', '2026-05-24 18:00:00', '校门口', '身份证尾号1234，姓名张三', '张三', '13800138001', 'APPROVED', 'ONGOING', '2026-05-24 18:05:00');

-- 校门口 - 拾物 - 其他
INSERT INTO items (user_id, item_type, title, category, happen_time, location, feature_desc, contact_name, contact_phone, audit_status, biz_status, published_at) 
VALUES (1, 'FOUND', '捡到一个挂饰', '其他', '2026-05-27 08:00:00', '校门口公交站', '小熊形状的挂饰，蓝色', '管理员', '13800138001', 'APPROVED', 'ONGOING', '2026-05-27 08:05:00');
