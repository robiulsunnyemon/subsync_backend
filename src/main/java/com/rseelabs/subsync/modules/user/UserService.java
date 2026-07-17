package com.rseelabs.subsync.modules.user;

import com.rseelabs.subsync.core.exception.UserNotFoundException;
import com.rseelabs.subsync.core.service.CloudinaryService;
import com.rseelabs.subsync.modules.user.dto.UpdateProfileRequest;
import com.rseelabs.subsync.modules.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToResponse(user);
    }

    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        userRepository.save(user);
        return mapToResponse(user);
    }

    public UserProfileResponse uploadProfileImage(String email, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String imageUrl = cloudinaryService.uploadImage(file);
        user.setProfileImage(imageUrl);
        userRepository.save(user);

        return mapToResponse(user);
    }

    private UserProfileResponse mapToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .build();
    }
}
