package c1wxautomator.backend.controllers;

// Author: Natalie Jungquist
// This controller handles authentication-related endpoints for the application.
// Key features include:
// - The `/check-auth` endpoint, which checks whether a user is authenticated by verifying the access token.
// - It returns a JSON response indicating whether the user is logged in or not.

import c1wxautomator.backend.services.WxAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class AuthController {

    private final WxAuthorizationService wxAuthorizationService;

    public AuthController(WxAuthorizationService wxAuthorizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
    }

    /**
     * Endpoint to validate if the user is logged in or not.
     * @return ResponseEntity as json containing true or false
     */
    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, String>> checkAuth() {

        Map<String, String> response = new HashMap<>();

        // check if they are logged in by seeing if an access token has been granted and saved
        String accessToken = wxAuthorizationService.getAccessToken();
        if (accessToken == null) {
            response.put("isAuthenticated", "false");
        } else {
            response.put("isAuthenticated", "true");
        }

        // get the provider they are logged in with
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            if (Objects.equals(clientRegistrationId, "webex")) {
                response.put("authProvider", "webex");
            } else if (Objects.equals(clientRegistrationId, "webex-cc")) {
                response.put("authProvider", "webex-cc");
            }
        }
        return ResponseEntity.ok(response);
    }
}