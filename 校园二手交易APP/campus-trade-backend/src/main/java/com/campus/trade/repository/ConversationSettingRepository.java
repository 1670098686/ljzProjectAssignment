package com.campus.trade.repository;

import com.campus.trade.model.entity.ConversationSetting;
import com.campus.trade.model.enums.RelatedType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationSettingRepository extends JpaRepository<ConversationSetting, Long> {

    Optional<ConversationSetting> findByOwnerIdAndPartnerId(Long ownerId, Long partnerId);

    Optional<ConversationSetting> findByOwnerIdAndPartnerIdAndRelatedTypeAndRelatedId(Long ownerId,
                                                                                      Long partnerId,
                                                                                      RelatedType relatedType,
                                                                                      Long relatedId);

    List<ConversationSetting> findByOwnerId(Long ownerId);
}
