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
import c1wxautomator.backend.dtos.locations.Location;
import c1wxautomator.backend.dtos.users.*;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import c1wxautomator.backend.exceptions.CsvProcessingException;
import c1wxautomator.backend.exceptions.LogicalProgrammingException;
import c1wxautomator.backend.exceptions.RequestCreationException;
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
     * @param file the file containing user data.
     * @param accessToken The token used for authenticating the request.
     * @param orgId id of the organization to export users to.
     * @param licenses map of licenses at this organization.
     * @param locations map of locations at this organization.
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

        // Step 1: Read users from CSV and populate usersMetadataMap with users
        List<UserRequest> userRequests;
        try {
            userRequests = csvProcessor.readUsersFromCsv(file, usersMetadataMap, licenses);
        } catch (LogicalProgrammingException | CsvProcessingException e) {
            response.setTotalCreateAttempts(0);
            response.setNumSuccessfullyCreated(0);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("An error occurred processing the CSV file: " + e.getMessage());
            return response;
        }

        if (userRequests.isEmpty()) {
            response.setTotalCreateAttempts(0);
            response.setNumSuccessfullyCreated(0);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("An error occurred processing the CSV file: no users found.");
        }

        // Step 2: Create BulkRequest and set bulkIds to usersMetadata
        UserBulkRequest bulkRequest;
        try {
            bulkRequest = createBulkRequest(userRequests, usersMetadataMap, bulkIdToEmailMap);
        } catch (RequestCreationException e) {
            response.setTotalCreateAttempts(0);
            response.setNumSuccessfullyCreated(0);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("An error occurred processing the data for exporting users: " + e.getMessage());
            return response;
        }

        // Step 3: Send BulkRequest to Webex
        ApiResponseWrapper webexResponse = send_ExportUsersBulkRequest_ToWebex(bulkRequest, accessToken, orgId);

        // if the call to the Webex API was not successful, send the error status and message back to client
        if (!webexResponse.is2xxSuccess()) {
            response.setTotalCreateAttempts(0);
            response.setNumSuccessfullyCreated(0);
            response.setStatus(webexResponse.getStatus());
            response.setMessage(webexResponse.getMessage());
            return response;
        }

        // Step 4: Process response about creating users


        // Step 5: Assign licenses
        List<UserMetadata> createdUsers = new ArrayList<>();

        if (webexResponse.is2xxSuccess() && webexResponse.hasData()) {
            UserBulkResponse userBulkResponse = (UserBulkResponse) webexResponse.getData();
            if (userBulkResponse.hasOperations()) {
                // TODO
                List<UserOperationResponse> operations = userBulkResponse.getOperations();  // Extract operations from the response
                for (UserOperationResponse operation : operations) {
                    // Using the bulk id from the response to get the email/username and other user data
                    String bulkId = operation.getBulkId();
                    String email = bulkIdToEmailMap.get(bulkId);
                    UserMetadata userMetadata = usersMetadataMap.get(email);
                    String firstName = userMetadata.getFirstName();
                    String lastName = userMetadata.getLastName();

                    switch (operation.getStatus()) {
                        case "201" -> {    // NOTE: Must hardcode the values as strings because that is how Webex API responds
                            response.addSuccess(201, email, firstName, lastName);
                            createdUsers.add(userMetadata);
                        }
                        case "200" ->
                                response.addFailure(200, email, firstName, lastName, "Webex API did not perform an operation for this user.");
                        case "400" -> {
                            String errorMessage = operation.getWebexErrorMessage();
                            response.addFailure(400, email, firstName, lastName, errorMessage);
                        }
                        case "409" -> {
                            String errorMessage = operation.getWebexErrorMessage();
                            response.addFailure(409, email, firstName, lastName, errorMessage);
                        }
                        case null, default -> {
                            String errorMessage = operation.getWebexErrorMessage();
                            response.addFailure(500, email, firstName, lastName, errorMessage);
                            // TODO
                        }
                    }
                }

                // Need to call the API to get the ids of the users at the organization. The ids are needed to assign licenses, but only accessible this way.
                ApiResponseWrapper searchUsersResponse = userGetter.searchUsers(accessToken, orgId);
                if (searchUsersResponse.is2xxSuccess() && searchUsersResponse.hasData()) {
                    SearchUsersResponse searchUsersData = (SearchUsersResponse) searchUsersResponse.getData();
//                    List<Object> allUsers = searchUsersData.getResources();
//                    for (Object user : allUsers) {
//                        String id = user.getId();
//                        String email = user.getEmail();  // TODO make sure this works cus the field is called userName on the webex side
//                        UserMetadata userMetadata = usersMetadataMap.get(email);
//                        userMetadata.setWebexId(id);
//                    }
                }

                for (UserMetadata createdUser : createdUsers) {
                    // TODO assign licenses
                    // TODO if license assignment succeeds:
                    // TODO else:
                    String id = createdUser.getWebexId();
                    String email = createdUser.getEmail();
                    String locationId = createdUser.getLocationId();
                }

            } else {
                response.setTotalCreateAttempts(0);
                response.setNumSuccessfullyCreated(0);
                response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
                response.setMessage("An error occurred: Webex did not perform any operations to create users.");
                return response;
            }
        } else {
            // TODO
        }

        // Step 6: Create custom response body to send to client

        response.setStatus(HttpStatus.OK.value());
        return response;
    }

    /**
     * Creates a bulk request to Webex API for user creation using the provided user data.
     * The request contains a list of user operations, where each operation creates a new user.
     * The method also tracks bulk IDs for each user.
     *
     * @param userRequests the list of UserRequest objects to be included in the bulk request.
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
     * @param orgId id of the organization to export users to.
     * @return custom ApiResponseWrapper object where 'status' is the status of the response from
     *      the call to the Webex API and 'data' is the UserBulkResponse data or null if there is an error.
     */
    private ApiResponseWrapper send_ExportUsersBulkRequest_ToWebex(UserBulkRequest bulkRequest, String accessToken, String orgId) {
        ApiResponseWrapper webexResponse = new ApiResponseWrapper();

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
            ResponseEntity<UserBulkResponse> response = restTemplate.exchange(URL, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {});
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

        } catch (RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
                                            //and all other runtime exceptions within the RestTemplate.
                                            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
                                            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error bulk exporting users with Webex API due to logical error in server program: " + e.getMessage());
            return webexResponse;
        }
    }

    // TODO
    private boolean isValidExtension(String extension) {
        return false;
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
