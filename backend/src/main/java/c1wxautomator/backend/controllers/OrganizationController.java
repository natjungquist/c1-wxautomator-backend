package c1wxautomator.backend.controllers;

// Author: Natalie Jungquist

import c1wxautomator.backend.dtos.organizations.OrganizationDetailsResponse;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import c1wxautomator.backend.services.OrganizationService;
import c1wxautomator.backend.services.WxAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

/**
 *  This controller manages organization-related endpoints in the application.
 *  Key features include:
 *       - The `/my-organization` endpoint, which retrieves details about the authenticated user's organization using the access token.
 *       - The `/my-name` endpoint, which fetches and returns the authenticated user's display name.
 *       - If no valid access token is found, endpoints return a `NOT_FOUND` status with an appropriate error message.
 *  *
 *  Dependencies:
 *       - wxAuthorizationService to retrieve access token
 *       - organizationService to retrieve organization details
 *  *
 *  Usage:
 *  Backend endpoints for client to request details about their organization.
 */
@RestController
public class OrganizationController {

    private final WxAuthorizationService wxAuthorizationService;
    private final OrganizationService organizationService;

    /**
     * Constructor with dependency injection.
     *
     * @param wxAuthorizationService to perform operations related to Webex details of the oauth2 client.
     * @param organizationService to perform operations related to organizations.
     */
    @Autowired
    public OrganizationController(WxAuthorizationService wxAuthorizationService, OrganizationService organizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
        this.organizationService = organizationService;
    }

    /**
     * Endpoint for returning a specific organization's details.
     *
     * @return ResponseEntity containing the organization details.
     */
    @GetMapping("/my-organization")
    public ResponseEntity<?> getMyOrganizationDetails() {
        String accessToken = wxAuthorizationService.getAccessToken();
        if (accessToken != null) {
            ApiResponseWrapper<OrganizationDetailsResponse> webexResponse = organizationService.getMyOrganizationDetails(accessToken);
            if (webexResponse.is2xxSuccess() && webexResponse.hasData()) {
                OrganizationDetailsResponse organizationDetailsResponse = webexResponse.getData();
                return ResponseEntity.ok(organizationDetailsResponse);
            } else {
                // return a response corresponding to what is set in ApiResponseWrapper
                return ResponseEntity.status(webexResponse.getStatus()).body(webexResponse.getMessage());
            }
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to get organization details because no access token was provided");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Endpoint for returning the authorized user display name.
     *
     * @return ResponseEntity as json containing the user display name.
     */
    @GetMapping("/my-name")
    public ResponseEntity<Map<String, String>> getAuthorizedUserDisplayName() {
        String userDisplayName = wxAuthorizationService.getUserDisplayName();
        if (userDisplayName != null) {
            Map<String, String> response = new HashMap<>();
            response.put("displayName", userDisplayName);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to get authenticated user display name because no access token was provided");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}