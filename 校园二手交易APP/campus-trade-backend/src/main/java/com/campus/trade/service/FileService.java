package com.campus.trade.service;

import com.campus.trade.config.FileStorageProperties;
import com.campus.trade.dto.file.FileUploadResult;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileService {

    private final FileStorageProperties properties;

    public FileService(FileStorageProperties properties) {
        this.properties = properties;
    }

    private static final ImageCompressOption AVATAR_OPTION = new ImageCompressOption(600, 600, 0.85f);
    private static final ImageCompressOption PRODUCT_OPTION = new ImageCompressOption(1280, 1280, 0.85f);
    private static final ImageCompressOption PRODUCT_THUMB_OPTION = new ImageCompressOption(480, 480, 0.8f);
    private static final ImageCompressOption CHAT_IMAGE_OPTION = new ImageCompressOption(1280, 1280, 0.85f);
    private static final ImageCompressOption CHAT_IMAGE_THUMB_OPTION = new ImageCompressOption(480, 480, 0.8f);

    public FileUploadResult uploadAvatar(Long userId, MultipartFile file) {
        validateFile(file, 2 * 1024 * 1024);
        String filename = "avatar_" + UUID.randomUUID() + getExtension(file);
        String relativePath = String.format("users/%d/avatar/%s", userId, filename);
        storeFile(file, relativePath, isCompressible(file) ? AVATAR_OPTION : null);
        String url = buildPublicUrl(relativePath);
        return new FileUploadResult(url, null);
    }

    public FileUploadResult uploadProductImage(Long productId, MultipartFile file) {
        validateFile(file, 5 * 1024 * 1024);
        String filename;
        String relativePath;
        
        // 如果没有商品ID，使用临时目录和随机命名
        if (productId == null) {
            String tempId = UUID.randomUUID().toString().replace("-", "");
            filename = String.format(Locale.ENGLISH, "temp_%s%s", tempId, getExtension(file));
            relativePath = String.format("products/temp/%s", filename);
        } else {
            filename = String.format(Locale.ENGLISH, "product_%d_%s%s", productId,
                    UUID.randomUUID().toString().replace("-", ""), getExtension(file));
            relativePath = String.format("products/%d/images/%s", productId, filename);
        }
        
        boolean compressible = isCompressible(file);
        storeFile(file, relativePath, compressible ? PRODUCT_OPTION : null);

        String url = buildPublicUrl(relativePath);
        String thumbnailUrl = null;
        if (compressible) {
            String thumbPath = relativePath.replace(filename, "thumb_" + filename);
            generateThumbnail(relativePath, thumbPath, PRODUCT_THUMB_OPTION);
            thumbnailUrl = buildPublicUrl(thumbPath);
        }
        return new FileUploadResult(url, thumbnailUrl);
    }

    public FileUploadResult uploadChatImage(Long userId, MultipartFile file) {
        validateFile(file, 5 * 1024 * 1024);
        String filename = "chat_img_" + UUID.randomUUID() + getExtension(file);
        String relativePath = String.format("chat/%d/images/%s", userId, filename);
        boolean compressible = isCompressible(file);
        storeFile(file, relativePath, compressible ? CHAT_IMAGE_OPTION : null);

        String url = buildPublicUrl(relativePath);
        String thumbnailUrl = null;
        if (compressible) {
            String thumbPath = relativePath.replace(filename, "thumb_" + filename);
            generateThumbnail(relativePath, thumbPath, CHAT_IMAGE_THUMB_OPTION);
            thumbnailUrl = buildPublicUrl(thumbPath);
        }
        return new FileUploadResult(url, thumbnailUrl);
    }

    public FileUploadResult uploadChatAudio(Long userId, MultipartFile file) {
        validateAudioFile(file, 10 * 1024 * 1024);
        String filename = "chat_audio_" + UUID.randomUUID().toString().replace("-", "") + getExtension(file);
        String relativePath = String.format("chat/%d/audio/%s", userId, filename);
        storeFile(file, relativePath, null);
        String url = buildPublicUrl(relativePath);
        return new FileUploadResult(url, null);
    }

    private void validateFile(MultipartFile file, long maxSize) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "文件超过大小限制");
        }
        String extension = getExtension(file).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|gif)$")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 jpg/png/gif");
        }
    }

    private void validateAudioFile(MultipartFile file, long maxSize) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "文件超过大小限制");
        }
        String extension = getExtension(file).toLowerCase(Locale.ENGLISH);
        if (!extension.matches("\\.(m4a|mp3|wav|aac|ogg|opus)$")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 m4a/mp3/wav/aac/ogg/opus");
        }
    }

    private String getExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);
        return ext == null ? "" : "." + ext;
    }

    private void storeFile(MultipartFile file, String relativePath, ImageCompressOption option) {
        Path destination = Paths.get(properties.getUploadRoot()).resolve(relativePath).normalize();
        try {
            Files.createDirectories(destination.getParent());
            if (option != null) {
                compressAndSave(file, destination, option);
            } else {
                file.transferTo(destination);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
    }

    private void compressAndSave(MultipartFile file, Path destination, ImageCompressOption option) throws IOException {
        BufferedImage image;
        try (InputStream in = file.getInputStream()) {
            image = ImageIO.read(in);
        }
        if (image == null) {
            try (InputStream raw = file.getInputStream()) {
                Files.copy(raw, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        Thumbnails.of(image)
                .size(option.maxWidth, option.maxHeight)
                .outputQuality(option.quality)
                .toFile(destination.toFile());
    }

    private void generateThumbnail(String sourceRelativePath, String targetRelativePath, ImageCompressOption option) {
        Path source = Paths.get(properties.getUploadRoot()).resolve(sourceRelativePath).normalize();
        Path target = Paths.get(properties.getUploadRoot()).resolve(targetRelativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Thumbnails.of(source.toFile())
                    .size(option.maxWidth, option.maxHeight)
                    .outputQuality(option.quality)
                    .toFile(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "缩略图生成失败");
        }
    }

    private boolean isCompressible(MultipartFile file) {
        String extension = getExtension(file).toLowerCase();
        return extension.matches("\\.(jpg|jpeg|png)$");
    }

    private String buildPublicUrl(String relativePath) {
        return properties.getPublicPrefix() + "/" + relativePath.replace('\\', '/');
    }

    private static class ImageCompressOption {
        private final int maxWidth;
        private final int maxHeight;
        private final float quality;

        private ImageCompressOption(int maxWidth, int maxHeight, float quality) {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.quality = quality;
        }
    }
}
