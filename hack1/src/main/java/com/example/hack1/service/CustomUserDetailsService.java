package com.example.hack1.service;

import com.example.hack1.entity.User;
import com.example.hack1.repository.UserRepository;
import com.example.hack1.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = repo.findByUsername(usernameOrEmail)
                .or(() -> repo.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username o email: " + usernameOrEmail));
        return new CustomUserDetails(user);
    }
}
