package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist
//
// This class holds data for the request sent to the Webex API for assigning licenses to users.
// Its fields are named to fit Webex API request specifications.
//
// Usage:
// Services will use this class to send data to Webex API.

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class AssignLicenseRequest {
    private String email;
    private String personId;  //called webexId elsewhere
    private String orgId;
    private List<LicenseRequest> licenses = new ArrayList<>();

    public AssignLicenseRequest(String email, String orgId, String webexId) {
        this.email = email;
        this.orgId = orgId;
        this.personId = webexId;
    }

    public void addLicense(LicenseRequest license) {
        this.licenses.add(license);
    }

    @Setter
    @Getter
    public static class LicenseRequest {
        private String operation;
        private String id;
        private LicenseProperty properties;  // 'properties' field must be null unless otherwise specified

        public LicenseRequest(String operation, String id, String locationId, String extension){
            this.operation = operation;
            this.id = id;
            this.properties = new LicenseProperty(locationId, extension);  // 'properties' field is not null ONLY for assigning webex calling - professional license
        }

        public LicenseRequest(String operation, String id){
            this.operation = operation;
            this.id = id;
        }
    }

    @Setter
    @Getter
    public static class LicenseProperty {
        private String locationId;
        private String extension;

//        private String phoneNumber; // NOTE this field is required for other types of licenses, but right now this app can only do extensions with main caller ID number of location

        public LicenseProperty(String locationId, String extension) {
            this.locationId = locationId;
            this.extension = extension;
        }
    }

}
