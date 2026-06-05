package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.admin.SystemNotificationRequest;
import com.campus.trade.dto.message.SystemNotificationResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.SystemNotification;
import com.campus.trade.model.entity.User;
import com.campus.trade.repository.SystemNotificationRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.util.NotificationMapper;
import com.campus.trade.websocket.NotificationSocketPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class NotificationService {

    private final SystemNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSocketPublisher notificationSocketPublisher;

    public NotificationService(SystemNotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationSocketPublisher notificationSocketPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationSocketPublisher = notificationSocketPublisher;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SystemNotificationResponse> listNotifications(String username, int page, int size) {
        User user = findUser(username);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<SystemNotification> result = notificationRepository.findByUserId(user.getId(), pageable);
        return PaginatedResponse.of(result.map(NotificationMapper::toResponse).getContent(), page, size, result.getTotalElements());
    }

    @Transactional
    public void markRead(String username, Long notificationId) {
        User user = findUser(username);
        SystemNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "通知不存在"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN, "无权操作通知");
        }
        boolean updated = false;
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadTime(LocalDateTime.now());
            updated = true;
        }
        if (updated) {
            notificationSocketPublisher.publishRead(notification);
        }
    }

    @Transactional
    public void createNotification(SystemNotificationRequest request) {
        notifyUser(request.getUserId(), request.getTitle(), request.getContent());
    }

    @Transactional
    public void notifyUser(Long userId, String title, String content) {
        User user = findUser(userId);
        createAndPublishNotification(user, title, content);
    }

    @Transactional
    public void notifyUser(User user, String title, String content) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "接收用户不存在");
        }
        User managed = findUser(user.getId());
        createAndPublishNotification(managed, title, content);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "接收用户不存在"));
    }

    private void createAndPublishNotification(User user, String title, String content) {
        Objects.requireNonNull(user, "user" );
        SystemNotification notification = new SystemNotification();
        notification.setUser(user);
        notification.setTitle(defaultTitle(title));
        notification.setContent(defaultContent(content));
        notificationRepository.save(notification);
        notificationSocketPublisher.publishCreated(notification);
    }

    private String defaultTitle(String title) {
        return StringUtils.hasText(title) ? title.trim() : "系统通知";
    }

    private String defaultContent(String content) {
        return StringUtils.hasText(content) ? content.trim() : "您有一条新的系统通知，请及时查看。";
    }
}
