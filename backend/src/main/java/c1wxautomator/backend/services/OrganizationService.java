package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// This service class fetches organization details for the authenticated user. It interacts with the WebEx API to retrieve
// information about the user's organization, such as its ID and display name. It also handles parsing the API response
// and managing the authorization code flow for provisioning calls.
//
// Dependencies:
//      - ObjectMapper and JsonNode to transfer Webex API response to data transfer object OrganizationDetailsResponse
//      - custom data transfer object OrganizationDetailsResponse to send custom response
//
// Usage:
// Used by any controller that needs to call the Webex API for organization details.

import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import c1wxautomator.backend.dtos.organizations.OrganizationDetailsResponse;

@Service
public class OrganizationService {

    /**
     * Retrieves the orgId from the application's saved authentication information.
     *
     * @return organization id of the authenticated user
     */
    public String getMyOrgId() {

        String orgId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            orgId = oauthToken.getPrincipal().getAttribute("orgId");  // the principle is the user's information
        }
        return orgId;
    }

    /**
     * Fetches the details of the organization using an access token and the orgId.
     * Note that as of 1/1/2025, the orgId returned by Webex's 'list organizations' endpoint
     * is not the same as the orgId needed to make API calls for provisioning.
     * The orgId needed to make calls for provisioning is provided in the authorization code
     * which is set/stored by this application's wxAuthenticationService.
     *
     * @param accessToken The token used for authenticating the request.
     * @return custom ApiResponseWrapper object where 'status' is the status of the response from
     * the call to the Webex API and 'data' is the organization details or null if there is an error.
     */
    public ApiResponseWrapper getMyOrganizationDetails(String accessToken) {

        String orgId = getMyOrgId();

        return getOrganizationDetails(accessToken, orgId);
    }

    /**
     * Calls Webex API to get details of a specific organization, specified by id.
     *
     * @param accessToken The token used for authenticating the request.
     * @param orgId       id of the organization to get the details of.
     * @return custom ApiResponseWrapper object where 'status' is the status of the response from
     * the call to the Webex API and 'data' is the organization details or null if there is an error.
     */
    public ApiResponseWrapper getOrganizationDetails(String accessToken, String orgId) {

        ApiResponseWrapper webexResponse = new ApiResponseWrapper();

        String url = String.format("https://webexapis.com/v1/organizations/%s", orgId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        // Per the Webex documentation, possible responses are 2xx, 4xx, or 5xx.
        // They will be handled and interpreted here.
        // Java throws exceptions for 4xx and 5xx status codes, so this must be in a try-catch block.
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();
            HttpStatusCode statusCode = response.getStatusCode();

            if (statusCode.is2xxSuccessful() && responseBody != null) {
                OrganizationDetailsResponse organizationDetailsResponse = createCustomOrganizationResponse(responseBody);
                if (organizationDetailsResponse == null) {
                    webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    webexResponse.setMessage("Error creating organization details response due to logical error in server program..");
                }
                webexResponse.setData(organizationDetailsResponse);
                webexResponse.setStatus(statusCode.value());
            } else {
                webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                webexResponse.setMessage("An unexpected error occurred getting organization details.");
            }
            return webexResponse;

            // NOTE: all possible exceptions are caught in this code for (1) debugging purposes and (2) to return
            // meaningful responses to client via ApiResponseWrapper.
        } catch (HttpClientErrorException e) { // These occur when the HTTP response status code is 4xx.
            // Examples:  400 Bad Request, 401 Unauthorized, 404 Not Found, 403 Forbidden
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("Webex API returned a 4xx error for getting organization details: " + e.getResponseBodyAsString());
            return webexResponse;
        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
            // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Webex API returned a 5xx error for getting organization details: " + e.getResponseBodyAsString());
            return webexResponse;
        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
            // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage("Error accessing Webex API when trying to get organization details: " + e.getMessage());
            return webexResponse;
        } catch (
                RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
            //and all other runtime exceptions within the RestTemplate.
            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error getting organization details from Webex API due to logical error in server program: " + e.getMessage());
            return webexResponse;
        }
    }

    /**
     * Builds an Organization response entity
     *
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

