package io.droidevs.mclub.dto;

import java.util.UUID;

public class AttendanceEventQrDto {
    private UUID eventId;
    private String token;

    public AttendanceEventQrDto() {
    }

    public AttendanceEventQrDto(UUID eventId, String token) {
        this.eventId = eventId;
        this.token = token;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

