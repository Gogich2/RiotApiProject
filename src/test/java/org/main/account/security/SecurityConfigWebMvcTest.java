package org.main.account.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.account.service.SessionService;
import org.main.account.web.CsrfController;
import org.main.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigWebMvcTest.ProbeController.class)
@Import({
        SecurityConfig.class,
        AppSessionAuthenticationFilter.class,
        CsrfController.class,
        SecurityConfigWebMvcTest.ProbeController.class
})
class SecurityConfigWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionService sessionService;

    @Test
    void keepsPublicApiReadsAnonymous() throws Exception {
        mockMvc.perform(get("/api/public-probe")).
                andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationForPrivateAccountRoutes() throws Exception {
        mockMvc.perform(post("/api/account/probe").with(csrf())).
                andExpect(status().isUnauthorized());
    }

    @Test
    void requiresCsrfForAuthenticatedAccountMutation() throws Exception {
        mockMvc.perform(post("/api/account/probe").with(authentication(appAuthentication()))).
                andExpect(status().isForbidden());
    }

    @Test
    void acceptsAuthenticatedAccountMutationWithCsrf() throws Exception {
        mockMvc.perform(post("/api/account/probe").
                        with(authentication(appAuthentication())).
                        with(csrf())).
                andExpect(status().isNoContent());
    }

    @Test
    void exposesAnonymousCsrfBootstrap() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")).
                andExpect(status().isOk()).
                andExpect(jsonPath("$.token").isNotEmpty()).
                andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    private UsernamePasswordAuthenticationToken appAuthentication() {
        AppPrincipal principal = new AppPrincipal(
                UUID.fromString("47c1e486-e990-4db5-a073-bf22049f0f1a"),
                "player@example.com",
                "Player"
        );
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/public-probe")
        ResponseEntity<Void> publicProbe() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/account/probe")
        ResponseEntity<Void> accountProbe() {
            return ResponseEntity.noContent().build();
        }
    }
}
