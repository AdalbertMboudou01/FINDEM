package com.memoire.assistant.security;

import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.model.User;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecruiterRepository recruiterRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TenantContext.clear();
        
        String path = request.getRequestURI();
        
        // Ignorer les endpoints publics - pas de validation JWT nécessaire
        if (path.startsWith("/api/auth/") || path.startsWith("/api/github/") || 
            path.startsWith("/api/notifications/") || path.startsWith("/assistant/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Pour les autres endpoints, appliquer la logique JWT
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(token);
            } catch (Exception ignored) {}
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                String role = jwtUtil.getRoleFromToken(token);
                UUID userId = jwtUtil.getUserIdFromToken(token);
                UUID recruiterId = jwtUtil.getRecruiterIdFromToken(token);
                UUID companyId = jwtUtil.getCompanyIdFromToken(token);

                if (userId == null || ("RECRUITER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role))) {
                    Optional<User> userOpt = userRepository.findByEmail(username);
                    if (userOpt.isPresent()) {
                        userId = userOpt.get().getId();
                        Optional<Recruiter> recruiterOpt = recruiterRepository.findByAuthUserId(userId);
                        if (recruiterOpt.isPresent()) {
                            Recruiter recruiter = recruiterOpt.get();
                            recruiterId = recruiter.getRecruiterId();
                            if (recruiter.getCompany() != null) {
                                companyId = recruiter.getCompany().getCompanyId();
                            }
                            if (role == null || role.isBlank()) {
                                role = recruiter.getRole();
                            }
                        }
                    }
                }

                TenantContext.set(userId, recruiterId, companyId, role);
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
