package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class holds data for user information needed to export users.
// Its fields are named to fit Webex API request specifications.
//
// Contains inner classes that represent nested json structure for request.
// Contains methods to set inner class data. This makes the code more modular, understandable,
//      and maintainable, ensuring proper data handling and integration with the Webex API.
//
// NOTE that other user metadata such as their licenses is encapsulated by the UserMetadata class.
//      The metadata is separate because it cannot be included as part of the UserRequest data sent to the Webex API.
//
// NOTE that the emails field is required, and one of the emails in it must match the email/userName field,
//      but it should be set to 'primary':false.
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
public class UserRequest {
    // REQUIRED FIELDS
    @JsonProperty("userName")  // This JsonProperty annotation is the field name that the Webex API expects to receive, but this program will call it email instead.
    private String email;  // userName must be in the form of an email (and the domain must be authorized in Webex Control Hub)
    private List<Email> emails;

    private String displayName;  // maps to 'Display Name' in client's csv
    private Name name;
    private String userType = "user";
    private boolean active;  // the status field
    private List<String> schemas;
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    // OPTIONAL FIELDS - HERE IN CASE THIS SERVER WANTS TO CONFIGURE THEM IN THE FUTURE
//    private String title;
//    private String preferredLanguage;
//    private String locale;
//    private String timezone;
//    private String profileUrl;
//    private String externalId;
//    private String nickName;
//    private List<Photo> photos;
//    private List<Address> addresses;
//    @JsonProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
//    private EnterpriseUser enterpriseUser;
//    @JsonProperty("urn:scim:schemas:extension:cisco:webexidentity:2.0:User")
//    private CiscoWebexIdentityUser webexIdentityUser;

    // METHODS
    /**
     * Method to set the user's 'email' field (which is called userName by the Webex API).
     * The email/userName field is required to create the user.
     * The email/userName domain must be registered by the customer in Control Hub.
     * The 'emails' field is also required to create the user. It must contain at least one email in it matching
     * the email/userName, and it must have 'primaru':false.
     *
     * @param email String representing the user's email.
     */
    public void setEmail(String email) {
        this.email = email;
        setFirstEmail();
    }

    /**
     * Sets the user's email/userName to their first email in the 'emails' field.
     * The 'emails' field is also required to create the user. It must contain at least one email in it matching
     * the email/userName, and it must have 'primaru':false.
     */
    private void setFirstEmail() {
        // This method exists because the emails field is required for the API call
        if (this.email != null) {
            this.emails = List.of(new Email(this.email, "work", "Work", false));
        }
    }

    /**
     * Adds primary work phone number to the user's list of phone numbers.
     *
     * @param workNumber representing the phone number to be added.
     */
    public void addPrimaryWorkNumber(String workNumber) {
        this.phoneNumbers.add( new PhoneNumber(workNumber, "work", "Work", true) );
    }

    /**
     * Adds a home phone number to the user's list of phone numbers.
     *
     * @param homeNumber representing the phone number to be added.
     */
    public void addHomeNumber(String homeNumber) {
        this.phoneNumbers.add( new PhoneNumber(homeNumber, "home", "Home", false) );
    }

    /**
     * Adds a mobile phone number to the user's list of phone numbers.
     *
     * @param mobileNumber representing the phone number to be added.
     */
    public void addMobileNumber(String mobileNumber) {
        this.phoneNumbers.add( new PhoneNumber(mobileNumber, "mobile", "Mobile", false) );
    }

    /**
     * Adds user's primary work extension number to the user's list of phone numbers.
     *
     * @param extension String representing primary work extension to be added.
     */
    public void addPrimaryExtension(String extension) {
        phoneNumbers.add( new PhoneNumber(extension, "work_extension", "Work extension", true) );
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
        private String type;
        private String display;
        private Boolean primary;

        public Email(String value, String type, String display, boolean isPrimary) {
            this.value = value;
            this.type = type;
            this.display = display;
            this.primary = isPrimary;
        }
    }

    // REQUIRED
    @Setter
    @Getter
    public static class PhoneNumber {
        private String value;
        private String type;
        private String display;
        private Boolean primary;

        public PhoneNumber(String value, String type, String display, boolean isPrimary) {
            this.value = value;
            this.type = type;
            this.display = display;
            this.primary = isPrimary;
        }
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
            private Boolean primary;
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
