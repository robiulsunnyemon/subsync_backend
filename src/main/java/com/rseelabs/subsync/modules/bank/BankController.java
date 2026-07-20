package com.rseelabs.subsync.modules.bank;

import com.rseelabs.subsync.modules.bank.provider.BankProviderFactory;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import com.rseelabs.subsync.core.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankProviderFactory providerFactory;
    private final UserRepository userRepository;
    
    // Should ideally be stored in DB, mocking repository for now
    // private final BankConnectionRepository bankConnectionRepository; 

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping("/auth-link")
    public ResponseEntity<Map<String, String>> getAuthLink(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String provider) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BankConnection.BankProvider bankProvider = BankConnection.BankProvider.valueOf(provider.toUpperCase());
        String redirectUri = frontendUrl + "/api/v1/bank/callback"; // Typically handled by backend directly or frontend deep link
        
        // Use user ID as state to track who initiated
        String authLink = providerFactory.getProvider(bankProvider).generateAuthLink(redirectUri, user.getId().toString());

        return ResponseEntity.ok(Map.of("authUrl", authLink));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String state = payload.get("state"); // This is the user ID
        
        Long userId = Long.valueOf(state);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = providerFactory.getProvider(BankConnection.BankProvider.TINK).exchangeAuthorizationCode(code);

        // Store the BankConnection here...
        // BankConnection connection = new BankConnection();
        // connection.setUser(user);
        // connection.setProvider(BankConnection.BankProvider.TINK);
        // connection.setAccessToken(accessToken);
        // connection.setStatus(BankConnection.ConnectionStatus.ACTIVE);
        // bankConnectionRepository.save(connection);

        return ResponseEntity.ok().build();
    }
}
