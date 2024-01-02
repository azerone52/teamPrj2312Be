package com.example.prj3be.config;


import com.example.prj3be.jwt.JwtAccessDeniedHandler;
import com.example.prj3be.jwt.JwtAuthenticationEntryPoint;
import com.example.prj3be.jwt.JwtSecurityConfig;
import com.example.prj3be.jwt.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig{
    private final TokenProvider tokenProvider;
    private CorsFilter corsFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(
            TokenProvider tokenProvider,
            CorsFilter corsFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ){
        this.tokenProvider = tokenProvider;
        this.corsFilter = corsFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // token을 사용하는 방식이므로 csrf를 disable
                // 토큰 자체에 인증 정보가 포함되어 있으므로 세션기반의 csrf 토큰이 필요하지 않을 수 있으므로
                //비활성화 하는 것이 편리함
                .csrf(csrf->csrf.disable())

                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling->exceptionHandling
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers("/member/add","/member/check/**","/member/{logId}/orders","/api/board/list","api/board/id/**","api/board/file/id/**","api/like/board/**", "/authenticate","/actuator/**", "api/comment/list").permitAll()
                                .requestMatchers("/login","/refreshToken","/cart/**", "/api/logout","/api/login/image", "/isSocialMember", "/api/auth/**","/cpu","/jvm").permitAll()
                                .requestMatchers("/accessToken","/api/order", "/payment/**").authenticated()
                                .requestMatchers("/api/board/add","/api/board/edit/**", "api/board/remove/**","member/list").hasRole("ADMIN")
                                .requestMatchers("api/like/update/**","/api/comment/delete/**", "/api/comment/update/**", "/api/comment/add/**", "/member", "/member/edit/**", "member/delete/**").hasAnyRole("ADMIN","USER")
                                .requestMatchers("/error").authenticated()


                .anyRequest().permitAll()
                )

                //세션을 사용하지 않으므로 STATELESS로 설정
                .sessionManagement(sessionMangement->
                        sessionMangement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .apply(new JwtSecurityConfig(tokenProvider));

        return http.build();
    }
}
