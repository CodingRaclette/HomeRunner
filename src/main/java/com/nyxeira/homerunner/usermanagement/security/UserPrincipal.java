package com.nyxeira.homerunner.usermanagement.security;
import com.nyxeira.homerunner.usermanagement.model.User;

import org.jspecify.annotations.NullMarked;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


/**
 * Adaptateur de la User métier à la classe UserDetails de SpringSecurity
 */
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) { this.user = user; }

    public User getUser() { return user; }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    @Override
    @NullMarked
    public String getUsername() { return user.getLogin(); }

}
