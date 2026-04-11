package io.droidevs.mclub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class AttendanceWindowRequest {

    @Min(0)
    @Max(24 * 60)
    private int opensBeforeStartMinutes;

    @Min(1)
    @Max(24 * 60)
    private int closesAfterStartMinutes;

    public int getOpensBeforeStartMinutes() {
        return opensBeforeStartMinutes;
    }

    public void setOpensBeforeStartMinutes(int opensBeforeStartMinutes) {
        this.opensBeforeStartMinutes = opensBeforeStartMinutes;
    }

    public int getClosesAfterStartMinutes() {
        return closesAfterStartMinutes;
    }

    public void setClosesAfterStartMinutes(int closesAfterStartMinutes) {
        this.closesAfterStartMinutes = closesAfterStartMinutes;
    }
}

