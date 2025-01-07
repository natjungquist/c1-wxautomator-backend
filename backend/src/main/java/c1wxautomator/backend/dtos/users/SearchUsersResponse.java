package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class represents the response structure for a result as returned by the Webex API.
//
// Usage:
// Services that make calls to the Webex API endpoint to search for existing users will receive
// a response represented by this data structure.

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

        @Getter
        @Setter
        public static class Name {
            private String familyName;
            private String givenName;
            private String middleName;
            private String honorificPrefix;
            private String honorificSuffix;
        }

        @Getter
        @Setter
        public static class Email {
            private String value;
            private String type;
            private Boolean primary;
            private String display;
        }

        @Getter
        @Setter
        public static class PhoneNumber {
            private String value;
            private String type;
            private Boolean primary;
            private String display;
        }

        @Getter
        @Setter
        public static class Photo {
            private String value;
            private String type;
            private Boolean primary;
            private String display;
        }

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

        @Getter
        @Setter
        public static class EnterpriseUserExtension {
            private String employeeNumber;
            private String costCenter;
            private String organization;
            private String division;
            private String department;
            private Manager manager;

            @Getter
            @Setter
            public static class Manager {
                private String value;
                private String displayName;
                private String ref;
            }
        }

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

            @Getter
            @Setter
            public static class SipAddress {
                private String value;
                private String type;
                private Boolean primary;
                private String display;
            }

            @Getter
            @Setter
            public static class ManagedOrg {
                private String orgId;
                private String role;
            }

            @Getter
            @Setter
            public static class ManagedGroup {
                private String orgId;
                private String groupId;
                private String role;
            }

            @Getter
            @Setter
            public static class ExternalAttribute {
                private String source;
                private String value;
            }
        }

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
