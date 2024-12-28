package c1wxautomator.backend.dtos.organizations;

// Author: Natalie Jungquist
//
// This is a custom class representing a response that the application sends to the client.
//
// Usage:
// This class is used to map the response from Webex API when fetching organization details.

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrganizationDetailsResponse {
    private String displayName;
    private String id;
}
