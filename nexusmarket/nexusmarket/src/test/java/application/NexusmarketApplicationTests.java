package application;

import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.models.User;
import application.domain.ports.in.UserInputPort;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NexusmarketApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInputPort users;

	@Test
	void contextLoads() {
	}

    @Test
    void unsafeRequestsRequireCsrfCookieAndMatchingHeader() throws Exception {
        long id = System.currentTimeMillis();
        String email = "csrf-" + id + "@example.com";
        String password = "CsrfTestPassword2026!";
        users.register(new User(id, "CSRF Test Administrator", email, "CSRF-" + id,
                Role.ADMINISTRATOR, UserStatus.ACTIVE), password);
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (email + ":" + password).getBytes(StandardCharsets.UTF_8));

        MvcResult bootstrap = mockMvc.perform(get("/api/csrf").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn();
        String tokenResponse = bootstrap.getResponse().getContentAsString();
        Matcher tokenMatcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(tokenResponse);
        assertTrue(tokenMatcher.find());
        String token = tokenMatcher.group(1);
        String headerName = "X-XSRF-TOKEN";
        Cookie csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        assertTrue(!token.isBlank());
        assertTrue(tokenResponse.contains("\"headerName\":\"" + headerName + "\""));
        assertNotNull(csrfCookie);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", authorization)
                        .cookie(csrfCookie)
                        .header(headerName, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", authorization)
                        .cookie(csrfCookie)
                        .header(headerName, csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsAllowsOnlyConfiguredFrontendOrigins() throws Exception {
        mockMvc.perform(options("/api/csrf")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(result -> assertTrue("http://localhost:3000".equals(
                        result.getResponse().getHeader("Access-Control-Allow-Origin"))))
                .andExpect(result -> assertTrue("true".equals(
                        result.getResponse().getHeader("Access-Control-Allow-Credentials"))));

        mockMvc.perform(options("/api/csrf")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(result -> assertTrue(result.getResponse()
                        .getHeader("Access-Control-Allow-Origin") == null));
    }

}
