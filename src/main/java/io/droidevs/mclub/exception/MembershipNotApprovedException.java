package io.droidevs.mclub.exception;

public class MembershipNotApprovedException extends InvalidMembershipStateException {

    public MembershipNotApprovedException() {
        super("Membership is not approved");
    }

    public MembershipNotApprovedException(String message) {
        super(message);
    }
}
