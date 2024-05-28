package com.sparta.todo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtUtil {

    //토큰 생성에 필요한 데이터
    public static final String AUTHORIZATION_KEY = "auth";

    public static final String BEARER_PREFIX = "Bearer ";

    private final long TOKEN_TIME = 60 * 60 * 1000L;

    @Value("${jwt.secret.key}") // Base64 Encode 한 SecretKey
    private String secretKey;

    // 로그 설정
    public static final Logger logger = LoggerFactory.getLogger("JWT 관련 로그");

    public String createToken(String username, UserRoleEnum role) {
        Date date = new Date();

        return BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(username) // 사용자 식별자값(ID)
                        .claim(AUTHORIZATION_KEY, role) // 사용자 권한
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME)) // 만료 시간
                        .setIssuedAt(date) // 발급일
                        .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)),
                                SignatureAlgorithm.HS256) // 암호화 알고리즘
                        .compact();
    }

    // JWT Cookie 에 저장
//    public void addJwtToCookie(String token, HttpServletResponse res) {
//        try {
//            token = URLEncoder.encode(token, "utf-8").replaceAll("\\+", "%20"); // Cookie Value 에는 공백이 불가능해서 encoding 진행
//
//            Cookie cookie = new Cookie(AUTHORIZATION_HEADER, token); // Name-Value
//            cookie.setPath("/");
//
//            // Response 객체에 Cookie 추가
//            res.addCookie(cookie);
//        } catch (UnsupportedEncodingException e) {
//            logger.error(e.getMessage());
//        }
//    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
    }

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))).
                build()
                .parseClaimsJws(token)
                .getBody();
    }

    // HttpServletRequest 헤더에서 JWT 가져오기
    public String getTokenFromRequest(HttpServletRequest req) {
        log.info("getTokenFromRequest");

        final String bearerToken = req.getHeader(HttpHeaders.AUTHORIZATION);

        boolean tokenFound = StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX);
        if (tokenFound) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}