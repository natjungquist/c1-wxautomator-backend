package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class holds data for user information needed to export users.
// Its fields are named to fit Webex API request specifications.
//
// Contains inner classes that represent nested json structure for request. It is an inner class to conform to the Webex API request format.
// NOTE that the license is not included here because the Webex APIs for create user and assign license are separate
//
// Usage:
// UserService will transfer client's data into this User class before sending it as part of a request to the Webex API.

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class User {

    // REQUIRED FIELDS
    @JsonProperty("userName")  // This annotation is the fieldName that the Webex API expects to receive, but this program will call it email instead.
    private String email;  // userName must be in the form of an email (and the domain must be authorized in Webex Control Hub)
    private List<Email> emails;

    private String displayName;  // maps to 'Display Name' in client's csv
    private Name name;
    private final String userType = "user";
    private boolean active;  // the status field
    private List<String> schemas;
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    // OPTIONAL FIELDS - HERE IN CASE THIS SERVER WANTS TO CONFIGURE THEM IN THE FUTURE
    private String title;
    private String preferredLanguage;
    private String locale;
    private String timezone;
    private String profileUrl;
    private String externalId;
    private String nickName;
    private List<Photo> photos;
    private List<Address> addresses;
    @JsonProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
    private EnterpriseUser enterpriseUser;
    @JsonProperty("urn:scim:schemas:extension:cisco:webexidentity:2.0:User")
    private CiscoWebexIdentityUser webexIdentityUser;

    // METHODS
    public void setEmail(String email) {
        // According to the Webex API, the field userName is actually the user's primary email.
        this.email = email;
        populateEmails();
    }

    private void populateEmails() {
        // This method exists because the emails field is required for the API call but the email should be the same as the userName
        if (this.email != null) {
            this.emails = List.of(new Email(this.email));
        }
    }

    public void addPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumbers.add(phoneNumber);
    }

    // INNER CLASSES
    // REQUIRED
    @Setter
    @Getter
    public static class Name {
        private String givenName;  // maps to 'First Name' in client's csv
        private String familyName;  // maps to 'Last Name' in client's csv
        private String middleName;  // optional
        private String honorificPrefix;  // optional
        private String honorificSuffix;  // optional
    }

    // REQUIRED
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

    // REQUIRED
    @Setter
    @Getter
    public static class PhoneNumber {
        private String value;
        private final String type = "work_extension";
        private String display;
        private final boolean primary = true;
    }


    // OPTIONAL
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Photo {
        private String value;
        private String type;
        private String display;
        private boolean primary;
    }

    // OPTIONAL
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Address {
        private String type;
        private String streetAddress;
        private String locality;
        private String region;
        private String postalCode;
        private String country;
    }

    // OPTIONAL
    @Setter
    @Getter
    @NoArgsConstructor
    public static class EnterpriseUser {
        private String costCenter;
        private String organization;
        private String division;
        private String department;
        private String employeeNumber;
        private Manager manager;

        @Setter
        @Getter
        @NoArgsConstructor
        public static class Manager {
            private String value;
        }
    }

    // OPTIONAL
    @Setter
    @Getter
    @NoArgsConstructor
    public static class CiscoWebexIdentityUser {
        private String accountStatus;
        private List<SipAddress> sipAddresses;
        private List<ManagedOrg> managedOrgs;
        private List<ManagedGroup> managedGroups;
        @JsonProperty("extensionAttribute*")
        private List<String> extensionAttribute;
        @JsonProperty("externalAttribute*")
        private List<ExternalAttribute> externalAttributes;

        @Setter
        @Getter
        @NoArgsConstructor
        public static class SipAddress {
            private String value;
            private String type;
            private String display;
            private boolean primary;
        }

        @Setter
        @Getter
        @NoArgsConstructor
        public static class ManagedOrg {
            private String orgId;
            private String role;
        }

        @Setter
        @Getter
        @NoArgsConstructor
        public static class ManagedGroup {
            private String orgId;
            private String groupId;
            private String role;
        }

        @Setter
        @Getter
        @NoArgsConstructor
        public static class ExternalAttribute {
            private String source;
            private String value;
        }
    }
}
