package com.kortex.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Interceptor for authenticating WebSocket connections using JWT tokens.
 * Validates JWT from the "Authorization" header during STOMP CONNECT.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Intercepts incoming WebSocket messages before they reach the handler.
     * Authenticates STOMP CONNECT commands using JWT token.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                
                try {
                    String username = jwtUtil.extractUsername(jwt);
                    
                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        if (jwtUtil.validateToken(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities()
                                );
                            
                            accessor.setUser(authentication);
                            logger.info("WebSocket authenticated for user: {}", username);
                        } else {
                            logger.warn("Invalid JWT token for WebSocket connection");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error authenticating WebSocket connection: {}", e.getMessage());
                }
            } else {
                logger.warn("WebSocket connection attempt without Authorization header");
            }
        }
        
        return message;
    }
}
