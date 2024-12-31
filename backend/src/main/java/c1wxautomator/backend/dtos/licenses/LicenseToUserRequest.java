package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist
//
// TODO
//
// Usage:
// This class is used

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class LicenseToUserRequest {
    private String personId;
    private String email;
    private String orgId;
    private List<LicenseAssignmentRequest> licenses;

    @Setter
    @Getter
    public static class LicenseAssignmentRequest {
        private String id;
        private final String operation = "add";
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
