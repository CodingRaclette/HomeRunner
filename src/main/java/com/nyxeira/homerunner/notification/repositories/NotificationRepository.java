package com.nyxeira.homerunner.notification.repositories;

import com.nyxeira.homerunner.notification.model.Notification;
import com.nyxeira.homerunner.usermanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadAtIsNull(User recipient);
}
