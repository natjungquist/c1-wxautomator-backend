package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class holds data for user information needed to export users.
// Its fields are named to fit Webex API request specifications.
//
// Contains inner class Name that holds data for the user's name. It is an inner class to conform to the Webex API request format.
//
// Usage:
// UserService will transfer client's data into this User class before sending it as part of a request to the Webex API.

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class User {

    private String userName;  // must be in the form of an email (and the domain must be authorized in Webex Control Hub)
    private String displayName;  // maps to 'Display Name' in client's csv
    private Name name;
    private boolean active;  // the status field
    private List<String> schemas;
    private List<Email> emails;
    private String location;
    private String title; // optional
    private String department; //optional
    // NOTE that the license is not included here because the Webex APIs for create user and assign license are separate
    // TODO other fields?

    public void populateEmails() {
        // This method exists because the emails field is required for the API call but the email should be the same as the userName
        if (this.userName != null) {
            this.emails = List.of(new Email(this.userName));
        }
    }

    @Setter
    @Getter
    public static class Name {
        private String givenName;  // maps to 'First Name' in client's csv
        private String familyName;  // maps to 'Last Name' in client's csv
        private String middleName;  // optional
        private String honorificPrefix;  // optional
        private String honorificSuffix;  // optional
    }

    @Setter
    @Getter
    public static class Email {
        private String value;
        private final String type = "home";
        private final String display = "home email";
        private final boolean primary = false;

        public Email(String value) {
            this.value = value;
        }
    }
}
