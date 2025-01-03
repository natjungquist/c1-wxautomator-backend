package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist
//
// Exception thrown when a location assignment is not possible because it is not available at the organization.
//
// Usage:
// Services that process locations to be assigned to users.

public class LocationNotAvailableException extends Exception {
    public LocationNotAvailableException(String message) { super(message); }
}
