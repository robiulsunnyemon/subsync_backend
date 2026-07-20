package com.rseelabs.subsync.modules.subscription;

import com.rseelabs.subsync.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    
    List<Subscription> findAllByUser(User user);
    
    List<Subscription> findAllByUserAndStatus(User user, Subscription.SubscriptionStatus status);
    
    List<Subscription> findAllByUserAndType(User user, Subscription.ExpenseType type);

    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.nextBillingDate >= :startDate AND s.nextBillingDate <= :endDate AND s.status = :status")
    List<Subscription> findUpcomingSubscriptions(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") Subscription.SubscriptionStatus status);
}
