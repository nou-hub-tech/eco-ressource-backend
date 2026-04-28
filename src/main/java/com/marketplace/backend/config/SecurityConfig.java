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
          CorsConfigurationSource corsConfigurationSource) throws Exception {
    http.cors(c -> c.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(
                    auth ->
                            auth
                                    // ✅ Authentification - public
                                    .requestMatchers("/api/auth/**").permitAll()

                                    // ✅ Livraisons - public (AJOUTÉ)
                                    .requestMatchers("/api/delivery-orders/**").permitAll()
                                    .requestMatchers("/api/shipments/**").permitAll()
                                    .requestMatchers("/api/dashboard/**").permitAll()

                                    // ✅ Transporteurs - public (AJOUTÉ - RÉSOLUTON ERREUR 403)
                                    .requestMatchers("/api/transporters").permitAll()
                                    .requestMatchers("/api/transporters/**").permitAll()

                                    // ✅ Notifications - public (AJOUTÉ - RÉSOLUTION ERREUR 403)
                                    .requestMatchers("/api/notifications/**").permitAll()

                                    // ✅ WebSocket - public (AJOUTÉ)
                                    .requestMatchers("/ws/**", "/ws/info/**").permitAll()

                                    // ✅ Listings - avec rôles
                                    .requestMatchers(HttpMethod.POST, "/api/listings/create")
                                    .hasAnyRole("ENTERPRISE", "ADMIN")
                                    .requestMatchers("/api/listings/**").authenticated()

                                    // ✅ Offres transport - avec rôles
                                    .requestMatchers(HttpMethod.POST, "/api/transport-offers")
                                    .hasAnyRole("TRANSPORTER", "ADMIN")

                                    // ✅ Admin - restreint
                                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                    .requestMatchers("/api/users/**").hasRole("ADMIN")
                                    .requestMatchers("/api/platform-events/**").hasRole("ADMIN")
                                    .requestMatchers("/api/solidarity-associations/**").hasRole("ADMIN")
                                    .requestMatchers("/api/enterprises/**").hasRole("ADMIN")

                                    // ✅ Le reste nécessite authentification
                                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}