package com.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * 数据加密服务
 * 提供敏感数据的加密和解密功能
 * 支持金融数据的安全存储和传输
 */
@Service
public class EncryptionService {
    
    // 加密算法和密钥长度
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_LENGTH = 256;
    
    // 从环境变量或配置文件中获取密钥
    private static final String KEY_PROPERTY = "app.encryption.key";
    private static final String DEFAULT_KEY_PROPERTY = "app.encryption.default-key";
    
    private final String encryptionKey;
    
    public EncryptionService() {
        // 优先从系统属性获取密钥
        this.encryptionKey = getEncryptionKey();
    }
    
    /**
     * 初始化加密密钥
     */
    private String getEncryptionKey() {
        // 首先尝试从环境变量获取
        String key = System.getenv(KEY_PROPERTY);
        if (StringUtils.hasText(key)) {
            return key;
        }
        
        // 其次从系统属性获取
        key = System.getProperty(KEY_PROPERTY);
        if (StringUtils.hasText(key)) {
            return key;
        }
        
        // 使用默认密钥（仅用于开发环境）
        key = System.getProperty(DEFAULT_KEY_PROPERTY);
        if (StringUtils.hasText(key)) {
            return key;
        }
        
        // 生成临时密钥用于开发（生产环境应该配置固定密钥）
        return generateDefaultKey();
    }
    
    /**
     * 生成默认加密密钥
     */
    private String generateDefaultKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * 加密数据
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec secretKey = new SecretKeySpec(getKeyBytes(), ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedData = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * 解密数据
     */
    public String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            return encryptedText;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec secretKey = new SecretKeySpec(getKeyBytes(), ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, "UTF-8");
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * 将Base64编码的密钥转换为字节数组
     */
    private byte[] getKeyBytes() {
        return Base64.getDecoder().decode(encryptionKey);
    }
    
    /**
     * 加密BigDecimal数据
     */
    public String encryptBigDecimal(java.math.BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return encrypt(amount.toString());
    }
    
    /**
     * 解密BigDecimal数据
     */
    public java.math.BigDecimal decryptBigDecimal(String encryptedAmount) {
        if (!StringUtils.hasText(encryptedAmount)) {
            return null;
        }
        return new java.math.BigDecimal(decrypt(encryptedAmount));
    }
    
    /**
     * 生成唯一的数据标识符
     */
    public String generateDataId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}