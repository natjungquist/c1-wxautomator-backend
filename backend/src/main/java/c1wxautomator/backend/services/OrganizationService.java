package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// This service class fetches organization details for the authenticated user. It interacts with the WebEx API to retrieve
// information about the user's organization, such as its ID and display name. It also handles parsing the API response
// and managing the authorization code flow for provisioning calls.
//
// Dependencies:
//      - JsonNode to transfer Webex API response to data transfer object OrganizationDetailsResponse
//      - custom data transfer object OrganizationDetailsResponse to send custom response
//
// Usage:
// Used by any controller that needs to call the Webex API for organization details.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import c1wxautomator.backend.dtos.organizations.OrganizationDetailsResponse;

@Service
public class OrganizationService {

    /**
     * Retrieves the orgId from the application's saved authentication information.
     *
     * @return organization id of the authenticated user
     */
    public String getOrgId() {

        String orgId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            orgId = oauthToken.getPrincipal().getAttribute("orgId");  // the principle is the user's information
        }
        return orgId;
    }

    /**
     * Fetches the details of the organization using an access token and the orgId.
     * Note that as of 1/1/2025, the orgId associated with the list organizations endpoint
     *      is not the same as the orgId needed to make API calls for provisioning.
     *      The orgId needed to make calls for provisioning is provided in the authorization code
     *      and set/stored by this application's wxAuthenticationService.
     *
     * @param accessToken The token used for authenticating the request.
     * @return custom object containing the organization details, else null if there is an error
     */
    public OrganizationDetailsResponse getOrganizationDetails(String accessToken) {

        String orgId = getOrgId();
        String url = String.format("https://webexapis.com/v1/organizations/%s", orgId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();
            HttpStatusCode statusCode = response.getStatusCode();

            if (statusCode.is2xxSuccessful() && responseBody != null) {
                return createCustomOrganizationResponse(responseBody);
            } else {
                return null;
            }
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds an Organization response entity
     * @param responseBody A response from an external API call
     * @return custom object with all fields set, else null
     */
    private static OrganizationDetailsResponse createCustomOrganizationResponse(String responseBody) {
        try {
            OrganizationDetailsResponse organizationDetailsResponse = new OrganizationDetailsResponse();
            // Parse the JSON response body
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String displayName = jsonNode.get("displayName").asText();
            String thisOrgId = jsonNode.get("id").asText();

            // Set values in the Organization object
            organizationDetailsResponse.setDisplayName(displayName);
            organizationDetailsResponse.setId(thisOrgId);
            return organizationDetailsResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

