package c1wxautomator.backend.services;

// Author: Natalie Jungquist

import c1wxautomator.backend.dtos.customResponses.CustomExportUsersResponse;
import c1wxautomator.backend.dtos.licenses.License;
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
import java.util.concurrent.*;

/**
 *  Service class for managing user operations with Webex APIs, including user creation and license assignment.
 *  This class interacts with Webex APIs to create users in bulk, assign licenses, and handle CSV file uploads.
 *  It processes CSV files containing user data, validates required columns, and manages the bulk creation process.
 *  *
 *  Dependencies:
 *       - Spring Framework's MultipartFile for file handling.
 *       - Apache Commons CSV to parse CSVs
 *       - custom data transfer objects such as User, UserBulkRequest, UserBulkResponse, etc.
 *  *
 *  Usage:
 *  Used by any controller that needs to bulk export users to Webex API.
 */
@Service
public class UserService {

    private final LicenseService licenseService;
    private final UserGetter userGetter;
    private final CsvProcessor csvProcessor;

    /**
     * Constructor with dependency injection.
     *
     * @param licenseService to perform operations related to licensing.
     * @param userGetter to perform operations related to getting user info.
     * @param csvProcessor to perform operations on CSV file.
     */
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

        // These maps store extra information about the users. This info is separate because creating the user must be done first.
        // After the user is created, this info is need for further operations on the user.
        Map<String, UserMetadata> usersMetadataMap = new HashMap<>(); //key: username, value: userMetadata
        Map<String, String> bulkIdToEmailMap = new HashMap<>(); //key:bulkId, value: email/username

        // Step 1: Read users from CSV and populate users in usersMetadataMap with their userRequests, licenses, and locations
        List<UserRequest> userRequests = readUsersFromCsv(file, licenses, locations, response, usersMetadataMap);
        if (userRequests == null) return response;

        // Step 2: Create BulkRequest
        UserBulkRequest bulkRequest = createUserBulkRequest(userRequests, usersMetadataMap, bulkIdToEmailMap, response);
        if (bulkRequest == null) return response;

        // Step 3: Send BulkRequest to Webex
        ApiResponseWrapper<UserBulkResponse> webexResponse = sendBulkRequest(bulkRequest, accessToken, orgId, response);
        if (webexResponse == null) return response;

        // Step 4: Process response about creating users
        List<UserMetadata> createdUsers = processUserCreationResponse(webexResponse, usersMetadataMap, bulkIdToEmailMap, response);
        if (createdUsers.isEmpty()) return response;

