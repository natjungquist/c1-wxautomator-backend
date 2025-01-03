package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist
//
// This class holds data for the request sent to the Webex API for assigning licenses to users.
// Its fields are named to fit Webex API request specifications.
//
// Usage:
// Services will use this class to send data to Webex API.

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class LicenseAssignmentRequest {
    private String email;
    private String orgId;
    @JsonProperty("personId")
    private String webexId;
    private List<LicenseRequest> licenses;

    @Setter
    @Getter
    public static class LicenseRequest {
        private String operation;
        private String id;
        private List<LicenseProperty> properties;
    }

    @Setter
    @Getter
    public static class LicenseProperty {
        private String locationId;
        private String phoneNumber;
        private String extension;
    }

}
