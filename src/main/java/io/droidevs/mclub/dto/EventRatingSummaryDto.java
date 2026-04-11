package io.droidevs.mclub.dto;

import java.util.UUID;

public class EventRatingSummaryDto {
    private UUID eventId;
    private double average;
    private long count;

    public EventRatingSummaryDto() {
    }

    public EventRatingSummaryDto(UUID eventId, double average, long count) {
        this.eventId = eventId;
        this.average = average;
        this.count = count;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

