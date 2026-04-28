package io.droidevs.mclub.domain;

import lombok.*;

/**
 * has just minimal info on application no info on user or anything else, just the application info
 */
public record ClubApplicationSummary(
    String name,
    String description,
    ApplicationStatus status
) {

}
