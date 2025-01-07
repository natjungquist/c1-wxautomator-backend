package c1wxautomator.backend.dtos.licenses;
// Author: Natalie Jungquist
//
// This class represents the response structure for PATCH from the Webex API to assign licenses to a user.
//
// Usage:
// Services that make calls to the Webex API endpoint to assign licenses will receive a response represented by this data structure.

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AssignLicenseResponse {
    private String orgId;
    private String personId;
    private String email;
    private List<String> licenses;
    private List<SiteUrl> siteUrls;

    @Setter
    @Getter
    public static class SiteUrl {
        private String siteUrl;
        private String accountType;
    }
}
