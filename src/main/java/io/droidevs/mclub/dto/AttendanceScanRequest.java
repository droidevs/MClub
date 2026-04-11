package io.droidevs.mclub.dto;

import jakarta.validation.constraints.NotBlank;

public class AttendanceScanRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

