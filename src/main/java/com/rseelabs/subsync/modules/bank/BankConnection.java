package com.rseelabs.subsync.modules.bank;

import com.rseelabs.subsync.modules.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bank_connections")
public class BankConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankProvider provider; // e.g., TINK, PLAID

    @Column(nullable = false)
    private String institutionId;

    @Column(nullable = false)
    private String institutionName;

    private String accountId; // The ID of the account at the provider

    private String consentId; // Authorization consent ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BankProvider {
        TINK,
        PLAID,
        NORDIGEN
    }

    public enum ConnectionStatus {
        PENDING,
        CONNECTED,
        FAILED,
        EXPIRED
    }
}
