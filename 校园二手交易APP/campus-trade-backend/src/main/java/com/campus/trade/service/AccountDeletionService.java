package com.campus.trade.service;

import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;

    public AccountDeletionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedDelayString = "${app.account-deletion.check-interval:3600000}")
    @Transactional
    public void processScheduledDeletions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<User> dueUsers = userRepository.findByDeleteRequestedTrueAndDeleteScheduleTimeBefore(now);
            if (dueUsers.isEmpty()) {
                return;
            }
            dueUsers.forEach(this::finalizeDeletionState);
            log.info("Processed {} scheduled account deletions", dueUsers.size());
        } catch (Exception e) {
            log.error("Error processing scheduled account deletions", e);
        }
    }

    @Transactional
    public void finalizeDeletionNow(User user) {
        finalizeDeletionState(user);
    }

    private void finalizeDeletionState(User user) {
        user.setStatus(AccountStatus.DISABLED);
        user.setDeleteRequested(false);
        user.setDeleteScheduleTime(null);
        user.setDeleteReason(null);
        user.setContactInfo(null);
        user.setPhone(null);
    }
}
