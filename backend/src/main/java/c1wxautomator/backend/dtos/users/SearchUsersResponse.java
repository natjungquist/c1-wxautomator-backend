package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *  This class represents the response structure for a result as returned by the Webex API.
 *  *
 *  Usage:
 *  Services that make calls to the Webex API endpoint to search for existing users will receive
 *  a response represented by this data structure.
 */
@Getter
@Setter
public class SearchUsersResponse {
    private List<String> schemas;
    private Integer totalResults;
    private Integer itemsPerPage;
    private Integer startIndex;

    // Map the top-level user array to this field
    @JsonProperty("Resources")
    private List<Resource> resources;

    /**
     * Nested class representing the 'resources' part of the response.
     */
    @Getter
    @Setter
    public static class Resource {
        private List<String> schemas;
        private String id;
        private String userName;
        private Boolean active;
        private Name name;
        private String displayName;
        private String nickName;
        private List<Email> emails;
        private String userType;
        private String profileUrl;
        private String title;
        private String preferredLanguage;
        private String locale;
        private String externalId;
        private String timezone;
        private List<PhoneNumber> phoneNumbers;
        private List<Photo> photos;
        private List<Address> addresses;

        @JsonProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
        private EnterpriseUserExtension enterpriseUser;

        @JsonProperty("urn:scim:schemas:extension:cisco:webexidentity:2.0:User")
        private CiscoUserExtension ciscoUser;

        private Meta meta;

        /**
         * Nested class representing the 'name' part of the search users response.
         */
        @Getter
        @Setter
        public static class Name {
            private String familyName;
            private String givenName;
            private String middleName;
            private String honorificPrefix;
            private String honorificSuffix;
        }

        /**
         * Nested class representing the 'email' part of the search users resource.
         */
        @Getter
        @Setter
        public static class Email {
            private String value;
            private String type;
            private Boolean primary;
            private String display;
        }

        /**
         * Nested class representing the 'phone number' part of the search users resource.
         */
        @Getter
        @Setter
        public static class PhoneNumber {
            private String value;
            private String type;
            private Boolean primary;
            private String display;
        }

        /**
         * Nested class representing the 'photo' part of the search users resource.
         */
        @Getter
        @Setter
        public static class Photo {
            private String value;
            private String type;
            private Boolean primary;
            private String display;
        }

        /**
         * Nested class representing the 'address' part of the search users resource.
         */
        @Getter
        @Setter
        public static class Address {
            private String type;
            private String streetAddress;
            private String locality;
            private String region;
            private String postalCode;
            private String country;
        }

        /**
         * Nested class representing the 'enterprise' part of the search users resource.
         */
        @Getter
        @Setter
        public static class EnterpriseUserExtension {
            private String employeeNumber;
            private String costCenter;
            private String organization;
            private String division;
            private String department;
            private Manager manager;

            /**
             * Nested class representing the 'manager' part of the enterprise schema.
             */
            @Getter
            @Setter
            public static class Manager {
                private String value;
                private String displayName;
                private String ref;
            }
        }

        /**
         * Nested class representing the 'cisco extension' part of the search users response.
         */
        @Getter
        @Setter
        public static class CiscoUserExtension {
            private List<String> accountStatus;
            private List<SipAddress> sipAddresses;
            private List<ManagedOrg> managedOrgs;
            private List<ManagedGroup> managedGroups;
            private List<ExternalAttribute> externalAttribute1;
            private List<ExternalAttribute> externalAttribute2;
            private List<String> extensionAttribute1;
            private List<String> extensionAttribute2;
            private Meta meta;

            /**
             * Nested class representing the 'sip address' part of the cisco schema.
             */
            @Getter
            @Setter
            public static class SipAddress {
                private String value;
                private String type;
                private Boolean primary;
                private String display;
            }

            /**
             * Nested class representing the 'managed org' part of the cisco schema.
             */
            @Getter
            @Setter
            public static class ManagedOrg {
                private String orgId;
                private String role;
            }

            /**
             * Nested class representing the 'managed group' part of the cisco schema.
             */
            @Getter
            @Setter
            public static class ManagedGroup {
                private String orgId;
                private String groupId;
                private String role;
            }

            /**
             * Nested class representing the 'external attribute' part of the cisco schema.
             */
            @Getter
            @Setter
            public static class ExternalAttribute {
                private String source;
                private String value;
            }
        }

        /**
         * Nested class representing the 'meta' data of the search users resource.
         */
        @Getter
        @Setter
        public static class Meta {
            private String resourceType;
            private String location;
            private String version;
            private String created;
            private String lastModified;
        }
    }
}
