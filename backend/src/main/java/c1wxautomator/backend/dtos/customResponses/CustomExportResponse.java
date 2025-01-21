package c1wxautomator.backend.dtos.customResponses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *  This is a parent class representing a response that the application sends to the client.
 *  Key features include:
 *       - status code to be sent to the client.
 *       - message if further details about the response need to be relayed.
 *       - integers to track the number of create attempts & number of successes.
 *  *
 *  Usage:
 *  This class is intended to be used in services and controllers that export users to Webex.
 *  Controllers will send this as the response body to the client.
 */
@Setter
@Getter
@NoArgsConstructor
public class CustomExportResponse {
    private Integer status;
    private Integer totalCreateAttempts = 0;
    private Integer numSuccessfullyCreated = 0;
    private String message = "";

    protected void incrementNumSuccessfullyCreated() {
        this.numSuccessfullyCreated++;
    }
    protected void incrementTotalCreateAttempts() {
        this.totalCreateAttempts++;
    }

    /**
     * Checks if the response is ready to be sent back to the client.
     *
     * @return true if its message and status are not null.
     */
    public boolean isReadyToSend() {
        return this.getStatus() != null && this.getMessage() != null;
    }

    /**
     * Sets error status and message for the response.
     *
     * @param status http status code representing the error
     * @param message error message to be included in the response
     */
    public void setError(Integer status, String message) {
        this.setStatus(status);
        this.setMessage(message);
    }
}
