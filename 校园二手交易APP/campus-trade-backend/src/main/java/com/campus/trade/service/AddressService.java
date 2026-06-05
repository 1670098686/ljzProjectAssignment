package com.campus.trade.service;

import com.campus.trade.dto.user.AddressRequest;
import com.campus.trade.dto.user.AddressResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.entity.UserAddress;
import com.campus.trade.repository.UserAddressRepository;
import com.campus.trade.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;

    public AddressService(UserRepository userRepository, UserAddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public List<AddressResponse> listMyAddresses(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return addressRepository.findByUserIdOrderByIsDefaultDescCreateTimeDesc(user.getId())
                .stream()
                .map(AddressService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse create(String username, AddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validatePhone(request.getPhone());

        UserAddress address = new UserAddress();
        address.setUser(user);
        apply(address, request);

        boolean shouldDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (shouldDefault) {
            clearDefault(user.getId());
            address.setDefault(true);
        } else {
            // First address defaults to true.
            if (addressRepository.findByUserId(user.getId()).isEmpty()) {
                address.setDefault(true);
            }
        }

        addressRepository.save(address);
        return toResponse(address);
    }

    @Transactional
    public AddressResponse update(String username, Long addressId, AddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserAddress address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地址不存在"));

        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
        }

        apply(address, request);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(user.getId());
            address.setDefault(true);
        }

        addressRepository.save(address);
        return toResponse(address);
    }

    @Transactional
    public void delete(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserAddress address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地址不存在"));
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<UserAddress> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreateTimeDesc(user.getId());
            if (!remaining.isEmpty()) {
                UserAddress first = remaining.get(0);
                clearDefault(user.getId());
                first.setDefault(true);
                addressRepository.save(first);
            }
        }
    }

    @Transactional
    public AddressResponse setDefault(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserAddress address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地址不存在"));
        clearDefault(user.getId());
        address.setDefault(true);
        addressRepository.save(address);
        return toResponse(address);
    }

    private static AddressResponse toResponse(UserAddress address) {
        AddressResponse res = new AddressResponse();
        res.setId(address.getId());
        res.setReceiverName(address.getReceiverName());
        res.setPhone(address.getPhone());
        res.setProvince(address.getProvince());
        res.setCity(address.getCity());
        res.setDistrict(address.getDistrict());
        res.setDetailAddress(address.getDetailAddress());
        res.setDefault(address.isDefault());
        return res;
    }

    private static void apply(UserAddress address, AddressRequest request) {
        if (request.getReceiverName() != null) address.setReceiverName(request.getReceiverName());
        if (request.getPhone() != null) address.setPhone(request.getPhone());
        if (request.getProvince() != null) address.setProvince(request.getProvince());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getDistrict() != null) address.setDistrict(request.getDistrict());
        if (request.getDetailAddress() != null) address.setDetailAddress(request.getDetailAddress());
    }

    private void clearDefault(Long userId) {
        List<UserAddress> all = addressRepository.findByUserId(userId);
        for (UserAddress a : all) {
            if (a.isDefault()) {
                a.setDefault(false);
            }
        }
        addressRepository.saveAll(all);
    }

    private void validatePhone(String phone) {
        if (phone == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "手机号不能为空");
        }
        String cleaned = phone.trim();
        if (cleaned.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "手机号不能为空");
        }
        // Simple CN mobile check (11 digits) fallback.
        String digits = cleaned.replaceAll("\\s+", "");
        if (!digits.matches("\\d{11}")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "手机号格式不正确");
        }
    }
}
