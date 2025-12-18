package rs.examproject.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        // Public endpoints
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/api/demo/public").permitAll()
                        .pathMatchers("/api/processing/health").permitAll()
                        // Authenticated endpoints
                        .pathMatchers("/api/demo/**").authenticated()
                        .pathMatchers("/api/files/**").authenticated()
                        .pathMatchers("/api/books/**").authenticated()
                        .pathMatchers("/api/processing/**").authenticated()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
