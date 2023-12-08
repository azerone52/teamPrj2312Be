package com.example.prj3be.controller;

import com.example.prj3be.dto.LoginDto;
import com.example.prj3be.dto.MemberInfoDto;
import com.example.prj3be.dto.TokenDto;
import com.example.prj3be.jwt.JwtAuthenticationEntryPoint;
import com.example.prj3be.jwt.JwtFilter;
import com.example.prj3be.jwt.LoginProvider;
import com.example.prj3be.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import com.example.prj3be.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;

@RestController
@RequiredArgsConstructor
public class LoginController {
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final LoginService loginService;
    private final LoginProvider loginProvider;

    @Value("${button.image.url}")
    private String socialButtonImagePrefix;

    @GetMapping("/refreshToken")
    public TokenDto byRefreshToken(@RequestHeader("Authorization")String refreshToken){
        System.out.println("LoginController.byRefreshToken's refreshToken = " + refreshToken);
        if(StringUtils.hasText(refreshToken) && refreshToken.startsWith("Bearer ")){
            refreshToken = refreshToken.substring(7);
        }

        Authentication authentication = tokenProvider.updateTokensByRefreshToken(refreshToken);

        System.out.println("LoginController.byRefreshToken's authentication = " + authentication);

        TokenDto tokens = tokenProvider.createTokens(authentication);

        return tokens;
    }
    @PostMapping("/login")
    public ResponseEntity<TokenDto> authorize(@Valid @RequestBody LoginDto loginDto){
        System.out.println("loginDto.getLogId() = " + loginDto.getLogId());
        System.out.println("loginDto.getPassword() = " + loginDto.getPassword());

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getLogId(), loginDto.getPassword());

        System.out.println("LoginController.authorize");
        System.out.println("authenticationToken = " + authenticationToken);

        try {
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("authentication = " + authentication);

//            String jwt = tokenProvider.createToken(authentication);
            TokenDto tokens = tokenProvider.createTokens(authentication);

            System.out.println("tokens = " + tokens);

//            HttpHeaders httpHeaders = new HttpHeaders();
//            // 헤더에 토큰 담기
//            httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + tokens.getAccessToken());
//
//            System.out.println("httpHeaders = " + httpHeaders);

            return new ResponseEntity<>(new TokenDto(tokens.getAccessToken(), tokens.getRefreshToken()), HttpStatus.OK);
        } catch (AuthenticationException e){
            System.out.println("인증 실패 :"+e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/login")
    public void login(HttpServletRequest servletRequest){
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("name = " + name);

        // Http 헤더에서 토큰 추출
        String token = servletRequest.getHeader("Authorization");
        if(token != null && token.startsWith("Bearer")){
            token = token.substring(7);
        }

        Authentication authentication = tokenProvider.getAuthentication(token);
        System.out.println("authentication = " + authentication);
    }

    @GetMapping("/api/login/image")
    public ResponseEntity<String> socialButtonImage() {
        return ResponseEntity.ok(socialButtonImagePrefix);
    }

    @ResponseBody
    @GetMapping("/api/login/kakao")
    public ResponseEntity<HashMap<String, Object>> kakaoLogin(@RequestParam(required = false) String code) {
        HashMap<String, Object> postLoginRes = new HashMap<>();

        try{
            //URL에 포함된 code를 이용하여 액세스 토큰 발급
            String accessToken = loginService.getKakaoAccessToken(code);
            System.out.println("accessToken = " + accessToken);

            //액세스 토큰을 이용하여 카카오 서버에서 유저 정보(닉네임, 이메일) 받아오기
            HashMap<String, Object> userInfo = loginService.getUserInfo(accessToken);
            System.out.println("userInfo = " + userInfo);

//            PostLoginRes postLoginRes = null;

            //만약 DB에 해당 이메일을 가진 유저가 없다면 회원가입 시키고, 유저 식별자와 JWT 반환
            //전화번호, 성별, 및 기타 개인 정보는 사업자 번호가 없기 때문에 받아올 권한이 없어 테스트 불가능
            if(loginService.checkEmail(String.valueOf(userInfo.get("email")))) {
                return ResponseEntity.ok(null);
                //TODO: "회원가입시키고, 유저 식별자와 JWT 반환" 완성하기
                //TODO : 회원가입 후 로그인 창으로 가기 때문에 냅둬도 ㄱㅊ?
            } else {
                //해당 이메일을 가진 유저가 있다면 기존 유저의 로그인으로 판단하고 유저 식별자와 JWT 반환
                //TODO: 유저 식별자와 JWT 반환
                postLoginRes = loginService.getUserInfo(String.valueOf(userInfo.get("email")));
                return ResponseEntity.ok(postLoginRes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @ResponseBody
    @GetMapping("/api/login/kakao/{userId}")
    public ResponseEntity<String> updateKakaoToken(@PathVariable int userId) {
        String result = "";

        try {
            // JWT에서 id 추출

            int userIndexByJwt = Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
            // userIndex와 접근한 유저가 같은지 확인
            if(userId != userIndexByJwt) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid User JWT");
            }
            loginService.updateKakaoToken(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}