package io.droidevs.mclub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class EventRatingRequest {

    @Min(1)
    @Max(5)
    private int rating;

    private String comment;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

