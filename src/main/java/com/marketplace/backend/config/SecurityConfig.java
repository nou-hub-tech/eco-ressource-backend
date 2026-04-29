package com.marketplace.backend.config;

import com.marketplace.backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import jakarta.servlet.DispatcherType;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationProvider authenticationProvider,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    http.cors(c -> c.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/me")
                    .authenticated()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs")
                    .permitAll()
                    .requestMatchers("/ws/**")
                    .permitAll()
                    .requestMatchers("/files/**")
                    .permitAll()
                    .requestMatchers("/stockitem/**")
                    .permitAll()
                    .requestMatchers("/stock-movement/**")
                    .permitAll()
                    .requestMatchers("/product/**")
                    .permitAll()
                    .requestMatchers("/ai/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/listings/create")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/transport-offers")
                    .hasAnyAuthority("ROLE_TRANSPORTER", "ROLE_ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/listing-images")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN")
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/users/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/platform-events/**")
                    .permitAll()
                    .requestMatchers("/api/ai/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/solidarity-associations/**", "/api/donations/**")
                    .permitAll()
                    .requestMatchers("/api/enterprises/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/transporters/**")
                    .hasRole("ADMIN")
                    // Dashboard / annonces : mêmes rôles que le JWT (ROLE_* explicite, évite les 403 SpEL)
                    .requestMatchers("/api/listings/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    .requestMatchers("/api/listing-ai/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    .requestMatchers("/api/geocoding/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    // Lecture catalogue produits sans authentification (création d’annonces, front public)
                    .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN")
                    .requestMatchers("/api/stock-items/**")
                    .authenticated()
                    .requestMatchers("/api/stock-movements/**")
                    .authenticated()
                    // Annonces « ressource » (création + favoris sur même préfixe) : GET public, écriture authentifiée
                    .requestMatchers(HttpMethod.GET, "/api/resource-listings", "/api/resource-listings/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/resource-listings", "/api/resource-listings/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    .requestMatchers(HttpMethod.PUT, "/api/resource-listings", "/api/resource-listings/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN")
                    .requestMatchers("/api/groups/**")
                    .authenticated()
                    .requestMatchers("/api/comments/**")
                    .authenticated()
                    .requestMatchers("/api/favorites/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    .requestMatchers("/api/deliveries/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    .requestMatchers("/api/transport/**")
                    .hasAnyAuthority("ROLE_ENTERPRISE", "ROLE_ADMIN", "ROLE_TRANSPORTER")
                    .requestMatchers("/api/enterprise/**")
                    .authenticated()
                    .requestMatchers("/inventory/**")
                    .authenticated()
                    .requestMatchers("/broken-product/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

}
