package com.globocom.grou.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class KeystoneAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        KeystoneAuthenticationToken auth = (KeystoneAuthenticationToken) authentication;

        if (auth.getPrincipal() == null) {
            throw new SecurityException("Auth problem. Check token or project scope");
        }
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return KeystoneAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
