package com.globocom.grou.security;

import org.openstack4j.model.identity.v3.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuditFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
        if (auth != null && auth.getPrincipal() instanceof User) {
            user = (User) auth.getPrincipal();
        }
        String principal = user != null ? user.getName() : "anonymous";
        LOGGER.info("[{}] {} {} (principal: {})", request.getRemoteAddr(), request.getMethod(), request.getRequestURI(), principal);
        filterChain.doFilter(request, response);
    }
}
