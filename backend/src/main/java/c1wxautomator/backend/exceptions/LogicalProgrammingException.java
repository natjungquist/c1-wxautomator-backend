package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist

/**
 *  Exception thrown when there is a logical error.
 *  *
 *  Usage:
 *  Any method that performs error checks which may result from developer mistakes.
 *  Mainly for debugging.
 */
public class LogicalProgrammingException extends Exception {
    /**
     * Constructor with a message.
     *
     * @param message detail about the exception.
     */
    public LogicalProgrammingException(String message) {
        super(message);
    }
}
