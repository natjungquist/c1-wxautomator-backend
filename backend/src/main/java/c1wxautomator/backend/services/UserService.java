package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for managing user operations with Webex APIs, including user creation and license assignment.
// This class interacts with Webex APIs to create users in bulk, assign licenses, and handle CSV file uploads.
// It processes CSV files containing user data, validates required columns, and manages the bulk creation process.
//
// Dependencies:
//      - Spring Framework's MultipartFile for file handling.
//      - Apache Commons CSV to parse CSVs
//      - custom data transfer objects such as User, UserBulkRequest, UserBulkResponse, etc.
//
// Usage:
// Used by any controller that needs to bulk export users to Webex API.

import c1wxautomator.backend.dtos.licenses.License;
import c1wxautomator.backend.dtos.licenses.AssignLicenseRequest;
import c1wxautomator.backend.dtos.locations.Location;
import c1wxautomator.backend.dtos.users.*;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import c1wxautomator.backend.exceptions.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class UserService {

    private final LicenseService licenseService;
    private final UserGetter userGetter;
    private final CsvProcessor csvProcessor;

    public UserService(LicenseService licenseService, UserGetter userGetter, CsvProcessor csvProcessor) {
        this.licenseService = licenseService;
        this.userGetter = userGetter;
        this.csvProcessor = csvProcessor;
    }

    /**
     * Exports users from the uploaded CSV file and creates them via the Webex API.
     * Validates the CSV file format, checks for required columns, and processes the file to create users in bulk.
     *
     * @param file        the file containing user data.
     * @param accessToken The token used for authenticating the request.
     * @param orgId       id of the organization to export users to.
     * @param licenses    map of licenses at this organization.
     * @param locations   map of locations at this organization.
     * @return CustomExportUsersResponse that represents the status and body of the response from this server to the client.
     */
    public CustomExportUsersResponse exportUsers(MultipartFile file, String accessToken, String orgId,
                                                 Map<String, License> licenses, Map<String, Location> locations) {

        CustomExportUsersResponse response = new CustomExportUsersResponse();

        // NOTE that the csv file will contain the license to be granted to the user, but the UserRequest object will not
        // contain this license because the Webex APIs for create user and assign license are separate.
        // Instead, the license assignment is processed in a separate request.
        // NOTE that creating the user with the bulk API automatically sets all licenses to false.

        // These maps store extra information about the users. This info is separate because creating the user must be done first.
        // After the user is created, this info is need for further operations on the user.
        Map<String, UserMetadata> usersMetadataMap = new HashMap<>(); //key: username, value: usermetadata
        Map<String, String> bulkIdToEmailMap = new HashMap<>(); //key:bulkId, value: email/username

        // Step 1: Read users from CSV and populate users in usersMetadataMap with their userRequests, licenses, and locations
        List<UserRequest> userRequests;
        try {
            userRequests = csvProcessor.readUsersFromCsv(file, usersMetadataMap, licenses, locations);
        } catch (LogicalProgrammingException | CsvProcessingException e) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred processing the CSV file: " + e.getMessage());
            return response;
        } catch (LicenseNotAvailableException | LocationNotAvailableException e) {
            response.setError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
            return response;
        }

        if (userRequests.isEmpty()) {
            response.setError(HttpStatus.BAD_REQUEST.value(), "An error occurred processing the CSV file: no users found.");
            return response;
        }

        // Step 2: Create BulkRequest and set bulkIds to usersMetadata
        UserBulkRequest bulkRequest;
        try {
            bulkRequest = createBulkRequest(userRequests, usersMetadataMap, bulkIdToEmailMap);
        } catch (RequestCreationException e) {
            response.setError(HttpStatus.BAD_REQUEST.value(), "An error occurred processing the data for exporting users: " + e.getMessage());
            return response;
        }

        // Step 3: Send BulkRequest to Webex
        ApiResponseWrapper webexResponse = send_ExportUsersBulkRequest_ToWebex(bulkRequest, accessToken, orgId);

        // ------------- TODO refactor into helper methods starting here -------------

        // if the call to the Webex API was not successful, send the error status and message back to client
        if (!webexResponse.is2xxSuccess()) {
            response.setError(webexResponse.getStatus(), webexResponse.getMessage());
            return response;
        }
        if (!webexResponse.hasData()) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error occurred exporting users: server did not track Webex API response.");
        }

        // Step 4: Process response about creating users
        UserBulkResponse userBulkResponse = (UserBulkResponse) webexResponse.getData();
        if (!userBulkResponse.hasOperations()) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error: Webex did not perform any operations to create users.");
            return response;
        }

        List<UserMetadata> createdUsers = new ArrayList<>();

        List<UserOperationResponse> operations = userBulkResponse.getOperations();  // Extract operations from the response
        for (UserOperationResponse operation : operations) {
            // Using the bulk id from the response to get the email/username and other user data
            String bulkId = operation.getBulkId();
            String email = bulkIdToEmailMap.get(bulkId);
            UserMetadata userMetadata = usersMetadataMap.get(email);
            String firstName = userMetadata.getFirstName();
            String lastName = userMetadata.getLastName();

            if (operation.getStatus().equals("201")) { // NOTE: Must hardcode the values as strings because that is how Webex API responds
                response.addSuccess(201, email, firstName, lastName);
                createdUsers.add(userMetadata);
            } else if (operation.getStatus().equals("200")) {
                response.addFailure(200, email, firstName, lastName, "Webex API returned 200 but did not create this user. Does it already exist?");
            } else {
                String errorMessage = String.format("Webex API responded with '%s' because a user with this email already exists.", operation.getWebexErrorMessage());
                response.addFailure(Integer.parseInt(operation.getStatus()), email, firstName, lastName, errorMessage);
            }
        }

        if (createdUsers.isEmpty()) {  // && response.getNumSuccessfullyCreated() == createdUsers.size()
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Attempted to create users but none succeeded.");
            return response;
        }

        // Step 5: Assign licenses
        // TODO - problem: sometimes searchusers does not get the newly created users???
        // 5a. First, need to call the Webex API to get the ids of the users at the organization. The ids are needed to assign licenses, but only accessible this way.
        ApiResponseWrapper searchUsersResponse = userGetter.searchUsers(accessToken, orgId);
        if (searchUsersResponse.is2xxSuccess() && searchUsersResponse.hasData()) {
            SearchUsersResponse searchUsersData = (SearchUsersResponse) searchUsersResponse.getData();
            List<SearchUsersResponse.Resource> allUsers = searchUsersData.getResources();
            if (allUsers != null) {
                for (SearchUsersResponse.Resource user : allUsers) {
                    String email = user.getUserName();
                    UserMetadata userMetadata = usersMetadataMap.get(email);
                    if (userMetadata == null) { // If the user is not in usersMetadataMap, it was not just now created, so it can be discarded.
                        continue;
                    }
                    String id = user.getId();
                    userMetadata.setPersonId(id);
                }
            }
        } else {
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Error getting any user IDs. No licenses were assigned.");
            return response;
        }

        for (UserMetadata createdUser : createdUsers) {
            String id = createdUser.getPersonId();
            String email = createdUser.getEmail();
            String locationId = createdUser.getLocationId();
            String extension = createdUser.getExtension();

            List<License> licensesToAdd = createdUser.getLicenses();
            if (licensesToAdd != null) {
                for (License license : licensesToAdd) {
                    AssignLicenseRequest licenseRequest = null;
                    if (license.getName().equals("Webex Calling - Professional")) {
                        try {
                            licenseRequest = licenseService.createCalling_Professional_AssignmentRequest(orgId, license, email, id, locationId, extension);
                        } catch (RequestCreationException e) {
                            String message = "An unexpected error occurred assigning license: " + e.getMessage();
                            response.addLicenseFailure(email, license.getName(), message, 500);
                        }
                    } else {
                        try {
                            licenseRequest = licenseService.createCC_AssignmentRequest(orgId, license, email, id);
                        } catch (RequestCreationException e) {
                            String message = "An unexpected error occurred assigning license: " + e.getMessage();
                            response.addLicenseFailure(email, license.getName(), message, 500);
                        }
                    }

                    ApiResponseWrapper licenseResponse = licenseService.sendLicenseRequest(accessToken, licenseRequest);
                    if (licenseResponse.is2xxSuccess() && licenseResponse.hasData()) {
//                        AssignLicenseResponse licenseResponseData = (AssignLicenseResponse) licenseResponse.getData();
                        response.addLicenseSuccess(email, license.getName());
                    } else {
                        int status = licenseResponse.getStatus();
                        String message = licenseResponse.getMessage();
                        response.addLicenseFailure(email, license.getName(), message, status);
                    }
                }
            }
        }

        response.setStatus(HttpStatus.OK.value());
        return response;
    }

    /**
     * Creates a bulk request to Webex API for user creation using the provided user data.
     * The request contains a list of user operations, where each operation creates a new user.
     * The method also tracks bulk IDs for each user.
     *
     * @param userRequests     the list of UserRequest objects to be included in the bulk request.
     * @param usersMetadataMap a map to associate each username to metadata about them.
     * @return A UserBulkRequest object representing the bulk creation request.
     * @throws RequestCreationException if there are no users to in the bulk request.
     */
    private UserBulkRequest createBulkRequest(List<UserRequest> userRequests, Map<String, UserMetadata> usersMetadataMap,
                                              Map<String, String> bulkIdToEmailMap) throws RequestCreationException {
        if (userRequests == null || userRequests.isEmpty()) {
            throw new RequestCreationException("Error occurred assembling user data: no users found.");
        }

        UserBulkRequest bulkRequest = new UserBulkRequest();
        List<String> bulkSchemas = new ArrayList<>(List.of(
                "urn:ietf:params:scim:api:messages:2.0:BulkRequest"
        )); // from Webex documentation
        bulkRequest.setSchemas(bulkSchemas);

        bulkRequest.setFailOnErrors(10);

        List<UserOperationRequest> operations = new ArrayList<>();

        int counter = 1;
        for (UserRequest userRequest : userRequests) {
            String bulkId = "user-" + counter++;
            UserMetadata currentUserMetadata = usersMetadataMap.get(userRequest.getEmail());
            currentUserMetadata.setBulkId(bulkId);
            bulkIdToEmailMap.put(bulkId, userRequest.getEmail());

            UserOperationRequest operation = new UserOperationRequest();
            operation.setMethod("POST");
            operation.setPath("/Users");
            operation.setBulkId(bulkId);
            operation.setData(userRequest);

            operations.add(operation);
        }
        bulkRequest.setOperations(operations);

        if (bulkRequest.getOperations().isEmpty()) {
            throw new RequestCreationException("Error occurred assembling user data: no operations found.");
        } else {
            return bulkRequest;
        }
    }

    /**
     * Sends the bulk user creation request to the Webex API and returns the response.
     * Uses the RestTemplate to make the API call and handles authorization with an OAuth2 token.
     *
     * @param bulkRequest the bulk request containing user data.
     * @param accessToken The token used for authenticating the request.
     * @param orgId       id of the organization to export users to.
     * @return custom ApiResponseWrapper object where 'status' is the status of the response from
     * the call to the Webex API and 'data' is the UserBulkResponse data or null if there is an error.
     */
    private ApiResponseWrapper send_ExportUsersBulkRequest_ToWebex(UserBulkRequest bulkRequest, String accessToken, String orgId) {
        ApiResponseWrapper webexResponse = new ApiResponseWrapper();

        if (bulkRequest == null) {
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("User bulk request is null.");
            return webexResponse;
        }

        String URL = String.format("https://webexapis.com/identity/scim/%s/v2/Bulk", orgId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserBulkRequest> requestEntity = new HttpEntity<>(bulkRequest, headers);

        RestTemplate restTemplate = new RestTemplate();

        // Per the Webex documentation, possible responses are 2xx, 4xx, or 5xx.
        // They will be handled and interpreted here.
        // Java throws exceptions for 4xx and 5xx status codes, so this must be in a try-catch block.
        try {
            ResponseEntity<UserBulkResponse> response = restTemplate.exchange(URL, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
            });
            if (response.getStatusCode().is2xxSuccessful()) {
                UserBulkResponse userBulkResponse = response.getBody();
                webexResponse.setData(userBulkResponse);
                webexResponse.setStatus(response.getStatusCode().value());
            } else {
                webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                webexResponse.setMessage("An unexpected error occurred exporting users.");
            }
            return webexResponse;

            // NOTE: all possible exceptions are caught in this code for (1) debugging purposes and (2) to return
            // meaningful responses to client via ApiResponseWrapper.
        } catch (HttpClientErrorException e) { // These occur when the HTTP response status code is 4xx.
            // Examples:  400 Bad Request, 401 Unauthorized, 404 Not Found, 403 Forbidden
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("Webex API returned a 4xx error for bulk exporting users: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
            // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Webex API returned a 5xx error for bulk exporting users: " + e.getResponseBodyAsString());
            return webexResponse;

        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
            // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage("Error accessing Webex API when trying to bulk export users: " + e.getMessage());
            return webexResponse;

        } catch (
                RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
            //and all other runtime exceptions within the RestTemplate.
            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error bulk exporting users with Webex API due to logical error in server program: " + e.getMessage());
            return webexResponse;
        }
    }

//    /**
//     * Processes the Webex API response after submitting the bulk user creation request.
//     * Maps the Webex API response to a custom response for the frontend, providing relevant details
//     * about the success or failure of each user operation.
//     *
//     * @param webexResponse the Webex API response body containing the bulk user creation results.
//     * @param bulkIdToEmailMap a map to relate bulk operation IDs to usernames.
//     * @return CustomExportUsersResponse containing the processed response details.
//     */
//    private CustomExportUsersResponse processWebexResponse(UserBulkResponse webexResponse, Map<String, String> bulkIdToEmailMap) {
//        return
//    }
}
