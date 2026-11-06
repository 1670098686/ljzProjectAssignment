package com.mall.user.controller;

import com.mall.common.response.Result;
import com.mall.common.utils.JwtUtils;
import com.mall.user.entity.Address;
import com.mall.user.entity.User;
import com.mall.user.repository.AddressRepository;
import com.mall.user.repository.UserRepository;
import com.mall.user.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String phone = request.get("phone");

        if (userRepository.findByUsername(username).isPresent()) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtils.encode(password));
        user.setPhone(phone);
        user.setStatus(1);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("createTime", user.getCreatedAt());

        return Result.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!PasswordUtils.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("phone", user.getPhone());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("token", token);
        result.put("expireTime", System.currentTimeMillis() + 86400000);
        result.put("user", userInfo);

        return Result.success(result);
    }

    /**
     * 用户退出
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        // 前端清除token即可
        return Result.success();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/profile")
    public Result<?> getProfile(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("createTime", user.getCreatedAt());

        return Result.success(result);
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> data) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (data.containsKey("phone")) {
            user.setPhone(data.get("phone"));
        }
        user.setUpdatedAt(new Date());

        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());

        return Result.success(result);
    }

    /**
     * 获取地址列表
     */
    @GetMapping("/addresses")
    public Result<?> getAddresses(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        List<Address> addresses = addressRepository.findByUserId(userId);
        return Result.success(addresses);
    }

    /**
     * 添加地址
     */
    @PostMapping("/address")
    public Result<?> addAddress(HttpServletRequest request, @RequestBody Address address) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        address.setUserId(userId);
        address.setCreatedAt(new Date());
        address.setUpdatedAt(new Date());

        // 如果设置为默认地址，先将用户其他地址的默认标记取消
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> userAddresses = addressRepository.findByUserId(userId);
            for (Address addr : userAddresses) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }

        Address savedAddress = addressRepository.save(address);
        return Result.success(savedAddress);
    }

    /**
     * 修改地址
     */
    @PutMapping("/address")
    public Result<?> updateAddress(HttpServletRequest request, @RequestBody Address address) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        Address existingAddress = addressRepository.findById(address.getId())
                .orElseThrow(() -> new RuntimeException("地址不存在"));

        if (!existingAddress.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此地址");
        }

        existingAddress.setName(address.getName());
        existingAddress.setPhone(address.getPhone());
        existingAddress.setProvince(address.getProvince());
        existingAddress.setCity(address.getCity());
        existingAddress.setDistrict(address.getDistrict());
        existingAddress.setDetail(address.getDetail());
        existingAddress.setIsDefault(address.getIsDefault());
        existingAddress.setUpdatedAt(new Date());

        // 如果设置为默认地址，先将用户其他地址的默认标记取消
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> userAddresses = addressRepository.findByUserId(userId);
            for (Address addr : userAddresses) {
                if (!addr.getId().equals(address.getId()) && Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }

        Address updatedAddress = addressRepository.save(existingAddress);
        return Result.success(updatedAddress);
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/address/{id}")
    public Result<?> deleteAddress(HttpServletRequest request, @PathVariable(name = "id") Long id) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("地址不存在"));

        if (!address.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此地址");
        }

        addressRepository.delete(address);
        return Result.success();
    }

    /**
     * 个人中心首页
     */
    @GetMapping("/center")
    public Result<?> getCenter(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<Address> addresses = addressRepository.findByUserId(userId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("pendingPayment", 0);
        statistics.put("pendingShipment", 0);
        statistics.put("pendingReceive", 0);
        statistics.put("pendingReview", 0);
        statistics.put("favoriteCount", 0);
        statistics.put("addressCount", addresses.size());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getUsername());
        result.put("avatar", "");
        result.put("statistics", statistics);

        return Result.success(result);
    }

    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/info/{userId}")
    public Result<?> getUserInfo(@PathVariable(name = "userId") Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("createTime", user.getCreatedAt());

        return Result.success(result);
    }
}
