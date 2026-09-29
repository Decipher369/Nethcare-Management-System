package com.nethcare.repository;

import com.nethcare.model.Notification;
import com.nethcare.model.NotificationChannel;
import com.nethcare.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByFollowUpIdOrderByIdAsc(Long followUpId);

    List<Notification> findByStatusOrderByScheduledForAsc(NotificationStatus status);

    List<Notification> findByStatusInOrderByScheduledForAsc(List<NotificationStatus> statuses);

    List<Notification> findByChannelOrderByIdDesc(NotificationChannel channel);

    List<Notification> findAllByOrderByIdDesc();

    long countByStatusIn(List<NotificationStatus> statuses);

    boolean existsByFollowUpIdAndStatusIn(Long followUpId, List<NotificationStatus> statuses);

    boolean existsByOrderId(Long orderId);

    List<Notification> findByStatusInAndScheduledForLessThanEqualOrderByScheduledForAsc(
            List<NotificationStatus> statuses, LocalDateTime scheduledFor);
}
