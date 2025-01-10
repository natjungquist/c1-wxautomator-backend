package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist

/**
 *  Exception thrown when there is an error processing the CSV file.
 *  Throwing this exception should halt any further processing of client requests.
 *  *
 *  Usage:
 *  Services that process CSV files.
 */
public class CsvProcessingException extends Exception {
    /**
     * Constructor with a message.
     *
     * @param message detail about the exception.
     */
    public CsvProcessingException(String message) {
        super(message);
    }
}
