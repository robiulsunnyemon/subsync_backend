package com.rseelabs.subsync.modules.bank;

import com.rseelabs.subsync.core.exception.ResourceNotFoundException;
import com.rseelabs.subsync.modules.bank.dto.BankConnectionResponse;
import com.rseelabs.subsync.modules.bank.provider.BankProviderFactory;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankConnectionService {

    private final BankConnectionRepository bankConnectionRepository;
    private final BankProviderFactory providerFactory;
    private final UserRepository userRepository;

    /**
     * Called after Tink redirects back with an authorization code.
     * Exchanges the code for an access token and saves the BankConnection to DB.
     */
    @Transactional
    public BankConnectionResponse handleCallback(String code, String state, String institutionId, String institutionName) {
        // state = userId
        Long userId = Long.valueOf(state);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = providerFactory.getProvider(BankConnection.BankProvider.TINK)
                .exchangeAuthorizationCode(code);

        BankConnection connection = BankConnection.builder()
                .user(user)
                .provider(BankConnection.BankProvider.TINK)
                .institutionId(institutionId != null ? institutionId : "unknown")
                .institutionName(institutionName != null ? institutionName : "Unknown Bank")
                .consentId(accessToken)  // store access token as consentId for now
                .status(BankConnection.ConnectionStatus.CONNECTED)
                .build();

        BankConnection saved = bankConnectionRepository.save(connection);
        log.info("Bank connection saved for user {} - institution: {}", userId, institutionName);

        return toResponse(saved);
    }

    /**
     * Returns all CONNECTED bank accounts for a user.
     */
    public List<BankConnectionResponse> getMyConnections(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return bankConnectionRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Disconnects (deletes) a bank connection for the authenticated user.
     */
    @Transactional
    public void disconnectBank(UUID connectionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BankConnection connection = bankConnectionRepository.findByIdAndUser(connectionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Bank connection not found"));

        bankConnectionRepository.delete(connection);
        log.info("Bank connection {} disconnected for user {}", connectionId, userEmail);
    }

    private BankConnectionResponse toResponse(BankConnection bc) {
        return BankConnectionResponse.builder()
                .id(bc.getId())
                .provider(bc.getProvider().name())
                .institutionId(bc.getInstitutionId())
                .institutionName(bc.getInstitutionName())
                .accountId(bc.getAccountId())
                .status(bc.getStatus().name())
                .connectedAt(bc.getCreatedAt())
                .build();
    }
}
