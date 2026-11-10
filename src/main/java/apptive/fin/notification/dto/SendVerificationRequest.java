package apptive.fin.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationRequest(
        @NotBlank @Email String email
) {}
