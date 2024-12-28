package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class represents the response structure for an individual user operation result as returned by the Webex API.
// It contains information about the status, method, and bulk ID of the operation, along with any error details if applicable.
// This class is used to capture the result of a specific user-related operation, such as creating or updating users.

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UserOperationResponse {
    private String status;
    private String method;
    private String bulkId;
    private ErrorResponseDetails response;  // This field will only be set if there is an error

    public static class ErrorResponseDetails {
        private List<String> schemas;
        private String status;

        @JsonProperty("urn:scim:schemas:extension:cisco:webexidentity:api:messages:2.0:Error")
        private ErrorDetails errorDetails;

        public static class ErrorDetails {
            private String trackingId;
            private String errorCode;
            private String details;
        }
    }
}
