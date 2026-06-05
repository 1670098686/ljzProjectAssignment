package com.campus.trade.service;

import com.campus.trade.dto.presence.PresenceSocketEvent;
import com.campus.trade.dto.presence.PresenceStatusResponse;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.PresenceStatus;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.websocket.PresenceSocketPublisher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final UserRepository userRepository;
    private final PresenceSocketPublisher presenceSocketPublisher;

    private final ConcurrentMap<Long, PresenceRecord> presenceByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> sessionIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> usernameIndex = new ConcurrentHashMap<>();

    public PresenceService(UserRepository userRepository, PresenceSocketPublisher presenceSocketPublisher) {
        this.userRepository = userRepository;
        this.presenceSocketPublisher = presenceSocketPublisher;
    }

    public void handleSessionConnected(String username, String sessionId) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(sessionId)) {
            return;
        }
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            log.warn("Skip presence connect for unknown user '{}'", username);
            return;
        }
        User user = optionalUser.get();
        String displayName = resolveDisplayName(user);
        PresenceRecord record = presenceByUser.compute(user.getId(), (id, existing) -> {
            PresenceRecord current = existing;
            if (current == null) {
                current = PresenceRecord.fromUser(user, displayName);
            } else {
                current.updateDisplayName(displayName);
            }
            current.addSession(sessionId);
            return current;
        });
        sessionIndex.put(sessionId, user.getId());
        usernameIndex.put(user.getUsername(), user.getId());
        presenceSocketPublisher.broadcast(PresenceSocketEvent.online(record.toResponse()));
    }

    public void handleSessionDisconnected(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        Long userId = sessionIndex.remove(sessionId);
        if (userId == null) {
            return;
        }
        presenceByUser.computeIfPresent(userId, (id, record) -> {
            record.removeSession(sessionId);
            PresenceStatusResponse snapshot = record.toResponse();
            if (record.hasSessions()) {
                presenceSocketPublisher.broadcast(PresenceSocketEvent.heartbeat(snapshot));
                return record;
            }
            usernameIndex.remove(record.getUsername());
            presenceSocketPublisher.broadcast(PresenceSocketEvent.offline(snapshot));
            return null;
        });
    }

    public void handleHeartbeat(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        Long userId = usernameIndex.get(username);
        if (userId == null) {
            return;
        }
        PresenceRecord record = presenceByUser.get(userId);
        if (record == null) {
            return;
        }
        record.touch();
        presenceSocketPublisher.broadcast(PresenceSocketEvent.heartbeat(record.toResponse()));
    }

    public void sendSnapshot(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        presenceSocketPublisher.sendSnapshot(username, listOnlineUsers());
    }

    public List<PresenceStatusResponse> listOnlineUsers() {
        List<PresenceStatusResponse> responses = new ArrayList<>();
        for (PresenceRecord record : presenceByUser.values()) {
            responses.add(record.toResponse());
        }
        responses.sort(Comparator.comparing(PresenceStatusResponse::getDisplayName,
                Comparator.nullsLast(String::compareToIgnoreCase)));
        return responses;
    }

    public PresenceStatusResponse getStatus(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        PresenceRecord record = presenceByUser.get(userId);
        if (record != null) {
            return record.toResponse();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        PresenceStatusResponse response = new PresenceStatusResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(resolveDisplayName(user));
        response.setStatus(PresenceStatus.OFFLINE);
        response.setLastActive(user.getLastLogin());
        response.setSessionCount(0);
        return response;
    }

    public PresenceStatusResponse getStatusByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Long userId = usernameIndex.get(username);
        if (userId != null) {
            return getStatus(userId);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return getStatus(user.getId());
    }

    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        PresenceRecord record = presenceByUser.get(userId);
        return record != null && record.hasSessions();
    }

    public void touchByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        PresenceRecord record = presenceByUser.get(userId);
        if (record == null) {
            return;
        }
        record.touch();
        presenceSocketPublisher.broadcast(PresenceSocketEvent.heartbeat(record.toResponse()));
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "用户";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return user.getUsername();
    }

    private static final class PresenceRecord {

        private final Long userId;
        private final String username;
        private volatile String displayName;
        private final Set<String> sessionIds = ConcurrentHashMap.newKeySet();
        private volatile LocalDateTime lastActive;

        private PresenceRecord(Long userId, String username, String displayName) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.lastActive = LocalDateTime.now();
        }

        static PresenceRecord fromUser(User user, String displayName) {
            return new PresenceRecord(user.getId(), user.getUsername(), displayName);
        }

        void addSession(String sessionId) {
            sessionIds.add(sessionId);
            touch();
        }

        void removeSession(String sessionId) {
            sessionIds.remove(sessionId);
            touch();
        }

        boolean hasSessions() {
            return !sessionIds.isEmpty();
        }

        void touch() {
            this.lastActive = LocalDateTime.now();
        }

        String getUsername() {
            return username;
        }

        void updateDisplayName(String displayName) {
            if (StringUtils.hasText(displayName)) {
                this.displayName = displayName;
            }
        }

        PresenceStatusResponse toResponse() {
            PresenceStatusResponse response = new PresenceStatusResponse();
            response.setUserId(userId);
            response.setUsername(username);
            response.setDisplayName(displayName);
            response.setLastActive(lastActive);
            response.setSessionCount(sessionIds.size());
            response.setStatus(hasSessions() ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE);
            return response;
        }
    }
}
