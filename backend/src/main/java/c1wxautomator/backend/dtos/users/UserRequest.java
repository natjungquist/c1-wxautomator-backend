package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class holds data for user information needed to export users.
 *  Its fields are named to fit Webex API request specifications.
 * //
 *  Contains inner classes that represent nested json structure for request.
 *  Contains methods to set inner class data. This makes the code more modular, understandable,
 *       and maintainable, ensuring proper data handling and integration with the Webex API.
 * //
 *  NOTE that other user metadata such as their licenses is encapsulated by the UserMetadata class.
 *       The metadata is separate because it cannot be included as part of the UserRequest data sent to the Webex API.
 * //
 *  NOTE that the emails field is required, and one of the emails in it must match the email/userName field,
 *       but it should be set to 'primary':false.
 * //
 *  Usage:
 *  UserService transfers client's data into this User class before sending it as part of a request to the Webex API.
 */
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
     * Adds non-primary work phone number to the user's list of phone numbers.
     *
     * @param workNumber representing the phone number to be added.
     */
    public void addNonPrimaryWorkNumber(String workNumber) {
        this.phoneNumbers.add( new PhoneNumber(workNumber, "work", "Work", false) );
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

    // REQUIRED

    /**
     * Nested class to represent the 'name' field of a user request.
     */
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
    /**
     * Nested class to represent the 'email' field of a user request.
     */
    @Setter
    @Getter
    public static class Email {
        private String value;
        private String type;
        private String display;
        private Boolean primary;

        /**
         * Constructor of an email field.
         *
         * @param value the email as a string
         * @param type the type of email. Possible values: 'work', 'home', 'room', or 'other'
         * @param display the display name of the email that appears on the Webex desktop app
         * @param isPrimary true if the email if their primary email
         */
        public Email(String value, String type, String display, boolean isPrimary) {
            this.value = value;
            this.type = type;
            this.display = display;
            this.primary = isPrimary;
        }
    }

    // REQUIRED
    /**
     * Nested class to represent the 'phone number' field of a user request.
     */
    @Setter
    @Getter
    public static class PhoneNumber {
        private String value;
        private String type;
        private String display;
        private Boolean primary;

        /**
         * Constructor of a phone number field.
         *
         * @param value the phone number as a string
         * @param type the type of phone number. Possible values: 'work', 'home', 'mobile',
         *             'work_extension', 'fax', 'pager', 'other'
         * @param display a human-readable name, primarily used for display purposes
         * @param isPrimary true if this phone number is the user's primary phone number
         */
        public PhoneNumber(String value, String type, String display, boolean isPrimary) {
            this.value = value;
            this.type = type;
            this.display = display;
            this.primary = isPrimary;
        }
    }


    // OPTIONAL

    /**
     * Nested class representing the 'photo' field of a user request.
     */
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
    /**
     * Nested class representing the 'address' field of a user request.
     */
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
    /**
     * Nested class representing the 'enterprise user' schema field of a user request.
     */
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

        /**
         * Nested class representing the 'manager' field of a enterprise user schema.
         */
        @Setter
        @Getter
        @NoArgsConstructor
        public static class Manager {
            private String value;
        }
    }

    // OPTIONAL
    /**
     * Nested class representing the 'cisco identity user' schema field of a user request.
     */
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

        /**
         * Nested class representing the 'sip address' field of a cisco identity schema.
         */
        @Setter
        @Getter
        @NoArgsConstructor
        public static class SipAddress {
            private String value;
            private String type;
            private String display;
            private Boolean primary;
        }

        /**
         * Nested class representing the 'managed org' field of a cisco identity schema.
         * The organizations that a user can manage.
         */
        @Setter
        @Getter
        @NoArgsConstructor
        public static class ManagedOrg {
            private String orgId;
            private String role;
        }

        /**
         * Nested class representing the 'managed group' field of a cisco identity schema.
         */
        @Setter
        @Getter
        @NoArgsConstructor
        public static class ManagedGroup {
            private String orgId;
            private String groupId;
            private String role;
        }

        /**
         * Nested class representing the 'external attribute' field of a cisco identity schema.
         */
        @Setter
        @Getter
        @NoArgsConstructor
        public static class ExternalAttribute {
            private String source;
            private String value;
        }
    }
}
