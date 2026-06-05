package com.campus.trade.service;

import com.campus.trade.config.FileStorageProperties;
import com.campus.trade.dto.file.FileUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setUploadRoot(tempDir.toString());
        properties.setPublicPrefix("/upload");
        fileService = new FileService(properties);
    }

    @Test
    void uploadProductImage_shouldGenerateThumbnail() throws Exception {
        MockMultipartFile file = buildImage("product.png", 1200, 800);

        FileUploadResult result = fileService.uploadProductImage(1L, file);

        assertNotNull(result.getThumbnailUrl());
        assertTrue(result.getUrl().contains("products/1"));
        assertTrue(result.getThumbnailUrl().contains("thumb_"));

        Path originalPath = resolvePath(result.getUrl());
        Path thumbPath = resolvePath(result.getThumbnailUrl());
        assertTrue(Files.exists(originalPath));
        assertTrue(Files.exists(thumbPath));
    }

    @Test
    void uploadAvatar_shouldSkipThumbnailForGif() throws Exception {
        MockMultipartFile gif = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61});

        FileUploadResult result = fileService.uploadAvatar(5L, gif);

        assertNull(result.getThumbnailUrl());
        Path avatarPath = resolvePath(result.getUrl());
        assertTrue(Files.exists(avatarPath));
    }

    private MockMultipartFile buildImage(String filename, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.CYAN);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLUE);
            graphics.drawString("test", 10, 20);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", filename, "image/png", out.toByteArray());
    }

    private Path resolvePath(String url) {
        String relative = url.replaceFirst("^/upload/?", "");
        return tempDir.resolve(relative);
    }
}
