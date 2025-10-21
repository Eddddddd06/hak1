package com.example.hack1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                String branch = jwtTokenProvider.getBranchFromToken(jwt);

                // Crear authorities basado en el rol
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // Crear authentication token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Agregar información adicional (branch) en los detalles
                authentication.setDetails(new UserAuthenticationDetails(
                        username,
                        role,
                        branch,
                        new WebAuthenticationDetailsSource().buildDetails(request)
                ));

                // Establecer en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Autenticación exitosa para usuario: {} con rol: {}", username, role);
            }
        } catch (Exception ex) {
            log.error("No se pudo establecer la autenticación del usuario: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el JWT del header Authorization
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Clase para almacenar detalles adicionales del usuario autenticado
     */
    public static class UserAuthenticationDetails extends WebAuthenticationDetailsSource {
        private final String username;
        private final String role;
        private final String branch;
        private final Object webDetails;

        public UserAuthenticationDetails(String username, String role, String branch, Object webDetails) {
            this.username = username;
            this.role = role;
            this.branch = branch;
            this.webDetails = webDetails;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }

        public String getBranch() {
            return branch;
        }
    }
}