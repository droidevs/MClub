package io.droidevs.mclub.exception;

public class InvalidMembershipStateException extends BusinessRuleViolationException {

    public InvalidMembershipStateException() {
        super("Invalid membership state");
    }

    public InvalidMembershipStateException(String message) {
        super(message);
    }

}
