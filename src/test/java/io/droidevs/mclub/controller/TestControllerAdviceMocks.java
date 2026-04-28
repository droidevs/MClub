package io.droidevs.mclub.controller;

import io.droidevs.mclub.repository.UserRepository;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

/**
 * Common MVC-slice test config.
 *
 * <p>{@link CurrentUserModelAdvice} is part of the MVC layer and gets picked up by {@code @WebMvcTest}
 * via component scanning. It depends on {@link UserRepository}, which isn't part of a Web MVC slice.
 *
 * <p>We provide a {@code @MockBean} here so all controller tests can import it.
 */
@Configuration
public class TestControllerAdviceMocks {

    @MockBean
    UserRepository userRepository;
}

