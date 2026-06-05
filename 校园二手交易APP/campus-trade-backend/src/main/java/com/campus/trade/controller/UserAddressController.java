package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.user.AddressRequest;
import com.campus.trade.dto.user.AddressResponse;
import com.campus.trade.security.AccessExpressions;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@Tag(name = "地址接口", description = "用户收货地址管理")
@PreAuthorize(AccessExpressions.MEMBER)
public class UserAddressController {

    private final AddressService addressService;

    public UserAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "获取我的地址列表")
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.success(addressService.listMyAddresses(SecurityUtils.getCurrentUsername()));
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(addressService.create(SecurityUtils.getCurrentUsername(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地址")
    public ApiResponse<AddressResponse> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(addressService.update(SecurityUtils.getCurrentUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        addressService.delete(SecurityUtils.getCurrentUsername(), id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "设为默认地址")
    public ApiResponse<AddressResponse> setDefault(@PathVariable Long id) {
        return ApiResponse.success(addressService.setDefault(SecurityUtils.getCurrentUsername(), id));
    }
}
