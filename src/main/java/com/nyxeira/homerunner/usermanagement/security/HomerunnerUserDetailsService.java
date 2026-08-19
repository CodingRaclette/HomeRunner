package com.nyxeira.homerunner.usermanagement.security;

import com.nyxeira.homerunner.usermanagement.repositories.UserRepository;

import org.jspecify.annotations.NullMarked;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
public class HomerunnerUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public HomerunnerUserDetailsService(UserRepository userRepository) { this.userRepository = userRepository; }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String login) {
        return userRepository.findByLogin(login)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(login));
    }
}

