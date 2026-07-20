package com.rseelabs.subsync.modules.subscription;

import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.rseelabs.subsync.core.exception.ResourceNotFoundException;
import com.rseelabs.subsync.core.exception.UnauthorizedAccessException;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Subscription>> getAllSubscriptions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(subscriptionRepository.findAllByUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getSubscription(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
                
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You don't have access to this subscription");
        }
        
        return ResponseEntity.ok(subscription);
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
                
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You don't have access to this subscription");
        }
        
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
        
        return ResponseEntity.ok(subscription);
    }
}
