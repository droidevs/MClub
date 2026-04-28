package io.droidevs.mclub.exception;

import java.util.UUID;

public class ClubAuthorizationException extends PlatformException {
    public ClubAuthorizationException() {
        super("You do not have permission to perform this action. for the club");
    }

    public ClubAuthorizationException(UUID clubId) {
        super("You do not have permission to perform this action for club with ID: " + clubId);
    }
}
