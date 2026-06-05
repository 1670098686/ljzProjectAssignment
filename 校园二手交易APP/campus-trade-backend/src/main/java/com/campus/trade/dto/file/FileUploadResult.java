package com.campus.trade.dto.file;

public class FileUploadResult {

    private final String url;
    private final String thumbnailUrl;

    public FileUploadResult(String url, String thumbnailUrl) {
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
