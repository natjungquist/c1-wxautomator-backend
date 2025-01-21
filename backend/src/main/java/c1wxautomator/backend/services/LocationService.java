package c1wxautomator.backend.services;

// Author: Natalie Jungquist

import c1wxautomator.backend.dtos.locations.ListFloorsResponse;
import c1wxautomator.backend.dtos.locations.ListLocationsResponse;
import c1wxautomator.backend.dtos.locations.Location;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Service class for managing location operations with Webex APIs.
 *  Key features:
 *       - list the available locations of an organization.
 *       - retrieve details about a specific location.
 *  *
 *  Usage:
 *  Used by any controller or service that needs to use Webex location API.
 */
@Service
public class LocationService {

    @Value("${webexApi.url}")
    private String apiBaseUrl;

    /**
     * Calls the Webex API to get a list of all locations of an organization.
     * Uses the RestTemplate to make the API call and handles authorization with an OAuth2 token.
     *
     * @param accessToken The token used for authenticating the request.
     * @param orgId id of the organization to export users to.
     * @return ApiResponseWrapper with 'data' being locations at the org.
     */
    public ApiResponseWrapper<ListLocationsResponse> listLocations(String accessToken, String orgId) {
        ApiResponseWrapper<ListLocationsResponse> webexResponse = new ApiResponseWrapper<>();

        String URL = String.format("https://webexapis.com/v1/locations?orgId=%s", orgId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ListLocationsResponse> requestEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<ListLocationsResponse> response = restTemplate.exchange(URL, HttpMethod.GET,
                    requestEntity, new ParameterizedTypeReference<>() {
                    });
            if (response.getStatusCode().is2xxSuccessful()) {
                ListLocationsResponse locationsResponse = response.getBody();
                webexResponse.setData(locationsResponse);
                webexResponse.setStatus(response.getStatusCode().value());
            } else {
                webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                webexResponse.setMessage(String.format("An unexpected error occurred retrieving the locations " +
                        "at the organization with id: %s", orgId));
            }
            return webexResponse;

            // NOTE: all possible exceptions are caught in this code for (1) debugging purposes and (2) to return
            // meaningful responses to client via ApiResponseWrapper.
        } catch (HttpClientErrorException e) { // These occur when the HTTP response status code is 4xx.
            // Examples:  400 Bad Request, 401 Unauthorized, 404 Not Found, 403 Forbidden
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("Webex API returned a 4xx error for retrieving locations: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
            // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Webex API returned a 5xx error for retrieving locations: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
            // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage("Error accessing Webex API when trying to retrieve locations: " + e.getMessage());
            return webexResponse;

        } catch (
                RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
            //and all other runtime exceptions within the RestTemplate.
            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error retrieving the organization's locations due to logical error in server program: " + e.getMessage());
            return webexResponse;
        }
    }

    /**
     * Creates a map of all the locations at an organization.
     *
     * @param accessToken the token used for authenticating the request
     * @param orgId to get the locations from
     * @return map of the locations at the specified organization where the keys are the location names and the values are location objects
     * else empty map if response from webex says there are no locations.
     */
    public Map<String, Location> getLocationsMap(String accessToken, String orgId) {
        Map<String, Location> locations = new HashMap<>();
        ApiResponseWrapper<ListLocationsResponse> listLocationsFromWebex = listLocations(accessToken, orgId);
        if (listLocationsFromWebex.is2xxSuccess() && listLocationsFromWebex.hasData()) {
            ListLocationsResponse listLocationsResponse = listLocationsFromWebex.getData();
            if (listLocationsResponse.hasLocations()) {
                for (Location loc : listLocationsResponse.getItems()) {
                    locations.put(loc.getName(), loc);
                }
            }
        }
        return locations;
    }

    /**
     * Calls Webex API to get a list of the floors at a certain location.
     *
     * @param accessToken the token used for authenticating the request
     * @param locationId location to get the floors from
     * @return ApiResponseWrapper with 'data' being the response from the API
     */
    public ApiResponseWrapper<ListFloorsResponse> listFloors(String accessToken, String locationId) {
        ApiResponseWrapper<ListFloorsResponse> webexResponse = new ApiResponseWrapper<>();

        WebClient webClient = WebClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            ListFloorsResponse listFloorsResponse = webClient.get()
                    .uri(String.format("%s/floors", locationId))
                    .retrieve()
                    .bodyToMono(ListFloorsResponse.class)
                    .block();

            webexResponse.setData(listFloorsResponse);
            webexResponse.setStatus(HttpStatus.OK.value());
            return webexResponse;

        } catch (WebClientRequestException e) { // Thrown when there are issues with the request itself (e.g., network issues, timeouts).
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return webexResponse;

        } catch (WebClientResponseException e) { // Thrown when the HTTP status code is 4xx or 5xx.
            webexResponse.setStatus(e.getStatusCode().value());
            webexResponse.setMessage(e.getMessage());
            return webexResponse;

        }  catch (Exception e) {
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("An unexpected error occurred. " + e.getMessage());
            return webexResponse;
        }
    }
}
