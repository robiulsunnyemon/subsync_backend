package com.rseelabs.subsync.modules.bank;

import com.rseelabs.subsync.modules.bank.dto.BankConnectionResponse;
import com.rseelabs.subsync.modules.bank.dto.BankProviderDto;
import com.rseelabs.subsync.modules.bank.provider.BankProviderFactory;
import com.rseelabs.subsync.modules.user.User;
import com.rseelabs.subsync.modules.user.UserRepository;
import com.rseelabs.subsync.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Bank Connections", description = "Endpoints for managing bank connections")
public class BankController {

    private final BankProviderFactory providerFactory;
    private final UserRepository userRepository;
    private final BankConnectionService bankConnectionService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // ─────────────────────────────────────────────
    // 1. Get Tink auth URL to redirect user to bank
    // ─────────────────────────────────────────────
    @GetMapping("/auth-link")
    public ResponseEntity<Map<String, String>> getAuthLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String provider,
            @RequestParam(required = false, defaultValue = "GB") String market) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BankConnection.BankProvider bankProvider = BankConnection.BankProvider.valueOf(provider.toUpperCase());

        // Deep link so Tink redirects back to the mobile app
        String redirectUri = "subsync://bank-callback";

        String authLink = providerFactory.getProvider(bankProvider)
                .generateAuthLink(redirectUri, user.getId().toString(), market);

        return ResponseEntity.ok(Map.of("authUrl", authLink));
    }

    // ─────────────────────────────────────────────
    // 2. List available banks by country
    // ─────────────────────────────────────────────
    @GetMapping("/providers")
    public ResponseEntity<List<BankProviderDto>> getProviders(
            @RequestParam(required = false, defaultValue = "GB") String countryCode) {

        List<BankProviderDto> providers = providerFactory
                .getProvider(BankConnection.BankProvider.TINK)
                .fetchProviders(countryCode);

        return ResponseEntity.ok(providers);
    }

    // ─────────────────────────────────────────────
    // 3. Tink callback — exchange code & save to DB
    //    Called by the mobile app after Tink redirect
    // ─────────────────────────────────────────────
    @PostMapping("/callback")
    public ResponseEntity<BankConnectionResponse> handleCallback(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> payload) {

        String code = payload.get("code");
        String institutionId = payload.get("institutionId");
        String institutionName = payload.get("institutionName");

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BankConnectionResponse response = bankConnectionService
                .handleCallback(code, user.getId().toString(), institutionId, institutionName);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // 4. Get connected banks for current user
    // ─────────────────────────────────────────────
    @GetMapping("/my-connections")
    public ResponseEntity<List<BankConnectionResponse>> getMyConnections(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<BankConnectionResponse> connections = bankConnectionService
                .getMyConnections(userDetails.getUsername());

        return ResponseEntity.ok(connections);
    }

    // ─────────────────────────────────────────────
    // 5. Disconnect a bank
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> disconnectBank(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        bankConnectionService.disconnectBank(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Bank disconnected successfully"));
    }
}
