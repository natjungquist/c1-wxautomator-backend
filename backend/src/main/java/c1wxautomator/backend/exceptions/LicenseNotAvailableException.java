package c1wxautomator.backend.exceptions;

// Author: Natalie Jungquist

/**
 *  Exception thrown when a license assignment is not possible because it is not available at the organization.
 *  *
 *  Usage:
 *  Services that process licenses to be assigned to users.
 */
public class LicenseNotAvailableException extends Exception {
     /**
     * Constructor with a message.
     *
     * @param message detail about the exception.
     */
    public LicenseNotAvailableException(String message) {
        super(message);
    }
}
