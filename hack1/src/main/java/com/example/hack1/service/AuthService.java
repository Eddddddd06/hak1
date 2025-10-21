package com.example.hack1.service;


import com.example.hack1.dto.auth.*;
import com.example.hack1.entity.User;
import com.example.hack1.exception.ResourceConflictException;
import com.example.hack1.repository.UserRepository;
import com.example.hack1.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo usuario
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Validación adicional
        request.validate();

        // Verificar que el username no exista
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceConflictException(
                    "El username '" + request.getUsername() + "' ya está en uso"
            );
        }

        // Verificar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException(
                    "El email '" + request.getEmail() + "' ya está registrado"
            );
        }

        // Crear usuario
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.valueOf(request.getRole()))
                .branch(request.getBranch())
                .build();

        // Guardar
        User savedUser = userRepository.save(user);

        log.info("Usuario registrado exitosamente: {} con rol {}",
                savedUser.getUsername(), savedUser.getRole());

        // Convertir a DTO
        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .branch(savedUser.getBranch())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    /**
     * Autentica un usuario y genera un JWT
     */
    public LoginResponse login(LoginRequest request) {
        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Obtener el usuario
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuario no encontrado: " + request.getUsername()
                    ));

            // Generar token
            String token = jwtTokenProvider.generateToken(user);

            log.info("Login exitoso para usuario: {}", user.getUsername());

            // Retornar respuesta
            return LoginResponse.builder()
                    .token(token)
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .role(user.getRole().name())
                    .branch(user.getBranch())
                    .build();

        } catch (AuthenticationException e) {
            log.error("Fallo de autenticación para usuario: {}", request.getUsername());
            throw new RuntimeException("Credenciales inválidas");
        }
    }

/**
 * Implementación de UserDetailsService