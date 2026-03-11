package apptive.fin.auth;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        String accessToken,
        String refreshToken
) {
}
