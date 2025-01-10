package c1wxautomator.backend.dtos.organizations;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;

/**
 *  This is a custom class representing a response that the application sends to the client.
 *  *
 *  Usage:
 *  This class is used to map the response from Webex API when fetching organization details.
 */
@Setter
@Getter
public class OrganizationDetailsResponse {
    private String displayName;
    private String id;
}
