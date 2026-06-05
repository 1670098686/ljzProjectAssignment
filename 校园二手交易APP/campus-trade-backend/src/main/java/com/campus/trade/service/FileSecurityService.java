package com.campus.trade.service;

import com.campus.trade.config.FileSecurityProperties;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSecurityService {

    private final FileSecurityProperties properties;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedMimeTypes;
    private final EnumSet<FileKind> allowedKinds;
    private final AntivirusScanner antivirusScanner;

    @Autowired
    public FileSecurityService(FileSecurityProperties properties) {
        this(properties, null);
    }

    FileSecurityService(FileSecurityProperties properties, AntivirusScanner scanner) {
        this.properties = properties;
        this.allowedExtensions = normalizeStrings(properties.getAllowedExtensions());
        this.allowedMimeTypes = normalizeStrings(properties.getAllowedMimeTypes());
        this.allowedKinds = EnumSet.noneOf(FileKind.class);
        for (String extension : this.allowedExtensions) {
            FileKind.fromExtension(extension).ifPresent(allowedKinds::add);
        }
        this.antivirusScanner = scanner != null ? scanner : buildScanner(properties);
    }

    public void validateImageFile(MultipartFile file, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "文件超过大小限制");
        }
        String extension = resolveExtension(file);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "不允许的文件扩展名");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !allowedMimeTypes.contains(contentType.toLowerCase(Locale.ENGLISH))) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "不允许的文件类型");
        }
        FileKind detectedKind = detectFileKind(file);
        if (detectedKind == FileKind.UNKNOWN || (!allowedKinds.isEmpty() && !allowedKinds.contains(detectedKind))) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件签名不匹配");
        }
        if (!detectedKind.matchesExtension(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "扩展名与文件内容不一致");
        }
        ImageMetadata metadata = probeImageMetadata(file);
        if (metadata == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "无法解析图片内容");
        }
        enforceImageConstraints(metadata);
        runAntivirusScan(file);
    }

    private void enforceImageConstraints(ImageMetadata metadata) {
        if (metadata.width() > properties.getMaxImageWidth() || metadata.height() > properties.getMaxImageHeight()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "图片尺寸超过限制");
        }
        long pixels = metadata.pixelCount();
        if (pixels > properties.getMaxImagePixels()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "图片像素过大");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalName);
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "缺少文件扩展名");
        }
        return extension.toLowerCase(Locale.ENGLISH);
    }

    private FileKind detectFileKind(MultipartFile file) {
        try (InputStream input = new BufferedInputStream(file.getInputStream())) {
            byte[] header = input.readNBytes(16);
            return FileKind.fromHeader(header);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "无法读取文件内容");
        }
    }

    private ImageMetadata probeImageMetadata(MultipartFile file) {
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                return null;
            }
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return new ImageMetadata(width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            return null;
        }
    }

    private void runAntivirusScan(MultipartFile file) {
        if (!properties.isAntivirusEnabled()) {
            return;
        }
        try (InputStream input = file.getInputStream()) {
            ScanResult result = antivirusScanner.scan(file.getOriginalFilename(), input);
            if (!result.clean()) {
                throw new BusinessException(ErrorCode.FILE_VIRUS_DETECTED, result.message());
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_SCAN_FAILED, "文件扫描失败: " + ex.getMessage());
        }
    }

    private AntivirusScanner buildScanner(FileSecurityProperties props) {
        if (!props.isAntivirusEnabled()) {
            return (name, input) -> ScanResult.CLEAN;
        }
        return new ClamAvAntivirusScanner(props.getAntivirusHost(), props.getAntivirusPort(),
                (int) props.getAntivirusTimeout().toMillis());
    }

    private Set<String> normalizeStrings(Iterable<String> values) {
        Set<String> normalized = new HashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String cleaned = value.replace(".", "").trim().toLowerCase(Locale.ENGLISH);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return normalized;
    }

    private record ImageMetadata(int width, int height) {
        long pixelCount() {
            return (long) width * height;
        }
    }

    private interface AntivirusScanner {
        ScanResult scan(String filename, InputStream inputStream) throws IOException;
    }

    private record ScanResult(boolean clean, String message) {
        private static final ScanResult CLEAN = new ScanResult(true, "clean");
    }

    private enum FileKind {
        JPEG("ffd8ff", Set.of("jpg", "jpeg")),
        PNG("89504e470d0a1a0a", Set.of("png")),
        GIF("47494638", Set.of("gif")),
        WEBP("52494646", Set.of("webp")),
        UNKNOWN("", Set.of());

        private final String signatureHex;
        private final Set<String> extensions;

        FileKind(String signatureHex, Set<String> extensions) {
            this.signatureHex = signatureHex;
            this.extensions = extensions;
        }

        boolean matchesExtension(String extension) {
            return extensions.isEmpty() || extensions.contains(extension);
        }

        static Optional<FileKind> fromExtension(String extension) {
            if (!StringUtils.hasText(extension)) {
                return Optional.empty();
            }
            String normalized = extension.replace(".", "").toLowerCase(Locale.ENGLISH);
            for (FileKind kind : values()) {
                if (kind.extensions.contains(normalized)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }

        static FileKind fromHeader(byte[] header) {
            if (header == null || header.length == 0) {
                return UNKNOWN;
            }
            String hex = bytesToHex(header).toLowerCase(Locale.ENGLISH);
            for (FileKind kind : values()) {
                if (kind == UNKNOWN) {
                    continue;
                }
                if (hex.startsWith(kind.signatureHex)) {
                    if (kind == WEBP && !hexContainsWebp(header)) {
                        continue;
                    }
                    return kind;
                }
            }
            return UNKNOWN;
        }

        private static boolean hexContainsWebp(byte[] header) {
            // WEBP 文件以 RIFF 开头，第 9~12 字节为 WEBP
            if (header.length < 12) {
                return false;
            }
            return header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format(Locale.ENGLISH, "%02x", b));
            }
            return builder.toString();
        }
    }

    private static final class ClamAvAntivirusScanner implements AntivirusScanner {

        private final String host;
        private final int port;
        private final int timeoutMillis;

        private ClamAvAntivirusScanner(String host, int port, int timeoutMillis) {
            this.host = host;
            this.port = port;
            this.timeoutMillis = timeoutMillis <= 0 ? 5000 : timeoutMillis;
        }

        @Override
        public ScanResult scan(String filename, InputStream inputStream) throws IOException {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMillis);
                socket.setSoTimeout(timeoutMillis);
                try (var out = socket.getOutputStream(); var in = socket.getInputStream()) {
                    out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        out.write(ByteBuffer.allocate(4).putInt(read).array());
                        out.write(buffer, 0, read);
                    }
                    out.write(new byte[]{0, 0, 0, 0});
                    out.flush();
                    String response = readResponse(in);
                    if (response == null) {
                        throw new IOException("未收到病毒扫描结果");
                    }
                    if (response.contains("FOUND")) {
                        return new ScanResult(false, response.trim());
                    }
                    if (!response.contains("OK")) {
                        throw new IOException("扫描失败: " + response);
                    }
                    return ScanResult.CLEAN;
                }
            }
        }

        private String readResponse(InputStream in) throws IOException {
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = in.read()) != -1) {
                if (ch == '\n') {
                    break;
                }
                builder.append((char) ch);
            }
            return builder.length() == 0 ? null : builder.toString();
        }
    }
}
