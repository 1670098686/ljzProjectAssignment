package com.finance.service.export;

import org.springframework.http.MediaType;

/**
 * 简单的导出结果封装，包含字节内容、文件名和响应的媒体类型。
 */
public record ExportedFile(byte[] content, String fileName, MediaType mediaType) {
}
