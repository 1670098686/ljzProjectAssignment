-- 修复verification_tokens表结构，使其与VerificationToken实体类一致
ALTER TABLE verification_tokens MODIFY COLUMN user_id BIGINT DEFAULT NULL;
ALTER TABLE verification_tokens ADD COLUMN email VARCHAR(100) DEFAULT NULL AFTER user_id;
ALTER TABLE verification_tokens ADD KEY idx_verification_email_type (email, type);