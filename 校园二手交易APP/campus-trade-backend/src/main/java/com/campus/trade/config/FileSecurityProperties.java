package com.campus.trade.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file.security")
public class FileSecurityProperties {

    private List<String> allowedExtensions = new ArrayList<>(List.of("jpg", "jpeg", "png", "gif"));
    private List<String> allowedMimeTypes = new ArrayList<>(List.of("image/jpeg", "image/png", "image/gif"));
    private long maxImageWidth = 6000;
    private long maxImageHeight = 6000;
    private long maxImagePixels = 20_000_000;
    private boolean antivirusEnabled = false;
    private Duration antivirusTimeout = Duration.ofSeconds(5);
    private String antivirusHost = "localhost";
    private int antivirusPort = 3310;

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public long getMaxImageWidth() {
        return maxImageWidth;
    }

    public void setMaxImageWidth(long maxImageWidth) {
        this.maxImageWidth = maxImageWidth;
    }

    public long getMaxImageHeight() {
        return maxImageHeight;
    }

    public void setMaxImageHeight(long maxImageHeight) {
        this.maxImageHeight = maxImageHeight;
    }

    public long getMaxImagePixels() {
        return maxImagePixels;
    }

    public void setMaxImagePixels(long maxImagePixels) {
        this.maxImagePixels = maxImagePixels;
    }

    public boolean isAntivirusEnabled() {
        return antivirusEnabled;
    }

    public void setAntivirusEnabled(boolean antivirusEnabled) {
        this.antivirusEnabled = antivirusEnabled;
    }

    public Duration getAntivirusTimeout() {
        return antivirusTimeout;
    }

    public void setAntivirusTimeout(Duration antivirusTimeout) {
        this.antivirusTimeout = antivirusTimeout;
    }

    public String getAntivirusHost() {
        return antivirusHost;
    }

    public void setAntivirusHost(String antivirusHost) {
        this.antivirusHost = antivirusHost;
    }

    public int getAntivirusPort() {
        return antivirusPort;
    }

    public void setAntivirusPort(int antivirusPort) {
        this.antivirusPort = antivirusPort;
    }
}
