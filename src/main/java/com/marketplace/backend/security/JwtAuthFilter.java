package com.marketplace.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = header.substring(7);
    if (!jwtUtils.validateToken(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Build authentication directly from JWT claims — no DB query needed
    String email = jwtUtils.getEmailFromToken(token);
    String role  = jwtUtils.getRoleFromToken(token).name(); // e.g. "ROLE_ADMIN"

    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      var authority = new SimpleGrantedAuthority(role);
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(email, null, List.of(authority));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
  }
}