        // Step 5: Assign licenses with delay
        // Run async because we have to wait for the delay to and do more processing before returning a response to the client.
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                        assignLicensesWithDelay(response, createdUsers, accessToken, orgId, usersMetadataMap))
                .exceptionally(ex -> {
                    response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error during async process: " + ex.getMessage());
                    return null;
                });
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error waiting for completion: " + e.getMessage());
        }

        response.setStatus(HttpStatus.OK.value());
        return response;
    }

    /**
     * Reads users from the CSV file and populates the metadata map.
     *
     * @param file             The CSV file containing user data.
     * @param licenses         The map of licenses at this organization.
     * @param locations        The map of locations at this organization.
     * @param response         The response object to update in case of errors.
     * @param usersMetadataMap The map to store user metadata.
     * @return The list of UserRequest objects or null if there were errors.
     */

    private List<UserRequest> readUsersFromCsv(MultipartFile file, Map<String, License> licenses, Map<String, Location> locations, CustomExportUsersResponse response, Map<String, UserMetadata> usersMetadataMap) {
        try {
            return csvProcessor.readUsersFromCsv(file, usersMetadataMap, licenses, locations);
        } catch (LogicalProgrammingException | CsvProcessingException e) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred processing the CSV file: " + e.getMessage());
        } catch (LicenseNotAvailableException | LocationNotAvailableException e) {
            response.setError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
        return null;
    }

    /**
     * Creates a UserBulkRequest for sending to Webex.
     *
     * @param userRequests     The list of user requests.
     * @param usersMetadataMap The map of user metadata.
     * @param bulkIdToEmailMap The map to link bulk IDs to email addresses.
     * @param response         The response object to update in case of errors.
     * @return The UserBulkRequest object or null if there were errors.
     */
    private UserBulkRequest createUserBulkRequest(List<UserRequest> userRequests, Map<String, UserMetadata> usersMetadataMap, Map<String, String> bulkIdToEmailMap, CustomExportUsersResponse response) {
        if (userRequests.isEmpty()) {
            response.setError(HttpStatus.BAD_REQUEST.value(), "An error occurred processing the CSV file: no users found.");
            return null;
        }

        try {
            return createBulkRequest(userRequests, usersMetadataMap, bulkIdToEmailMap);
        } catch (RequestCreationException e) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred processing the data for exporting users: " + e.getMessage());
            return null;
        }
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
            throw new RequestCreationException("Error occurred assembling user data: no users provided.");
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

        return bulkRequest;
    }

    /**
     * Sends the bulk request to Webex and processes the response.
     *
     * @param bulkRequest The bulk request to be sent.
     * @param accessToken The token used for authenticating the request.
     * @param orgId       The ID of the organization.
     * @param response    The response object to update in case of errors.
     * @return The Webex API response or null if there were errors.
     */
    private ApiResponseWrapper<UserBulkResponse> sendBulkRequest(UserBulkRequest bulkRequest, String accessToken, String orgId, CustomExportUsersResponse response) {
        ApiResponseWrapper<UserBulkResponse> webexResponse = send_ExportUsersBulkRequest_ToWebex(bulkRequest, accessToken, orgId);

        // if the call to the Webex API was not successful, send the error status and message back to client
        if (!webexResponse.is2xxSuccess()) {
            response.setError(webexResponse.getStatus(), webexResponse.getMessage());
            return null;
        }
        if (!webexResponse.hasData()) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error occurred exporting users: server did not track Webex API response.");
            return null;
        }
        return webexResponse;
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
    private ApiResponseWrapper<UserBulkResponse> send_ExportUsersBulkRequest_ToWebex(UserBulkRequest bulkRequest, String accessToken, String orgId) {
        ApiResponseWrapper<UserBulkResponse> webexResponse = new ApiResponseWrapper<>();

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
            webexResponse.setMessage("4xx error");
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

    /**
     * Processes the response from Webex about user creation.
     *
     * @param webexResponse    The Webex API response.
     * @param usersMetadataMap The map of user metadata.
     * @param bulkIdToEmailMap The map linking bulk IDs to email addresses.
     * @param response         The response object to update with success or failure.
     * @return The list of created UserMetadata objects.
     */
    private List<UserMetadata> processUserCreationResponse(ApiResponseWrapper<UserBulkResponse> webexResponse, Map<String, UserMetadata> usersMetadataMap, Map<String, String> bulkIdToEmailMap, CustomExportUsersResponse response) {
        UserBulkResponse userBulkResponse = webexResponse.getData();
        if (!userBulkResponse.hasOperations()) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error: Webex attempted to create users but none succeeded.");
            return new ArrayList<>();
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
            } else {
                handleOperationFailure(response, operation, email, firstName, lastName);
            }
        }

        if (createdUsers.isEmpty()) {  // && response.getNumSuccessfullyCreated() == createdUsers.size()
            response.setError(HttpStatus.OK.value(), "No users were created.");
        }

        return createdUsers;
    }

    /**
     * Handles the failure cases in the user creation operation.
     *
     * @param response  The response object to update with failure details.
     * @param operation The user operation response from Webex.
     * @param email     The email address of the user.
     * @param firstName The first name of the user.
     * @param lastName  The last name of the user.
     */
    private void handleOperationFailure(CustomExportUsersResponse response, UserOperationResponse operation, String email, String firstName, String lastName) {
        if (operation.getStatus().equals("200")) {
            response.addFailure(200, email, firstName, lastName, "Webex API returned 200 instead of 201 and did not create this user.");
        } else if (operation.getStatus().equals("409")) {
            String errorMessage = String.format("Webex API responded with '%s' because a user with this email already exists.", operation.getWebexErrorMessage());
            response.addFailure(Integer.parseInt(operation.getStatus()), email, firstName, lastName, errorMessage);
        } else {
            response.addFailure(Integer.parseInt(operation.getStatus()), email, firstName, lastName, operation.getWebexErrorMessage());
        }
    }

    /**
     * Assigns licenses to users after a delay to allow Webex API processing.
     *
     * @param response         The response object to update in case of errors.
     * @param createdUsers     The list of created UserMetadata objects.
     * @param accessToken      The token used for authenticating the request.
     * @param orgId            The ID of the organization.
     * @param usersMetadataMap The map of user metadata.
     */
    private void assignLicensesWithDelay(CustomExportUsersResponse response, List<UserMetadata> createdUsers, String accessToken, String orgId, Map<String, UserMetadata> usersMetadataMap) {
        // Using a scheduler to delay the call because the Webex API needs time to process the newly created users.

        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            scheduler.schedule(() -> {
                // 5a. First, need to call the Webex API to get the ids of the users at the organization. The ids are needed to assign licenses, but only accessible this way.
                SearchUsersResponse searchUsersResponse = canGetUserIds(response, accessToken, orgId);
                if (searchUsersResponse != null) {
                    // 5b. Save the userIds into the usersMetadataMap
                    saveUserIds(searchUsersResponse, usersMetadataMap);
                }
            }, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            response.setError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error scheduling the delay: " + e.getMessage());
        }

        // 5c. After getting the userIds, assign the licenses
        licenseService.assignLicensesAndUpdateResponse(response, createdUsers, accessToken, orgId);
    }

    /**
     * Retrieves user IDs from Webex API for the newly created users.
     *
     * @param response    The response object to update in case of errors.
     * @param accessToken The token used for authenticating the request.
     * @param orgId       The ID of the organization.
     * @return The SearchUsersResponse object or null if there were errors.
     */
    private SearchUsersResponse canGetUserIds(CustomExportUsersResponse response, String accessToken, String orgId) {
        ApiResponseWrapper<SearchUsersResponse> searchUsersResponse = userGetter.searchUsers(accessToken, orgId);
        if (searchUsersResponse.is2xxSuccess() && searchUsersResponse.hasData()) {
            response.setStatus(HttpStatus.OK.value());
            return searchUsersResponse.getData();
        }
        response.setMessage("Error getting any user IDs. No licenses were assigned.");
        return null;
    }

    /**
     * Saves the user IDs into the user metadata map.
     *
     * @param searchUsersResponse The response from the Webex API with user IDs.
     * @param usersMetadataMap    The map to store user metadata.
     */
    private static void saveUserIds(SearchUsersResponse searchUsersResponse, Map<String, UserMetadata> usersMetadataMap) {
        List<SearchUsersResponse.Resource> allUsers = searchUsersResponse.getResources();
        if (allUsers != null) {
            for (SearchUsersResponse.Resource user : allUsers) {
                String email = user.getUserName();
                UserMetadata userMetadata = usersMetadataMap.get(email);
                if (userMetadata == null) { // If the user is not in usersMetadataMap, it was not just now created, so it can be discarded.
                    continue;
                }
                String id = user.getId();
                userMetadata.setPersonId(id);  // sets the personId for the userMetadata objects in userMetadataMap and createdUsers list
            }
        }
    }
}
