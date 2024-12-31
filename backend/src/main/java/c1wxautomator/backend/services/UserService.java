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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;

@Service
public class UserService {

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

        // Checks that the file is valid
        if (!CsvValidator.isCsvFile(file)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("The wrong type of file was provided. Must be a CSV file.");
            return response;
        }

        // Required CSV columns
        Set<String> requiredCols = new HashSet<>(
                Set.of("First Name", "Display Name", "Status", "Email", "Extension", "Location", "Phone Number",
                        "Webex Contact Center Premium Agent", "Webex Contact Center Standard Agent", "Webex Calling - Professional")
        );
        if (!(CsvValidator.csvContainsRequiredCols(file, requiredCols))) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("File provided does not contain all the columns required to process the request.");
            return response;
        }

        // NOTE that the csv file will contain the license to be granted to the user, but the User object will not
        // contain this license because the Webex APIs for create user and assign license are separate.
        // Instead, the license assignment is processed in a separate request.
        // NOTE that creating the user with the bulk API automatically sets all licenses to false.

        // These maps store extra information about the users. This info is separate because creating the user must be done first.
        // After the user is created, this info is need for further operations on the user.
        Map<String, UserMetadata> usersMetadataMap = new HashMap<>();

        // Step 1: Read users from CSV
        List<UserRequest> userRequests = readUsersFromCsv(file, usersMetadataMap);

        // Step 2: Create BulkRequest and bulkId-to-username map
        UserBulkRequest bulkRequest = createBulkRequest(userRequests, usersMetadataMap);

        // Step 3: Send BulkRequest to Webex
        ApiResponseWrapper webexResponse = send_ExportUsersBulkRequest_ToWebex(bulkRequest, accessToken, orgId);

        // if the call to the Webex API was not successful, send the error status and message back to client
        if (!webexResponse.is2xxSuccess()) {
            response.setStatus(webexResponse.getStatus());
            response.setMessage(webexResponse.getMessage());
            return response;
        }

        // Step 4: Process response about creating users


        // Step 5: Assign licenses


        // Step 6: Create custom response body to send to client

        response.setStatus(HttpStatus.OK.value());
        return response;
    }

    /**
     * Reads user data from the CSV file and maps the information to UserRequest objects.
     * The method also tracks licenses for each user based on the CSV content.
     *
     * @param file the CSV file to read.
     * @return List of UserRequest objects created from the CSV file.
     */
    private List<UserRequest> readUsersFromCsv(MultipartFile file, Map<String, UserMetadata> usersMetadataMap) {
        List<UserRequest> userRequests = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Reader reader = new InputStreamReader(inputStream)) {

            // Use CSVFormat.Builder to configure headers and skipping the header record
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setHeader() // Indicates the first row contains the header
                    .setSkipHeaderRecord(true) // Skip the header row in iteration
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(reader);

            for (CSVRecord record : records) {
                // Parse the rest of the records and set them to UserRequest objects
                UserRequest userRequest = new UserRequest();
                UserMetadata userMetadata = new UserMetadata();

                userRequest.setDisplayName(record.get("Display Name"));

                UserRequest.Name name = new UserRequest.Name();
                name.setGivenName(record.get("First Name"));
                name.setFamilyName(record.get("Last Name"));
                userRequest.setName(name);

                userRequest.setEmail(record.get("Email"));  // The email column of the csv file corresponds to the userName field for the request

                if (record.get("Status").equalsIgnoreCase("active")) {
                    userRequest.setActive(true);
                } else {
                    userRequest.setActive(false);
                }

                List<String> userSchemas = new ArrayList<>(List.of(
                        "urn:ietf:params:scim:schemas:core:2.0:User",
                        "urn:scim:schemas:extension:cisco:webexidentity:2.0:User",
                        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
                ));
                userRequest.setSchemas(userSchemas);

                if (record.get("Extension") != null) {
                  // TODO MAKE SURE THE EXTENSION IS A NUMBER AND
                  // TODO make sure the extension does not already exist
                    userRequest.addPrimaryExtension((record.get("Extension")));
                }

                // TODO set non-extension phone numbers -
                //  NOTE: must already be configured as a phone number at this location in order for this to work

                // TODO set usermetadata location... and check if it's (1) a valid location and (2) if it matches the phone number

                userRequests.add(userRequest);

                // Keep track of the licenses that users might need to be granted
                List<String> licenses = new ArrayList<>();
                if (record.get("Webex Contact Center Premium Agent").equalsIgnoreCase("true")) {
                    licenses.add("Webex Contact Center Premium Agent");
                }
                if (record.get("Webex Contact Center Standard Agent").equalsIgnoreCase("true")) {
                    licenses.add("Webex Contact Center Standard Agent");
                }
                if (record.get("Webex Calling - Professional").equalsIgnoreCase("true")) {
                    licenses.add("Webex Calling - Professional");
                }
//                usernameToLicensesMap.put(record.get("Email"), licenses);
                // TODO licenses here

                userMetadata.setUserRequest(userRequest);
                usersMetadataMap.put(userRequest.getEmail(), userMetadata);
            }
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Replace with proper logging or error handling
        }

        return userRequests;
    }

    /**
     * Creates a bulk request to Webex API for user creation using the provided user data.
     * The request contains a list of user operations, where each operation creates a new user.
     * The method also tracks bulk IDs for each user.
     *
     * @param userRequests the list of UserRequest objects to be included in the bulk request.
     * @param usersMetadataMap a map to associate each username to metadata about them.
     * @return A UserBulkRequest object representing the bulk creation request.
     */
    private UserBulkRequest createBulkRequest(List<UserRequest> userRequests, Map<String, UserMetadata> usersMetadataMap) {
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
            // TODO make sure this does pass by val correctly

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
            ResponseEntity<UserBulkResponse> response = restTemplate.exchange(URL, HttpMethod.POST,
                    requestEntity, new ParameterizedTypeReference<>() {});
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
            System.out.println("HttpClientErrorException: " + e.getMessage());
            webexResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            webexResponse.setMessage("Webex API returned a 4xx error for bulk exporting users.");
            return webexResponse;

        } catch (HttpServerErrorException e) { // These occur when the HTTP response status code is 5xx.
                                                // Examples: 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable
            System.out.println("HttpServerErrorException: " + e.getMessage());
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Webex API returned a 5xx error for bulk exporting users.");
            return webexResponse;

        } catch (ResourceAccessException e) { // These occur when there are problems with the network or the server.
                                                // Examples: DNS resolution failures, Connection timeouts, SSL handshake failures
            System.out.println("ResourceAccessException: " + e.getMessage());
            webexResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            webexResponse.setMessage("Error accessing Webex API when trying to bulk export users.");
            return webexResponse;

        } catch (RestClientException e) { // These occur when the response body cannot be converted to the desired object type.
                                            //and all other runtime exceptions within the RestTemplate.
                                            // Examples: Mismatched response structure, Parsing errors, Incorrect use of
                                            // ParameterizedTypeReference, Invalid request or URL, Method not allowed
            System.out.println("RestClientException: " + e.getMessage());
            webexResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webexResponse.setMessage("Error bulk exporting users with Webex API due to logical error in server program.");
            return webexResponse;
        }
    }

    private SearchUsersResponse searchUsers(String accessToken, String orgId) {
        return null;
    }

    private boolean isValidExtension(String extension) {
        return false;
    }

//    /**
//     * Processes the Webex API response after submitting the bulk user creation request.
//     * Maps the Webex API response to a custom response for the frontend, providing relevant details
//     * about the success or failure of each user operation.
//     *
//     * @param webexResponse the Webex API response body containing the bulk user creation results.
//     * @param bulkIdToUsernameMap a map to relate bulk operation IDs to usernames.
//     * @return CustomExportUsersResponse containing the processed response details.
//     */
//    private CustomExportUsersResponse processWebexResponse(UserBulkResponse webexResponse, Map<String, String> bulkIdToUsernameMap) {
//
//            List<UserOperationResponse> operations = bulkResponse.getOperations();  // Extract operations from the response
//
//            if (operations == null || operations.isEmpty()) {
//            // no operations were performed
//            }
//
//        return
//    }
}
