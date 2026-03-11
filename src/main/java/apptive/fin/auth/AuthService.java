package apptive.fin.auth;


import apptive.fin.global.util.JwtUtil;
import apptive.fin.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;


    @Transactional
    public String getRefreshToken(User user) {
        byte[] rawRefreshToken = jwtUtil.generateRefreshToken();
        String rawRefreshTokenStr = Base64.getEncoder().encodeToString(rawRefreshToken);
        String hashedRefreshToken = jwtUtil.hashToken(rawRefreshToken);


        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hashedRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpiration()))
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawRefreshTokenStr;
    }
}
