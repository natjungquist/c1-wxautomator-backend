package c1wxautomator.backend.config;

// Author: Natalie Jungquist
//
// This configuration class defines security settings for the application using Spring Security.
// Key features include:
// - Disabling CSRF protection as OAuth2 login may rely on browser redirects that can be interrupted by CSRF protection.
// - Configuring CORS to allow requests only from the specified frontend URL, which is set in the `frontend.url` property.
// - Defining security rules for different URLs, ensuring public access to `/check-auth` while requiring authentication for others.
// - Setting up OAuth2 login and specifying the `LoginSuccessHandler` for redirection upon successful login.
// - Configuring logout behavior and clearing session data and cookies.

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private AuthenticationSuccessHandler LoginSuccessHandler;

    @Autowired
    private LogoutSuccessHandler LogoutSuccessHandler;

    /**
     * Configures various aspects of the security filter chain for an application,
     * including CSRF protection, CORS configuration, request authorization, OAuth2 login, and
     * logout behavior.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain object
     * @throws Exception if an error occurs while configuring the HttpSecurity object
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable cross-site request forgery because OAuth2 login flows can be interrupted by CSRF protection since the login process relies on browser redirects
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/check-auth").permitAll()  // Allow public access to check-auth
                                .anyRequest().authenticated() // All requests require authentication
                )
                .oauth2Login(oauth2 ->
                        oauth2
                                .successHandler(LoginSuccessHandler)  // Register a custom success handler that will make sure a successful login redirects to the frontend
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // Backend logout endpoint
                        .addLogoutHandler(LogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID") // Optional: Clear cookies
                );
        return http.build();
    }

    /**
     * Sets up the CORS configuration to allow requests only from the specified frontend URL.
     * It permits all headers, supports various HTTP methods, and allows credentials to be included in requests.
     *
     * @return the configured CorsConfigurationSource object
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        // Configures this server so it can only receive requests coming from the frontendUrl.
        // This only affects cross-origin requests made via JavaScript.
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

