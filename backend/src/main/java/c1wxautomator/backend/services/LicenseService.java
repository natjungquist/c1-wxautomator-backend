package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for managing license operations with Webex APIs.
// Key features:
//      - list all the available licenses of an organization.
//      - retrieve details about specific licenses such as 'Contact center standard agent',
//        'contact center premium agent', and 'webex calling professional'.
//
// Usage:
// Used by any controller or service that needs to use Webex license API.

import c1wxautomator.backend.dtos.licenses.License;
import c1wxautomator.backend.dtos.licenses.AssignLicenseRequest;
import c1wxautomator.backend.dtos.licenses.ListLicensesResponse;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LicenseService {

    /**
     * Calls the Webex API to get a list of all licenses owned by an organization.
     * Uses the RestTemplate to make the API call and handles authorization with an OAuth2 token.
     *
     * @param accessToken The token used for authenticating the request.
     * @param orgId       id of the organization to export users to.
     * @return ApiResponseWrapper with 'data' being a list of available licenses.
     */
    public ApiResponseWrapper listLicenses(String accessToken, String orgId) {
        ApiResponseWrapper webexResponse = new ApiResponseWrapper();

        String URL = String.format("https://webexapis.com/v1/licenses?orgId=%s", orgId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ListLicensesResponse> requestEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        // Per the Webex documentation, possible responses are 2xx, 4xx, or 5xx.
        // They will be handled and interpreted here.
        // Java throws exceptions for 4xx and 5xx status codes, so this must be in a try-catch block.
        try {
            ResponseEntity<ListLicensesResponse> response = restTemplate.exchange(URL, HttpMethod.GET,
                    requestEntity, new ParameterizedTypeReference<>() {
                    });
            if (response.getStatusCode().is2xxSuccessful()) {
                ListLicensesResponse listLicensesResponse = response.getBody();
                webexResponse.setData(listLicensesResponse);
                webexResponse.setStatus(response.getStatusCode().value());
            } else {
                webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                webexResponse.setMessage(String.format("An unexpected error occurred retrieving the license information " +
                        "at the organization with id: %s", orgId));
            }
            return webexResponse;

            // NOTE: all possible exceptions are caught in this code for (1) debugging purposes and (2) to return
            // meaningful responses to client via ApiResponseWrapper.
        } catch (HttpClientErrorException e) { // These occur when the HTTP response status code is 4xx.
            // Examples:  400 Bad Request, 401 Unauthorized, 404 Not Found, 403 Forbidden
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("Webex API returned a 4xx error for retrieving license information: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
            // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Webex API returned a 5xx error for retrieving license information: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
            // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage("Error accessing Webex API when trying to retrieve license information: " + e.getMessage());
            return webexResponse;

        } catch (
                RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
            //and all other runtime exceptions within the RestTemplate.
            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error retrieving the license information from Webex API due to logical error in server program: " + e.getMessage());
            return webexResponse;
        }
    }

    /**
     * Creates a map of all the licenses given. This map makes it easier to lookup by a license name.
     *
     * @param allLicenses list of licenses to be put in a map.
     * @return map of licenses where the name is the key and a license object is the value,
     * else null if allLicenses has nothing in it.
     */
    public Map<String, License> makeLicensesMap(List<License> allLicenses) {
        if (allLicenses == null || allLicenses.isEmpty()) {
            return null;
        }
        Map<String, License> licenseMap = new HashMap<>();
        for (License license : allLicenses) {
            licenseMap.put(license.getName(), license);
        }
        return licenseMap;
    }

    public AssignLicenseRequest createLicenseRequest(String userName, String orgId, String webexId, License license, String operation, String locationId, String extension) {
        return null;
    }

    public ApiResponseWrapper sendLicenseRequest(String accessToken, AssignLicenseRequest licenseRequest) {
        ApiResponseWrapper webexResponse = new ApiResponseWrapper();

        if (licenseRequest != null) {
            // TODO
        }

        WebClient webClient = WebClient.builder()
                .baseUrl("https://webexapis.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            Map<String, Object> licenseAssignment = webClient.patch()
                    .uri("/v1/licenses/users")
                    .bodyValue(licenseRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new HttpClientErrorException(response.statusCode(), errorBody))
                            )
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new HttpServerErrorException(response.statusCode(), errorBody))
                            )
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            webexResponse.setData(licenseAssignment);
            webexResponse.setStatus(HttpStatus.OK.value());
            return webexResponse;

            // NOTE: all possible exceptions are caught in this code for (1) debugging purposes and (2) to return
            // meaningful responses to client via ApiResponseWrapper.
        } catch (HttpClientErrorException e) { // These occur when the HTTP response status code is 4xx.
            // Examples:  400 Bad Request, 401 Unauthorized, 404 Not Found, 403 Forbidden
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage(String.format("Webex API returned a 4xx error for assigning license: %s", e.getResponseBodyAsString()));
            return webexResponse;

        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
            // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage(String.format("Webex API returned a 5xx error for assigning license: %s", e.getResponseBodyAsString()));
            return webexResponse;

        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
            // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage(String.format("Error accessing Webex API when trying to assign license : %s", e.getMessage()));
            return webexResponse;

        } catch (
                RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
            //and all other runtime exceptions within the RestTemplate.
            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage(String.format("Error assigning license: %s", e.getMessage()));
            return webexResponse;
        }
    }
}
