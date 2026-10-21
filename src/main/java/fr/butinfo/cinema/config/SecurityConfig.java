package fr.butinfo.cinema.config;

import fr.butinfo.cinema.security.JsonAccessDeniedHandler;
import fr.butinfo.cinema.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails client = User.withUsername("client")
                .password("$2b$10$/Scl.UaQd74spCoWwbPj1eh8ldTNANk.3QKdOaATry/V/qGQW5koC")
                .roles("CLIENT")
                .build();
        UserDetails programmateur = User.withUsername("programmateur")
                .password("$2b$10$4rLgRwxzL8SG/8Isq9dJQO4tpkLpTqYem/So7u8xvAZyNSjfzzp5K")
                .roles("PROGRAMMATEUR")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password("$2b$10$4emGczn/kVdry0gqdiLVe.q9H6uhm2ieU8Q8CUfaga721fhIalxiO")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(client, programmateur, admin);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                      JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.securityMatcher(
                        "/api/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/films/**", "/api/seances/**", "/api/salles/**")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/programmation/**")
                        .hasAnyRole("PROGRAMMATEUR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations")
                        .hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**")
                        .hasAnyRole("CLIENT", "ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/films", "/films/**", "/seances", "/seances/**",
                                "/css/**", "/js/**", "/images/**", "/affiches/**", "/favicon.ico",
                                "/login", "/error", "/acces-refuse")
                        .permitAll()
                        .requestMatchers("/admin/salles", "/admin/salles/**",
                                "/admin/reservations", "/admin/reservations/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/admin", "/admin/**")
                        .hasAnyRole("ADMIN", "PROGRAMMATEUR")
                        .requestMatchers("/reservation", "/reservation/**")
                        .hasAnyRole("CLIENT", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception.accessDeniedPage("/acces-refuse"));
        return http.build();
    }
}
