package c1wxautomator.backend.config;

// Author: Natalie Jungquist

import c1wxautomator.backend.services.WxAuthorizationService;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

/**
 *  This class handles a successful logout. It extends 'LogoutHandler' to ensure that after
 *  successful logout, the app's cache of authorized org IDs are cleared so logins made after this
 *  will not have the wrong authorized org IDs.
 *  *
 *  Usage:
 *  SecurityConfig registers this
 */
@Component
public class LogoutSuccessHandler implements LogoutHandler {

    private final WxAuthorizationService wxAuthorizationService;

    /**
     * Constructor with dependency injection.
     *
     * @param wxAuthorizationService to perform operations related to Webex details of the oauth2 client.
     */
    public LogoutSuccessHandler(WxAuthorizationService wxAuthorizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
    }

    /**
     * Handles the logout event by clearing the authorized organization IDs.
     *
     * @param request the HttpServletRequest object that contains the request the client made to the servlet
     * @param response the HttpServletResponse object that contains the response the servlet returns to the client
     * @param authentication the Authentication object that represents the current authentication state
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            wxAuthorizationService.clearAuthorizedOrgIds();
        }
    }
}

