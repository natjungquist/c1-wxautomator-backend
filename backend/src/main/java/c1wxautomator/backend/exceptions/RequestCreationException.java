package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist
//
// Exception thrown when there is an error assembling data for an API request.
// Should cause an INTERNAL_SERVER_ERROR (500) because it means that some of the data provided to create
// the Request object was null or empty. This data cannot be null/empty because it is required for the request.
// This data should have been verified as not null/empty before being passed to methods that depend on it.
//
// Usage:
// Services responsible for preparing request data.

public class RequestCreationException extends Exception {
    public RequestCreationException(String message) {
        super(message);
    }
}
