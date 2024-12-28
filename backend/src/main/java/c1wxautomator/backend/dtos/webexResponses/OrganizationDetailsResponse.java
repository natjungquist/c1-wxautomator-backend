package c1wxautomator.backend.dtos.webexResponses;

// Author: Natalie Jungquist
//
// This class represents the response structure for an organization details query in the WebEx system.
// It contains the basic information about an organization, specifically the display name and unique ID.
// This class is used to map the response from WebEx API when fetching organization details.

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrganizationDetailsResponse {
    private String displayName;
    private String id;
}
