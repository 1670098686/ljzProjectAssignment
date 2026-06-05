package com.campus.trade.service;

import com.campus.trade.dto.user.NotificationSettingRequest;
import com.campus.trade.dto.user.NotificationSettingResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.entity.UserNotificationSetting;
import com.campus.trade.repository.UserNotificationSettingRepository;
import com.campus.trade.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSettingService {

    private final UserRepository userRepository;
    private final UserNotificationSettingRepository repository;

    public NotificationSettingService(UserRepository userRepository, UserNotificationSettingRepository repository) {
        this.userRepository = userRepository;
        this.repository = repository;
    }

    @Transactional
    public NotificationSettingResponse getOrCreate(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserNotificationSetting setting = repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserNotificationSetting created = new UserNotificationSetting();
                    created.setUser(user);
                    return repository.save(created);
                });

        return toResponse(setting);
    }

    @Transactional
    public NotificationSettingResponse update(String username, NotificationSettingRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserNotificationSetting setting = repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserNotificationSetting created = new UserNotificationSetting();
                    created.setUser(user);
                    return repository.save(created);
                });

        if (request.getNotifyChat() != null) setting.setNotifyChat(request.getNotifyChat());
        if (request.getNotifyOrders() != null) setting.setNotifyOrders(request.getNotifyOrders());
        if (request.getNotifySystem() != null) setting.setNotifySystem(request.getNotifySystem());
        if (request.getNotifyMarketing() != null) setting.setNotifyMarketing(request.getNotifyMarketing());

        repository.save(setting);
        return toResponse(setting);
    }

    private static NotificationSettingResponse toResponse(UserNotificationSetting setting) {
        NotificationSettingResponse res = new NotificationSettingResponse();
        res.setNotifyChat(setting.isNotifyChat());
        res.setNotifyOrders(setting.isNotifyOrders());
        res.setNotifySystem(setting.isNotifySystem());
        res.setNotifyMarketing(setting.isNotifyMarketing());
        return res;
    }
}
