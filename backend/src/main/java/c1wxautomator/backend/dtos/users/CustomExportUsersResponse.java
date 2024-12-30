package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This is a custom class representing a response that the application sends to the client.
// Key features include:
//      - message if further details about the response need to be relayed.
//      - two lists: one for successes and one for failures, storing information about
//          each user processed during the export.
//     - methods to add success and failure results to the respective lists,
//          including details such as bulk ID, username, and error details.
//
// The SuccessResult and FailureResult classes are nested static classes that represent individual
// success and failure results, each containing relevant details for the operation.
//
// Usage:
// This class is intended to be used in services and controllers that export users to Webex.
// Controllers will send this as the response body to the client.

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class CustomExportUsersResponse {
    private Integer status;
    private String message;
    private List<SuccessResult> successes = new ArrayList<>();
    private List<FailureResult> failures = new ArrayList<>();

    public boolean is2xxSuccess() {
        return this.status != null && this.status >= 200 && this.status < 300;
    }

    public void addSuccess(String bulkId, String username) {
        successes.add(new SuccessResult(bulkId, username));
    }

    public void addFailure(String bulkId, String username, String errorDetail) {
        failures.add(new FailureResult(bulkId, username, errorDetail));
    }

    @Getter
    @Setter
    public static class SuccessResult {
        private String bulkId;
        private String username;

        public SuccessResult(String bulkId, String username) {
            this.bulkId = bulkId;
            this.username = username;
        }
    }

    @Getter
    @Setter
    public static class FailureResult {
        private String bulkId;
        private String username;
        private String errorDetail;

        public FailureResult(String bulkId, String username, String errorDetail) {
            this.bulkId = bulkId;
            this.username = username;
            this.errorDetail = errorDetail;
        }
    }
}
