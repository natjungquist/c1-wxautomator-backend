package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class holds data for the request sent to the Webex API for assigning licenses to users.
 *  Its fields are named to fit Webex API request specifications.
 *  *
 *  Usage:
 *  Services will use this class to send data to Webex API.
 */
@Setter
@Getter
public class AssignLicenseRequest {
    private String email;
    private String personId;
    private String orgId;
    private List<LicenseRequest> licenses = new ArrayList<>();

    /**
     * Constructor
     *
     * @param email address of the user
     * @param orgId organization ID
     * @param personId ID of the person
     */
    public AssignLicenseRequest(String email, String orgId, String personId) {
        this.email = email;
        this.orgId = orgId;
        this.personId = personId;
    }

    /**
     * Adds a license request to the list of licenses.
     *
     * @param license request to be added
     */
    public void addLicense(LicenseRequest license) {
        this.licenses.add(license);
    }

    /**
     * Class representing the structure of a nested license request in the licenses list.
     */
    @Setter
    @Getter
    public static class LicenseRequest {
        private String operation;
        private String id;
        private LicenseProperty properties;  // 'properties' field must be null unless otherwise specified

        /**
         * Constructor for 'Webex Calling - Professional' license requests.
         *
         * @param operation to be performed with this license on the user
         * @param id of the license
         * @param locationId of the location of the user
         * @param extension of the user
         */
        public LicenseRequest(String operation, String id, String locationId, String extension){
            this.operation = operation;
            this.id = id;
            this.properties = new LicenseProperty(locationId, extension);  // 'properties' field is not null ONLY for assigning webex calling - professional license
        }

        /**
         * Constructor for all other kinds of license requests.
         *
         * @param operation to be performed with this license on the user
         * @param id of the license
         */
        public LicenseRequest(String operation, String id){
            this.operation = operation;
            this.id = id;
        }
    }

    /**
     * Class that represents the request structure nested in the LicenseProperty.
     */
    @Setter
    @Getter
    public static class LicenseProperty {
        private String locationId;
        private String extension;

//        private String phoneNumber; // NOTE this field is required for other types of licenses, but right now this app can only do extensions with main caller ID number of location

        /**
         * Constructor that fills all fields.
         *
         * @param locationId of the location of the user.
         * @param extension of the user.
         */
        public LicenseProperty(String locationId, String extension) {
            this.locationId = locationId;
            this.extension = extension;
        }
    }

}
