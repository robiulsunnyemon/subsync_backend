package com.rseelabs.subsync.modules.bank;

import com.rseelabs.subsync.modules.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankConnectionRepository extends JpaRepository<BankConnection, UUID> {
    List<BankConnection> findByUser(User user);
    List<BankConnection> findByUserAndStatus(User user, BankConnection.ConnectionStatus status);
    Optional<BankConnection> findByIdAndUser(UUID id, User user);
}
