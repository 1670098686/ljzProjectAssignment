package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.file.FileUploadResult;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.FileService;
import com.campus.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "文件接口", description = "文件上传相关接口")
@PreAuthorize(AccessExpressions.MEMBER)
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    public FileController(FileService fileService, UserService userService) {
        this.fileService = fileService;
        this.userService = userService;
    }

    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像", description = "上传用户头像文件")
    public ApiResponse<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestPart("file") MultipartFile file) {
        Long userId = userService.getCurrentUserSummary(SecurityUtils.getCurrentUsername()).getId();
        FileUploadResult result = fileService.uploadAvatar(userId, file);
        return ApiResponse.success(buildBody(result));
    }

    @PostMapping(value = "/upload/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传商品图片", description = "上传商品图片文件")
    public ApiResponse<Map<String, String>> uploadProductImage(
            @Parameter(description = "商品ID（新建商品时可选）") @RequestParam(required = false) Long productId,
            @Parameter(description = "商品图片文件") @RequestPart("file") MultipartFile file) {
        FileUploadResult result = fileService.uploadProductImage(productId, file);
        return ApiResponse.success(buildBody(result));
    }

    @PostMapping(value = "/upload/chat-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传聊天图片", description = "上传聊天图片附件")
    public ApiResponse<Map<String, String>> uploadChatImage(
            @Parameter(description = "图片文件") @RequestPart("file") MultipartFile file) {
        Long userId = userService.getCurrentUserSummary(SecurityUtils.getCurrentUsername()).getId();
        FileUploadResult result = fileService.uploadChatImage(userId, file);
        return ApiResponse.success(buildBody(result));
    }

    @PostMapping(value = "/upload/chat-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传聊天语音", description = "上传聊天语音附件")
    public ApiResponse<Map<String, String>> uploadChatAudio(
            @Parameter(description = "语音文件") @RequestPart("file") MultipartFile file) {
        Long userId = userService.getCurrentUserSummary(SecurityUtils.getCurrentUsername()).getId();
        FileUploadResult result = fileService.uploadChatAudio(userId, file);
        return ApiResponse.success(buildBody(result));
    }

    private Map<String, String> buildBody(FileUploadResult result) {
        Map<String, String> body = new HashMap<>();
        body.put("url", result.getUrl());
        if (result.getThumbnailUrl() != null) {
            body.put("thumbnailUrl", result.getThumbnailUrl());
        }
        return body;
    }
}
