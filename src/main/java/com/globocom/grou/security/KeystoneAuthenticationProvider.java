package com.globocom.grou.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import static com.globocom.grou.security.KeystoneAuthFilter.AUTH_PROBLEM_ERRORMSG;

@Component
public class KeystoneAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        KeystoneAuthenticationToken auth = (KeystoneAuthenticationToken) authentication;

        if (auth.getPrincipal() == null) {
            throw new SecurityException(AUTH_PROBLEM_ERRORMSG);
        }
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return KeystoneAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
