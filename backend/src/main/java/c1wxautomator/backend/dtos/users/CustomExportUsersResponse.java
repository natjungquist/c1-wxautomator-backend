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

import c1wxautomator.backend.dtos.licenses.License;
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
    private Integer totalCreateAttempts = 0;
    private Integer numSuccessfullyCreated = 0;
    private String message;
    private List<CreateUserResult> results = new ArrayList<>();

    public boolean is2xxSuccess() {
        return this.status != null && this.status >= 200 && this.status < 300;
    }

    public void setError(Integer status, String message) {
        this.status = status;
        this.message = message;
    }

    public void addSuccess(Integer status, String email, String firstName, String lastName) {
        results.add(new CreateUserResult(status, email, firstName, lastName));
        this.numSuccessfullyCreated++;
        this.totalCreateAttempts++;
    }

    public void addFailure(Integer status, String email, String firstName, String lastName, String message) {
        results.add(new CreateUserResult(status, email, firstName, lastName, message));
        this.totalCreateAttempts++;
    }

    @Getter
    @Setter
    public static class CreateUserResult {
        private Integer status;
        private String message;
        private String email;
        private String firstName;
        private String lastName;
        private List<License> licenses = new ArrayList<>();

        public CreateUserResult(Integer status, String email, String firstName, String lastName) {
            this.status = status;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public CreateUserResult(Integer status, String email, String firstName, String lastName, String message) {
            this.status = status;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.message = message;
        }
    }
}
