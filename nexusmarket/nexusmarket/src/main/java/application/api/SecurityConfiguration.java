package application.api;

import application.domain.models.UserCredential;
import application.domain.ports.out.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepositoryPort users) {
        return email -> users.findCredentialByEmail(email)
                .map(SecurityConfiguration::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository(
            @Value("${nexusmarket.security.csrf-cookie-secure:true}") boolean secureCookie,
            @Value("${nexusmarket.security.csrf-cookie-same-site:Lax}") String sameSite) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/api")
                .secure(secureCookie)
                .sameSite(sameSite));
        return repository;
    }

    @Bean
    CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new SpaCsrfTokenRequestHandler();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
                                            CsrfTokenRequestHandler csrfTokenRequestHandler) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/users")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/users/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "BUYER", "SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/users")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/**")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/password")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/sellers")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "LOGISTICS_OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/sellers/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/warehouses", "/api/warehouses/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "LOGISTICS_OPERATOR", "SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/sellers")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/api/warehouses")
                        .hasAnyRole("ADMINISTRATOR", "SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/buyers")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/buyers/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "BUYER")
                        .requestMatchers("/api/buyers/*/addresses")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers(HttpMethod.POST, "/api/buyers")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*")
                        .authenticated()
                        .requestMatchers("/api/products", "/api/products/*/**")
                        .hasAnyRole("ADMINISTRATOR", "SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/inventories", "/api/inventories/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "LOGISTICS_OPERATOR", "SELLER")
                        .requestMatchers("/api/inventories", "/api/inventories/*/**")
                        .hasAnyRole("ADMINISTRATOR", "LOGISTICS_OPERATOR", "SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/orders")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/orders/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "BUYER")
                        .requestMatchers(HttpMethod.POST, "/api/orders")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/products")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*/products/*")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/payment", "/api/orders/*/paid")
                        .hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/dispatch", "/api/orders/*/finalize")
                        .hasAnyRole("ADMINISTRATOR", "LOGISTICS_OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/cancel")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/invoices")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/invoices/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/shipments", "/api/shipments/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "LOGISTICS_OPERATOR")
                        .requestMatchers("/api/shipments", "/api/shipments/*/**")
                        .hasAnyRole("ADMINISTRATOR", "LOGISTICS_OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/returns")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/returns/*")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR", "BUYER")
                        .requestMatchers(HttpMethod.POST, "/api/returns")
                        .hasAnyRole("ADMINISTRATOR", "BUYER")
                        .requestMatchers("/api/returns/*/**")
                        .hasAnyRole("ADMINISTRATOR", "LOGISTICS_OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/reports/**")
                        .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${nexusmarket.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
            String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static org.springframework.security.core.userdetails.UserDetails toUserDetails(UserCredential credential) {
        return User.withUsername(credential.email())
                .password(credential.passwordHash())
                .roles(credential.role().name())
                .disabled(credential.status() != application.domain.enums.UserStatus.ACTIVE)
                .build();
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            if (!StringUtils.hasText(headerValue)) {
                return xor.resolveCsrfTokenValue(request, csrfToken);
            }
            String cookieValue = request.getCookies() == null ? null : Arrays.stream(request.getCookies())
                    .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            CsrfTokenRequestHandler requestHandler = headerValue.equals(cookieValue) ? plain : xor;
            return requestHandler.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
