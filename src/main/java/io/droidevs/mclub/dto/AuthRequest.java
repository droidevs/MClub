package io.droidevs.mclub.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AuthRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}
