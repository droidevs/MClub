package io.droidevs.mclub.exception;

public class BusinessRuleViolationException extends PlatformException{

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException() {
        super("Business rule violation");
    }
}
