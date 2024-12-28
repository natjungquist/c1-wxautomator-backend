package c1wxautomator.backend.config;

// Author: Natalie Jungquist
//
// This class handles the redirection after a successful login. It extends `SavedRequestAwareAuthenticationSuccessHandler`
// to ensure that after successful authentication, the user is redirected to a specific URL.
//
// - The URL is dynamically obtained from the `frontend.url` property in the application's configuration.
// - The default target URL is set to `/home` on the frontend after a successful login.
//
// Usage:
// SecurityConfig registers this

import c1wxautomator.backend.services.WxAuthorizationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    private final WxAuthorizationService wxAuthorizationService;

    public LoginSuccessHandler(WxAuthorizationService wxAuthorizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        // Extract the authorization code from the request
        String authorizationCode = request.getParameter("code");
        if (authorizationCode != null) {
            wxAuthorizationService.storeAuthorizedId(authorizationCode);
        }

        // On success, redirect back to the frontend
        this.setAlwaysUseDefaultTargetUrl(true);
        this.setDefaultTargetUrl(frontendUrl + "/home");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

