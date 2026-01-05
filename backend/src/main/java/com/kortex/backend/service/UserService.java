package com.kortex.backend.service;

import com.kortex.backend.dto.UpdatePasswordRequest;
import com.kortex.backend.dto.UpdateProfileRequest;
import com.kortex.backend.dto.UserProfileResponse;
import com.kortex.backend.exception.InvalidCredentialsException;
import com.kortex.backend.exception.ResourceNotFoundException;
import com.kortex.backend.model.User;
import com.kortex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for user profile operations.
 * Handles profile retrieval and updates.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user profile by ID.
     *
     * @param userId the user ID
     * @return user profile response
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return mapToProfileResponse(user);
    }

    /**
     * Update user profile name.
     *
     * @param userId the user ID
     * @param request update profile request
     * @return updated user profile
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setName(request.getName());
        User updatedUser = userRepository.save(user);

        return mapToProfileResponse(updatedUser);
    }

    /**
     * Update user password.
     *
     * @param userId the user ID
     * @param request update password request
     * @throws ResourceNotFoundException if user not found
     * @throws InvalidCredentialsException if current password is incorrect
     */
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Map User entity to UserProfileResponse DTO.
     */
    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .active(user.getActive())
                .build();
    }
}
