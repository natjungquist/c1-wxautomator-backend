package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for managing user GET operations with Webex APIs.
// Key features:
//      - search all users at an organization
//
// Usage:
// Used by any controller or service that needs to use Webex SCIM 2 User API with GET operation.

import c1wxautomator.backend.dtos.users.SearchUsersResponse;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
public class UserGetter {
    /**
     * Calls the Webex API to get a list of users at an organization.
     *
     * @param accessToken The token used for authenticating the request.
     * @param orgId id of the organization to export users to.
     * @return ApiResponseWrapper with 'data' being a list of users at the organization.
     */
    public ApiResponseWrapper searchUsers(String accessToken, String orgId) {
        ApiResponseWrapper webexResponse = new ApiResponseWrapper();

        String URL = String.format("https://webexapis.com/identity/scim/%s/v2/Users", orgId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchUsersResponse> requestEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<SearchUsersResponse> response = restTemplate.exchange(URL, HttpMethod.GET,
                    requestEntity, new ParameterizedTypeReference<>() {});
            if (response.getStatusCode().is2xxSuccessful()) {
                SearchUsersResponse searchUsers = response.getBody();
                webexResponse.setData(searchUsers);
                webexResponse.setStatus(response.getStatusCode().value());
            } else {
                webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                webexResponse.setMessage(String.format("An unexpected error occurred retrieving the users " +
                        "at the organization with id: %s", orgId));
            }
            return webexResponse;

            // NOTE: all possible exceptions are caught in this code for (1) debugging purposes and (2) to return
            // meaningful responses to client via ApiResponseWrapper.
        } catch (HttpClientErrorException e) { // These occur when the HTTP response status code is 4xx.
            // Examples:  400 Bad Request, 401 Unauthorized, 404 Not Found, 403 Forbidden
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("Webex API returned a 4xx error for retrieving users: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
            // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Webex API returned a 5xx error for retrieving users: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
            // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage("Error accessing Webex API when trying to retrieve users: " + e.getMessage());
            return webexResponse;

        } catch (RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
            //and all other runtime exceptions within the RestTemplate.
            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error retrieving the organization's users due to logical error in server program: " + e.getMessage());
            return webexResponse;
        }
    }
}
