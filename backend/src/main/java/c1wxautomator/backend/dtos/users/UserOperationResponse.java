package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *  This class represents the response structure for an individual user operation result as returned by the Webex API.
 *  It contains information about the status, method, and bulk ID of the operation, along with any error details if applicable.
 *  This class is used to capture the result of a specific user-related operation, such as creating or updating users.
 * //
 *  Usage:
 *  Services that make calls to the Webex API endpoint to bulk create users will receive
 *  a response represented by this data structure.
 */
@Setter
@Getter
@NoArgsConstructor
public class UserOperationResponse {
    private String status;
    private String method;
    private String bulkId;
    private ErrorResponseDetails response;  // This field will only be set if there is an error

    /**
     * Method to retrieve the error message from a response.
     *
     * @return the error message returned by webex or the empty string if one is not available.
     */
    public String getWebexErrorMessage() {
        if (this.response == null || this.response.getErrorDetails() == null || this.response.getErrorDetails().getDetails() == null) {
            return "";
        }
        return this.response.getErrorDetails().getDetails();
    }

    /**
     * Nested class to represent the error response of a user operation response if there are any.
     */
    @Setter
    @Getter
    public static class ErrorResponseDetails {
        private List<String> schemas;
        private String status;

        @JsonProperty("urn:scim:schemas:extension:cisco:webexidentity:api:messages:2.0:Error")
        private ErrorDetails errorDetails;

        /**
         * Nested class to represent the error details of the error response.
         */
        @Setter
        @Getter
        public static class ErrorDetails {
            private String trackingId;
            private String errorCode;
            private String details;
        }
    }
}
