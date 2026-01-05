package com.kortex.backend.controller;

import com.kortex.backend.dto.UpdatePasswordRequest;
import com.kortex.backend.dto.UpdateProfileRequest;
import com.kortex.backend.dto.UserProfileResponse;
import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current user profile.
     *
     * @param authentication the authenticated user
     * @return user profile response
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserProfileResponse profile = userService.getUserProfile(userDetails.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user profile (name).
     *
     * @param authentication the authenticated user
     * @param request update profile request
     * @return updated user profile
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserProfileResponse profile = userService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user password.
     *
     * @param authentication the authenticated user
     * @param request update password request
     * @return no content response
     */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateCurrentUserPassword(
            Authentication authentication,
            @Valid @RequestBody UpdatePasswordRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        userService.updatePassword(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user profile by ID (Admin only).
     *
     * @param userId the user ID
     * @return user profile response
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
