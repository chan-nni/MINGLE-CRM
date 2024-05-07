package com.team2final.minglecrm.service.jwt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.team2final.minglecrm.controller.user.dto.SignInResponseDTO;
import com.team2final.minglecrm.controller.user.dto.TokenResponseDTO;
import com.team2final.minglecrm.domain.User;
import com.team2final.minglecrm.repository.UserRepository;
import com.team2final.minglecrm.util.redis.RedisDao;
import com.team2final.minglecrm.vo.Subject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final ObjectMapper objectMapper;
    private final RedisDao redisDao;
    private final UserRepository userRepository;

    @Value("${spring.jwt.key}")
    private String key;

    @Value("${spring.jwt.live.atk}")
    private Long atkLive;

    @Value("${spring.jwt.live.rtk}")
    private Long rtkLive;


    public TokenResponseDTO createTokensBySignIn(SignInResponseDTO signInResponseDTO) throws JsonProcessingException {
        User user = userRepository.findByEmail(signInResponseDTO.getEmail()).get();
        Subject atkSubject = Subject.atk(
                signInResponseDTO.getId(),
                signInResponseDTO.getEmail(),
                signInResponseDTO.getAuthority()
        );

        Subject rtkSubject = Subject.rtk(
                signInResponseDTO.getId(),
                signInResponseDTO.getEmail(),
                signInResponseDTO.getAuthority()
        );

        String atk = createToken(atkSubject, atkLive);
        String rtk = createToken(rtkSubject, rtkLive);

        redisDao.setValues(signInResponseDTO.getEmail(), rtk, Duration.ofMillis(rtkLive));

        return new TokenResponseDTO(atk, rtk);
    }

    // 토큰 생성 로직
    private String createToken(Subject subject, Long tokenLive) throws JsonProcessingException {
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        String subjectStr = objectMapper.writeValueAsString(subject);
        Claims claims = Jwts.claims()
                .setSubject(subjectStr);
        Date date = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(date)
                .setExpiration(new Date(date.getTime() + tokenLive))
                .signWith(secretKey)
                .compact();
    }

    // 토큰에 담긴 유저 정보(Subject)를 추출하는 함수
    public Subject getSubject(String atk) throws JsonProcessingException{
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        String subjectStr = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(atk)
                .getBody()
                .getSubject();
        return objectMapper.readValue(subjectStr, Subject.class);
    }

    /**
     * 토큰 재발급 함수
     * AccessToken 만료시 AccessToken과 RefreshToken을 재발급(갱신)함
     *
     */
    public TokenResponseDTO renewToken(String rtk) throws JsonProcessingException {
        Subject subject = getSubject(rtk);
        String rtkInRedis = redisDao.getValues(subject.getEmail());

        if (Objects.isNull(rtkInRedis) || !subject.getType().equals("RTK")) throw new BadCredentialsException("만료된 RefreshToken입니다.");

        redisDao.deleteValues(subject.getEmail()); // 갱신을 위해 RefreshToken 제거

        Subject atkSubject = Subject.atk(
                subject.getId(),
                subject.getEmail(),
                subject.getAuthority());

        Subject rtkSubject = Subject.rtk(
                subject.getId(),
                subject.getEmail(),
                subject.getAuthority());

        String newAtk = createToken(atkSubject, atkLive);
        String newRtk = createToken(rtkSubject, rtkLive);

        // RefreshToken 갱신
        redisDao.setValues(subject.getEmail(), newRtk, Duration.ofMillis(rtkLive));
        return new TokenResponseDTO(newAtk, newRtk);
    }
}