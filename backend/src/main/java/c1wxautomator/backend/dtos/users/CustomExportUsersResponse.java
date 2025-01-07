package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This is a custom class representing a response that the application sends to the client.
// Key features include:
//      - message if further details about the response need to be relayed.
//      - results list storing information about each user processed during the export.
//      - licenseResults lists for each user, storing information about each license that was attempted to be assigned.
//      - methods to add success and failure results to the respective lists,
//          including details such as bulk ID, username, and error details.
//
// The CreateUserResult and AssignLicenseResult classes are nested static classes that represent individual
// results of API operations, each containing relevant details for the operation.
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
    private Integer totalCreateAttempts = 0;
    private Integer numSuccessfullyCreated = 0;
    private String message = "";
    private List<CreateUserResult> results = new ArrayList<>();


    public boolean isReadyToSend() {
        return this.status != null && this.totalCreateAttempts != null && this.numSuccessfullyCreated != null
                && this.message != null;
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
    /**
     * Adds a license success result to a specific CreateUserResult in the results list.
     *
     * @param email        The email of the user whose result will be modified.
     * @param licenseName  The name of the license assigned.
     */
    public void addLicenseSuccess(String email, String licenseName) {
        CreateUserResult userResult = findUserResultByEmail(email);
        if (userResult != null) {
            String message = "Assigned.";
            userResult.getLicenseResults().add(new CreateUserResult.AssignLicenseResult(200, message, licenseName));
        }
    }

    /**
     * Adds a license failure result to a specific CreateUserResult in the results list.
     *
     * @param email        The email of the user whose result will be modified.
     * @param licenseName  The name of the license.
     * @param apiMessage   The error message from the API.
     * @param status       The HTTP status code of the failure.
     */
    public void addLicenseFailure(String email, String licenseName, String apiMessage, Integer status) {
        CreateUserResult userResult = findUserResultByEmail(email);
        if (userResult != null) {
            String message = String.format("Failed to assign license: %s", apiMessage);
            userResult.getLicenseResults().add(new CreateUserResult.AssignLicenseResult(status, message, licenseName));
        }
    }

    /**
     * Helper method to find a CreateUserResult by email.
     *
     * @param email The email of the user to find.
     * @return The CreateUserResult object if found, null otherwise.
     */
    private CreateUserResult findUserResultByEmail(String email) {
        return results.stream()
                .filter(result -> result.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }

    @Getter
    @Setter
    public static class CreateUserResult {
        private Integer status;
        private String message = "Created.";
        private String email;
        private String firstName;
        private String lastName;
        private List<AssignLicenseResult> licenseResults = new ArrayList<>();

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

        @Getter
        @Setter
        public static class AssignLicenseResult {
            private String licenseName;
            private Integer status;
            private String message;

            public AssignLicenseResult(Integer status, String message, String licenseName) {
                this.status = status;
                this.message = message;
                this.licenseName = licenseName;
            }
        }
    }
}
