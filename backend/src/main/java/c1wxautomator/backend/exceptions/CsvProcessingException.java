package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist
//
// Exception thrown when there is an error processing the CSV file.
//
// Usage:
// Services that process CSV files.

public class CsvProcessingException extends Exception {
    public CsvProcessingException(String message) {
        super(message);
    }
}
