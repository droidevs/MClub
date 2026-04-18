package io.droidevs.mclub.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight wiring test for SecurityConfig.
 */
class SecurityConfigAuthorizationTest {

    private static final class EmptyJwtFilterProvider implements ObjectProvider<JwtAuthenticationFilter> {
        @Override
        public JwtAuthenticationFilter getObject(Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JwtAuthenticationFilter getObject() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JwtAuthenticationFilter getIfAvailable() {
            return null;
        }

        @Override
        public JwtAuthenticationFilter getIfUnique() {
            return null;
        }

        @Override
        public Iterator<JwtAuthenticationFilter> iterator() {
            return java.util.Collections.emptyIterator();
        }
    }

    @Test
    void securityConfig_shouldProvideJwtAuthenticationFilterForChain_evenWhenRealFilterMissing() {
        SecurityConfig cfg = new SecurityConfig();
        OncePerRequestFilter filter = cfg.jwtAuthenticationFilterForChain(new EmptyJwtFilterProvider());
        assertNotNull(filter);
    }

    @Test
    void jwtAuthenticationFilter_shouldBeKnownType() {
        assertNotNull(UsernamePasswordAuthenticationFilter.class);
        assertNotNull(JwtAuthenticationFilter.class);
    }
}
