package io.droidevs.mclub.exception;

import java.util.UUID;

public class MemberShipNotFoundException extends PlatformException  {
    public MemberShipNotFoundException() {
        super("Membership not found");
    }

    public MemberShipNotFoundException(UUID id) {
        super("Membership not found: " + id.toString());
    }
}
