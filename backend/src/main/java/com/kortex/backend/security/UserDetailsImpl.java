package com.kortex.backend.security;

import com.kortex.backend.model.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation for Spring Security.
 * Wraps the User entity for authentication and authorization.
 */
@Data
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private String name;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean active;

    public UserDetailsImpl(Long id, String email, String password, String name, Collection<? extends GrantedAuthority> authorities, boolean active) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.authorities = authorities;
        this.active = active;
    }

    /**
     * Build UserDetails from User entity.
     */
    public static UserDetailsImpl build(User user) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                Collections.singletonList(authority),
                user.getActive()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
