package io.tayviscon.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** Spring Security: вход через GitHub OAuth, cookie CSRF и CSP для SPA. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String CSP =
      "default-src 'self'; "
          + "img-src 'self' https://avatars.githubusercontent.com https://i.ytimg.com data:; "
          + "style-src 'self' 'unsafe-inline'; "
          + "script-src 'self'; "
          + "connect-src 'self'; "
          + "frame-src 'self' https://www.youtube.com; "
          + "frame-ancestors 'none'; "
          + "object-src 'none'; "
          + "base-uri 'self'";

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, GitHubOAuthAccountService gitHubOAuthAccountService) throws Exception {
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/courses/**", "/api/youtube/**", "/api/me", "/api/blog/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler))
        .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(CSP)))
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    apiAuthenticationEntryPoint(), new AntPathRequestMatcher("/api/**")))
        .oauth2Login(
            oauth ->
                oauth
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.userService(gitHubOAuthAccountService))
                    .failureUrl("/login?error=github"))
        .logout(logout -> logout.logoutSuccessUrl("/"));
    http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
    return http.build();
  }

  private static AuthenticationEntryPoint apiAuthenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"unauthorized\"}");
    };
  }

  static final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
      if (token != null) {
        token.getToken();
      }
      filterChain.doFilter(request, response);
    }
  }
}
