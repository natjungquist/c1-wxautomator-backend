package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist
//
// Exception thrown when there is a logical error.
//
// Usage:
// Any method that performs error checks which may result from developer mistakes.

public class LogicalProgrammingException extends Exception {
    public LogicalProgrammingException(String message) {
        super(message);
    }
}
