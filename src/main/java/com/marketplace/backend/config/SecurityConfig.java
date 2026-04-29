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
import org.springframework.web.client.RestTemplate;

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

                .authorizeHttpRequests(auth -> auth

                        // Public routes
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers("/stockitem/**").permitAll()
                        .requestMatchers("/stock-movement/**").permitAll()
                        .requestMatchers("/product/**").permitAll()
                        .requestMatchers("/ai/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Stripe webhook + clé publique + polling status — sans JWT
                        .requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stripe/public-key").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stripe/polling-status").permitAll()

                        // Protected POST routes
                        .requestMatchers(HttpMethod.POST, "/api/listings/create")
                        .hasAnyAuthority("ENTERPRISE", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/transport-offers")
                        .hasAnyAuthority("TRANSPORTER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/transport/offer")
                        .hasAnyAuthority("TRANSPORTER", "ADMIN")

                        // Admin routes
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/users/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/platform-events/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/solidarity-associations/**").hasAuthority("ADMIN")
                        // Transporters: GET open to any authenticated user, writes = ADMIN only
                        .requestMatchers(HttpMethod.GET, "/api/transporters", "/api/transporters/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/transporters/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/transporters/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/transporters/**").hasAuthority("ADMIN")

                        .requestMatchers("/api/notifications/**").permitAll()



                        // Authenticated routes
                        .requestMatchers("/api/listings/**").authenticated()
                        .requestMatchers("/api/transport/**").authenticated()
                        .requestMatchers("/api/enterprise/**").authenticated()


                        .requestMatchers("/api/delivery-orders/**").permitAll()
                        .requestMatchers("/api/shipments/**").permitAll()
                        .requestMatchers("/api/dashboard/**").permitAll()
                        .requestMatchers("/api/notifications/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/listings/create")
                        .hasAnyRole("ENTERPRISE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transport-offers")
                        .hasAnyRole("TRANSPORTER", "ADMIN")

                        .requestMatchers("/api/platform-events/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/solidarity-associations/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/enterprises/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/listings/**")
                        .authenticated()


                        // All others
                        .anyRequest().authenticated())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Bean RestTemplate utilisé par KonnectService pour les appels HTTP sortants */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}