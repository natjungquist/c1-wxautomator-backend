package c1wxautomator.backend.config;

// Author: Natalie Jungquist

import c1wxautomator.backend.services.WxAuthorizationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *  This class handles the redirection after a successful login. It extends `SavedRequestAwareAuthenticationSuccessHandler`
 *  to ensure that after successful authentication, the user is redirected to a specific URL.
 *  *
 *  Key features:
 *      - The URL is dynamically obtained from the `frontend.url` property in the application's configuration.
 *      - The default target URL is set to `/home` on the frontend after a successful login.
 *  *
 *  Usage:
 *  SecurityConfig registers this
 */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    private final WxAuthorizationService wxAuthorizationService;

    /**
     * Constructor with dependency injection.
     *
     * @param wxAuthorizationService to perform operations related to Webex details of the oauth2 client.
     */
    public LoginSuccessHandler(WxAuthorizationService wxAuthorizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
    }

    /**
     * Handles a successful authentication event by storing the authorization code
     * and redirecting the user to the home page.
     *
     * @param request the HttpServletRequest object that contains the request the client made to the servlet
     * @param response the HttpServletResponse object that contains the response the servlet returns to the client
     * @param authentication the Authentication object that represents the successful authentication
     * @throws ServletException if a servlet-specific error occurs * @throws IOException if an I/O error occurs
     * */
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

