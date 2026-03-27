package com.finance.converter;

import com.finance.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 金额字段加密转换器
 * 用于JPA自动加密和解密金额字段
 */
@Component
@Converter
public class AmountEncryptConverter implements AttributeConverter<BigDecimal, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return encryptionService.encryptBigDecimal(amount);
    }
    
    @Override
    public BigDecimal convertToEntityAttribute(String encryptedAmount) {
        if (!org.springframework.util.StringUtils.hasText(encryptedAmount)) {
            return null;
        }
        return encryptionService.decryptBigDecimal(encryptedAmount);
    }
}