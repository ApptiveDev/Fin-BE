package apptive.fin.auth;

import apptive.fin.global.error.BusinessException;
import apptive.fin.global.util.JwtUtil;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends GenericFilterBean {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String token = resolveToken((HttpServletRequest) request);

        if (token != null && jwtUtil.validateToken(token)) {
            try {

                Long userId = jwtUtil.getUserIdFromToken(token); // provider_providerId

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new BusinessException(AuthErrorCode.UNAUTHORIZED));

                UserDetails userDetails = new AuthUserDetails(user);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            catch (Exception e) {
                log.error("인증 처리 중 알 수 없는 에러", e);
                SecurityContextHolder.clearContext();
            }

        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

}
