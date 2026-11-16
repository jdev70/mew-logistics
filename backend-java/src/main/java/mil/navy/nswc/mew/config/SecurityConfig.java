package mil.navy.nswc.mew.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** Production API requests require a validated bearer JWT; demo identity is profile-configured only. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean @ConditionalOnProperty(prefix = "mew.security", name = "mode", havingValue = "jwt", matchIfMissing = true)
    SecurityFilterChain jwt(HttpSecurity http) throws Exception {
        http.csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().anyRequest().authenticated().and().oauth2ResourceServer().jwt();
        return http.build();
    }
    @Bean @ConditionalOnProperty(prefix = "mew.security", name = "mode", havingValue = "demo")
    SecurityFilterChain demo(HttpSecurity http, @Value("${mew.security.demo-subject}") String subject,
                             @Value("${mew.security.demo-role}") String role) throws Exception {
        return http.csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .addFilterBefore(new DemoIdentityFilter(subject, role), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests().anyRequest().authenticated().and().build();
    }
    private static final class DemoIdentityFilter extends OncePerRequestFilter {
        private final String subject; private final String role;
        DemoIdentityFilter(String subject, String role) { this.subject = subject; this.role = role; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(subject, "N/A", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            try { chain.doFilter(request, response); } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        }
    }
}
