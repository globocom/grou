package com.globocom.grou.security;

import com.globocom.grou.SystemEnv;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

import static com.globocom.grou.SystemEnv.KEYSTONE_DOMAIN_CONTEXT;
import static com.globocom.grou.SystemEnv.KEYSTONE_URL;

public class KeystoneAuthenticationToken extends AbstractAuthenticationToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoneAuthenticationToken.class);
    public static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ADMIN");

    private final String token;
    private final String project;

    private Object principal = null;

    public KeystoneAuthenticationToken(String token, String project) {
        super(Collections.emptyList());
        this.token = token;
        this.project = project;
        LOGGER.info("Openstack Keystone url: {}  (domain scope: {})", KEYSTONE_URL.getValue(), KEYSTONE_DOMAIN_CONTEXT.getValue());
    }

    @Override
    public Object getCredentials() {
        return project.equals(SystemEnv.PROJECT_ADMIN.getValue()) ? Collections.singleton(ADMIN) : AuthorityUtils.NO_AUTHORITIES;
    }

    @Override
    public Object getPrincipal() {
        if (principal == null) {
            try {
                OSClient.OSClientV3 os = OSFactory.builderV3()
                        .endpoint(KEYSTONE_URL.getValue())
                        .token(token)
                        .scopeToProject(Identifier.byName(project), Identifier.byName(KEYSTONE_DOMAIN_CONTEXT.getValue()))
                        .authenticate();

                this.principal = os.getToken().getUser();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        return principal;
    }

    public boolean isAdmin() {
        return ((Collection<?>)getCredentials()).contains(KeystoneAuthenticationToken.ADMIN);
    }
}
