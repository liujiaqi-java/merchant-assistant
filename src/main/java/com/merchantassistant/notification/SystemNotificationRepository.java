package com.merchantassistant.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    List<SystemNotification> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    Optional<SystemNotification> findByIdAndStoreId(Long id, Long storeId);
    Optional<SystemNotification> findFirstByStoreIdAndTypeAndReferenceTypeAndReferenceIdAndReadFlagFalse(
            Long storeId, NotificationType type, String referenceType, Long referenceId);
    List<SystemNotification> findByStoreIdAndReferenceTypeAndReferenceIdAndReadFlagFalse(
            Long storeId, String referenceType, Long referenceId);
    List<SystemNotification> findByStoreIdAndReadFlagFalse(Long storeId);
}
