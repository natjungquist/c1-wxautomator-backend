package c1wxautomator.backend.config;

// Author: Natalie Jungquist
//
// This class handles a successful logout. It extends 'LogoutHandler' to ensure that after
// successful logout, the app's cache of authorized org IDs are cleared so logins made after this
// will not have the wrong authorized org IDs.
//
// Usage:
// SecurityConfig registers this

import c1wxautomator.backend.services.WxAuthorizationService;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

@Component
public class LogoutSuccessHandler implements LogoutHandler {

    private final WxAuthorizationService wxAuthorizationService;

    public LogoutSuccessHandler(WxAuthorizationService wxAuthorizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            wxAuthorizationService.clearAuthorizedOrgIds();
        }
    }
}

