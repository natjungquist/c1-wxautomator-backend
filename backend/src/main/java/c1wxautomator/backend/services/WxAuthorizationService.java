package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// This service class handles OAuth2 authorization for Webex. It provides methods to retrieve access tokens, user details,
// and manage the organization IDs that the authenticated user is authorized to interact with.
// It uses the Spring Security OAuth2 library to manage OAuth2 authentication and authorization flows.
//
// Dependencies:
//      - OAuth2AuthorizedClientService to get the current oauth client's info
//
// Usage:
// Used by any service or controller that needs to retrieve oauth2 info about the current user/client.

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WxAuthorizationService {
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final List<String> authorizedOrgIds = new ArrayList<>();

    public WxAuthorizationService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * Retrieves the OAuth2 access token for the authenticated user.
     *
     * @return the access token as a String, or null if not available
     */
    public String getAccessToken() {
        String accessToken = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(clientRegistrationId, oauthToken.getName());
            if (authorizedClient != null) {
                OAuth2AccessToken accessTokenObj = authorizedClient.getAccessToken();
                accessToken = accessTokenObj.getTokenValue();
            }
        }
        return accessToken;
    }

    /**
     * Retrieves the OAuth2 user details for the authenticated user.
     *
     * @return the user's name as a String, or null if not available
     */
    public String getUserDisplayName() {
        String userDisplayName = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User user = oauthToken.getPrincipal();
            Map<String, Object> attributes = user.getAttributes();
            if (attributes != null) {
                userDisplayName = attributes.get("displayName").toString();
            }
        }
        return userDisplayName;
    }

    /**
     * Stores the id of the organization that the user is authorized to make API calls on behalf of.
     * The orgId is embedded in the code from the authorization code flow.
     *
     * @param code the authorization code the app was given to ask for an access token.
     */
    public void storeAuthorizedId(String code) {
        String[] parts = code.split("_");
        String authorizedOrgId = parts[2];
        authorizedOrgIds.add(authorizedOrgId);
    }

    /**
     * Gets the org ID that the user is authorized to make API calls on behalf of.
     * As of 1/1/2025, Webex can only authorize OAuth2 users with ONE organization at a time.
     * But authorizedOrgIds is a list here in case the Webex API allows us to be authorized with multiple organizations in the future.
     *
     * @return an org ID that is usable by the Webex API for provisioning calls.
     */
    public String getAuthorizedOrgId() {
        return authorizedOrgIds.getFirst();
    }

    /**
     * Determines if the user is authorized to make API calls on behalf of this organization.
     *
     * @param orgId the id of the organization
     * @return true or false
     */
    public boolean orgIsAuthorized(String orgId) {
        return authorizedOrgIds.contains(orgId);
    }

    /**
     * Removes the orgId from the stored array of organizations that the user is authorized to make API calls on behalf of.
     *
     * @param orgId the id of the organization to be removed
     */
    public void removeAuthorizedOrgId(String orgId) {
        authorizedOrgIds.remove(orgId);
    }

    /**
     * Clears memory of all authorized organization IDs.
     */
    public void clearAuthorizedOrgIds() {
        authorizedOrgIds.clear();
    }
}
