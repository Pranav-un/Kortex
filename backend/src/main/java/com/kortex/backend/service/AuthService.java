package com.kortex.backend.service;

import com.kortex.backend.dto.AuthResponse;
import com.kortex.backend.dto.LoginRequest;
import com.kortex.backend.dto.RegisterRequest;
import com.kortex.backend.exception.EmailAlreadyExistsException;
import com.kortex.backend.exception.InvalidCredentialsException;
import com.kortex.backend.model.Role;
import com.kortex.backend.model.User;
import com.kortex.backend.repository.UserRepository;
import com.kortex.backend.security.JwtUtil;
import com.kortex.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for authentication operations.
 * Handles user registration and login with JWT token generation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user.
     *
     * @param request registration details
     * @return authentication response with JWT token
     * @throws EmailAlreadyExistsException if email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already registered: " + request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // Generate JWT token
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * Authenticate user and generate JWT token.
     *
     * @param request login credentials
     * @return authentication response with JWT token
     * @throws InvalidCredentialsException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Check if account is active
            if (!userDetails.isActive()) {
                throw new InvalidCredentialsException("Account is inactive");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails);

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .id(userDetails.getId())
                    .email(userDetails.getEmail())
                    .name(userDetails.getName())
                    .role(Role.valueOf(userDetails.getAuthorities().iterator().next()
                            .getAuthority().replace("ROLE_", "")))
                    .build();

        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
}
