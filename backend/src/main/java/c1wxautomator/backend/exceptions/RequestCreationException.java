package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist
//
// Exception thrown when there is an error assembling data for an API request.
//
// Usage:
// Services responsible for preparing request data.

public class RequestCreationException extends Exception {
    public RequestCreationException(String message) {
        super(message);
    }
}
