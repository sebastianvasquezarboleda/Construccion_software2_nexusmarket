package application;

import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.ports.in.UserInputPort;
import application.domain.ports.in.BuyerInputPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.adapters.out.memory.InMemoryBuyerRepositoryAdapter;
import application.adapters.out.memory.InMemoryOrderRepositoryAdapter;
import application.adapters.out.memory.InMemoryUserRepositoryAdapter;
import jakarta.servlet.http.Cookie;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NexusmarketApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInputPort users;

    @Autowired
    private BuyerInputPort buyers;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestRepositories {
        @Bean
        @Primary
        UserRepositoryPort userRepositoryPort() {
            return new InMemoryUserRepositoryAdapter();
        }

        @Bean
        @Primary
        BuyerRepositoryPort buyerRepositoryPort() {
            return new InMemoryBuyerRepositoryAdapter();
        }

        @Bean
        @Primary
        OrderRepositoryPort orderRepositoryPort() {
            return new InMemoryOrderRepositoryAdapter();
        }
    }

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

    @Test
    void usersCannotReadAnotherUsersAccount() throws Exception {
        long id = System.currentTimeMillis();
        String password = "OwnershipTestPassword2026!";
        User buyer = new User(id, "Buyer", "buyer-" + id + "@example.com", "BUYER-" + id,
                Role.BUYER, UserStatus.ACTIVE);
        User other = new User(id + 1, "Other", "other-" + id + "@example.com", "OTHER-" + id,
                Role.SELLER, UserStatus.ACTIVE);
        users.register(buyer, password);
        users.register(other, password);
        String authorization = basicAuth(buyer.getEmail(), password);

        mockMvc.perform(get("/api/users/{id}", other.getId()).header("Authorization", authorization))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{id}", buyer.getId()).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buyer.getId()));
    }

    @Test
    void endpointRoleRestrictionsAreEnforced() throws Exception {
        long id = System.currentTimeMillis();
        String password = "RoleMatrixTestPassword2026!";
        User buyer = registerUser(id, "buyer-role", Role.BUYER, password);
        User seller = registerUser(id + 1, "seller-role", Role.SELLER, password);
        User supervisor = registerUser(id + 2, "supervisor-role", Role.SUPERVISOR, password);
        User logistics = registerUser(id + 3, "logistics-role", Role.LOGISTICS_OPERATOR, password);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", basicAuth(buyer.getEmail(), password)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", basicAuth(seller.getEmail(), password)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", basicAuth(logistics.getEmail(), password)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", basicAuth(supervisor.getEmail(), password)))
                .andExpect(status().isOk());
    }

    @Test
    void orderCreationRequiresAnExistingBuyerProfile() throws Exception {
        long id = System.currentTimeMillis();
        String email = "order-admin-" + id + "@example.com";
        String password = "OrderTestPassword2026!";
        users.register(new User(id, "Order Administrator", email, "ORDER-ADMIN-" + id,
                Role.ADMINISTRATOR, UserStatus.ACTIVE), password);
        Buyer buyer = new Buyer(id + 1, id + 2, "Main address", List.of(), "ACTIVE");
        buyers.register(buyer);
        String authorization = basicAuth(email, password);
        CsrfDetails csrf = csrfDetails(authorization);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authorization)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + (id + 3) + ",\"buyerId\":" + buyer.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buyerId").value(buyer.getId()));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authorization)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + (id + 4) + ",\"buyerId\":" + (id + 99) + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buyerCanOnlyCreateOrdersForTheirOwnBuyerProfile() throws Exception {
        long id = System.currentTimeMillis();
        String password = "BuyerOrderTestPassword2026!";
        User buyerUser = registerUser(id, "buyer-order", Role.BUYER, password);
        Buyer ownProfile = new Buyer(id + 1, buyerUser.getId(), "Own address", List.of(), "ACTIVE");
        Buyer otherProfile = new Buyer(id + 2, id + 3, "Other address", List.of(), "ACTIVE");
        buyers.register(ownProfile);
        buyers.register(otherProfile);
        String authorization = basicAuth(buyerUser.getEmail(), password);
        CsrfDetails csrf = csrfDetails(authorization);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authorization)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + (id + 4) + ",\"buyerId\":" + ownProfile.getId() + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authorization)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + (id + 5) + ",\"buyerId\":" + otherProfile.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEndpointsReturnBoundedPagesWithMetadata() throws Exception {
        long id = System.currentTimeMillis();
        String email = "page-" + id + "@example.com";
        String password = "PageTestPassword2026!";
        users.register(new User(id, "Page Test Administrator", email, "PAGE-" + id,
                Role.ADMINISTRATOR, UserStatus.ACTIVE), password);
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                (email + ":" + password).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/users?page=0&size=1").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());

        mockMvc.perform(get("/api/users?page=0&size=101").header("Authorization", authorization))
                .andExpect(status().isBadRequest());
    }

    private CsrfDetails csrfDetails(String authorization) throws Exception {
        MvcResult bootstrap = mockMvc.perform(get("/api/csrf").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn();
        String response = bootstrap.getResponse().getContentAsString();
        Matcher tokenMatcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response);
        assertTrue(tokenMatcher.find());
        Cookie cookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return new CsrfDetails(tokenMatcher.group(1), cookie);
    }

    private static String basicAuth(String email, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (email + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private User registerUser(long id, String prefix, Role role, String password) {
        User user = new User(id, prefix + " user", prefix + "-" + id + "@example.com",
                prefix.toUpperCase() + "-" + id, role, UserStatus.ACTIVE);
        users.register(user, password);
        return user;
    }

    private record CsrfDetails(String token, Cookie cookie) { }
}
