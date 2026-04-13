package com.example.thinkmap.config;

import com.example.thinkmap.domain.repository.UserRepository;
import com.example.thinkmap.security.jwt.JwtAuthenticationFilter;
import com.example.thinkmap.security.jwt.JwtTokenProvider;
import com.example.thinkmap.security.oauth2.CustomOAuth2UserService;
import com.example.thinkmap.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.thinkmap.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.thinkmap.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 기존 CORS 설정(CorsConfig) 그대로 사용
            .cors(Customizer.withDefaults())
            // REST API이므로 CSRF 불필요
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless (JWT 사용)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 폼 로그인 / HTTP Basic 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // H2 콘솔용 frameOptions 허용
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            // 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/api/auth/**",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // OAuth2 소셜 로그인
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ep -> ep
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestRepository(cookieRepository)
                )
                .redirectionEndpoint(ep -> ep
                    .baseUri("/login/oauth2/code/*")
                )
                .userInfoEndpoint(ep -> ep
                    .userService(customOAuth2UserService)
                )
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            )
            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
