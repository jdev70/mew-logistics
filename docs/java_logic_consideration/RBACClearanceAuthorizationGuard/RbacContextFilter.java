package com.defense.mew.security;

import com.defense.mew.entity.PersonnelRbac;
import com.defense.mew.repository.PersonnelRbacRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RbacContextFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTR = "AUTHENTICATED_MEW_USER";
    private final PersonnelRbacRepository personnelRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Read EDIPI from header (or extract from CAC/JWT principal in production)
        String edipi = request.getHeader("X-EDIPI");

        if (edipi == null || edipi.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing required X-EDIPI authentication header.");
            return;
        }

        Optional<PersonnelRbac> userOpt = personnelRepository.findById(edipi.trim());
        if (userOpt.isEmpty()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(),
                    "Unrecognized EDIPI: User not found in RBAC directory.");
            return;
        }

        PersonnelRbac user = userOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            response.sendError(
                    HttpStatus.FORBIDDEN.value(),
                    "Account is not ACTIVE (Current status: " + user.getAccountStatus() + ").");
            return;
        }

        request.setAttribute(CURRENT_USER_ATTR, user);
        filterChain.doFilter(request, response);
    }
}
