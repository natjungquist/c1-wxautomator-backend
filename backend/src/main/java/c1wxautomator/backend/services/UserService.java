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
//      - custom data transfer objects such as User, UserBulkRequest, UserBulkResponse, etc
//
// Usage:
// Used by any controller that needs to bulk export users to Webex API.

import c1wxautomator.backend.dtos.users.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;

@Service
public class UserService {
    private final WxAuthorizationService wxAuthorizationService;

    public UserService(WxAuthorizationService wxAuthorizationService) {
        this.wxAuthorizationService = wxAuthorizationService;
    }

    /**
     * Exports users from the uploaded CSV file and creates them via the Webex API.
     * Validates the CSV file format, checks for required columns, and processes the file to create users in bulk.
     *
     * @param file the file containing user data.
     * @return ResponseEntity containing the response status and data after processing the CSV.
     */
    public CustomExportUsersResponse exportUsers(MultipartFile file) {

        CustomExportUsersResponse response = new CustomExportUsersResponse();

        // Checks that the file is valid
        if (!CsvValidator.isCsvFile(file)) {
            // TODO
            return response;
        }

        // Required CSV columns
        Set<String> requiredCols = new HashSet<>(
                Set.of("First Name", "Display Name", "Status", "Email", "Location",
                        "Webex Contact Center Premium Agent", "Webex Contact Center Standard Agent", "Webex Calling - Professional")
        );
        if (!(CsvValidator.csvContainsRequiredCols(file, requiredCols))) {
            //response.put("message", "File provided does not contain all the columns required to process the request.");
            return response;
        }

        // NOTE that the csv file will contain the license to be granted to the user, but the User object will not
        // contain this license because the Webex APIs for create user and assign license are separate.
        // Instead, the license assignment is processed in a separate request.
        // NOTE that creating the user with the bulk API automatically sets all licenses to false.

        Map<String, List<String>> usernameToLicensesMap = new HashMap<>();  // to keep track of each user's licenses
        Map<String, String> bulkIdToUsernameMap = new HashMap<>();  // to keep track of each user and whether the export succeeds or fails

        // Step 1: Read users from CSV
        List<User> users = readUsersFromCsv(file, usernameToLicensesMap);

        // Step 2: Create BulkRequest and bulkId-to-username map
        UserBulkRequest bulkRequest = createBulkRequest(users, bulkIdToUsernameMap);

        // Step 3: Send BulkRequest to Webex
        UserBulkResponse webexResponse = sendBulkRequestToWebex(bulkRequest);
        if (webexResponse != null) {
            // TODO
        } else {
            // TODO
        }

        // Step 4: Process response and create a custom response for the frontend
        // ResponseEntity<?> customResponse = processWebexResponse(webexResponse, bulkIdToUsernameMap);

        return response;

        // Step 5: Assign licenses
    }

    /**
     * Reads user data from the CSV file and maps the information to User objects.
     * The method also tracks licenses for each user based on the CSV content.
     *
     * @param file the CSV file to read.
     * @param usernameToLicensesMap a map to associate users with their required licenses.
     * @return List of User objects created from the CSV file.
     */
    private List<User> readUsersFromCsv(MultipartFile file, Map<String, List<String>> usernameToLicensesMap) {
        List<User> users = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Reader reader = new InputStreamReader(inputStream)) {

            // Use CSVFormat.Builder to configure headers and skipping the header record
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setHeader() // Indicates the first row contains the header
                    .setSkipHeaderRecord(true) // Skip the header row in iteration
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(reader);

            for (CSVRecord record : records) {
                // Parse the rest of the records and set them to User objects
                User user = new User();
                user.setDisplayName(record.get("Display Name"));
                User.Name name = new User.Name();
                name.setGivenName(record.get("First Name"));
                name.setFamilyName(record.get("Last Name"));
                user.setName(name);
                user.setUserName(record.get("Email"));  // The email column of the csv file corresponds to the userName field for the request
                user.populateEmails();
                if (record.get("Status").equalsIgnoreCase("active")) {
                    user.setActive(true);
                } else {
                    user.setActive(false);
                }
                List<String> userSchemas = new ArrayList<>(List.of(
                        "urn:ietf:params:scim:schemas:core:2.0:User",
                        "urn:scim:schemas:extension:cisco:webexidentity:2.0:User",
                        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
                ));
                user.setSchemas(userSchemas);

                users.add(user);

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
                usernameToLicensesMap.put(record.get("Email"), licenses);
            }
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Replace with proper logging or error handling
        }

        return users;
    }

    /**
     * Creates a bulk request to Webex API for user creation using the provided user data.
     * The request contains a list of user operations, where each operation creates a new user.
     * The method also tracks bulk IDs for each user.
     *
     * @param users the list of User objects to be included in the bulk request.
     * @param bulkIdToUsernameMap a map to associate each bulk operation with a username.
     * @return A UserBulkRequest object representing the bulk creation request.
     */
    private UserBulkRequest createBulkRequest(List<User> users, Map<String, String> bulkIdToUsernameMap) {
        UserBulkRequest bulkRequest = new UserBulkRequest();
        List<String> bulkSchemas = new ArrayList<>(List.of(
                "urn:ietf:params:scim:api:messages:2.0:BulkRequest"
        )); // from Webex documentation
        bulkRequest.setSchemas(bulkSchemas);

        bulkRequest.setFailOnErrors(10);

        List<UserOperationRequest> operations = new ArrayList<>();

        int counter = 1;
        for (User user : users) {
            String bulkId = "user-" + counter++;
            bulkIdToUsernameMap.put(bulkId, user.getUserName());

            UserOperationRequest operation = new UserOperationRequest();
            operation.setMethod("POST");
            operation.setPath("/Users");
            operation.setBulkId(bulkId);
            operation.setData(user);

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
     * @return ResponseEntity containing the Webex API response with bulk user creation results.
     */
    private UserBulkResponse sendBulkRequestToWebex(UserBulkRequest bulkRequest) {
        String accessToken = wxAuthorizationService.getAccessToken();
        String orgId = wxAuthorizationService.getAuthorizedOrgId();
        String URL = String.format("https://webexapis.com/identity/scim/%s/v2/Bulk", orgId);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserBulkRequest> requestEntity = new HttpEntity<>(bulkRequest, headers);

        try {
            ResponseEntity<UserBulkResponse> response = restTemplate.exchange(
                    URL,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            // response: status:200 OK, headers:..., body:userbulkresponse:schemas...operations: [useroperationresponse]
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                // TODO extra error handling for specific status codes
                return null;
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.getBody());
            }

        } catch (HttpServerErrorException e) {
            System.out.println(e.getMessage());

            //Map<String, String> response = new HashMap<>();
            //response.put("message", "something went wrong calling the webex api");
            return null;
        }
    }

    /**
     * Processes the Webex API response after submitting the bulk user creation request.
     * Maps the Webex API response to a custom response for the frontend, providing relevant details
     * about the success or failure of each user operation.
     *
     * @param webexResponse the Webex API response containing the bulk user creation results.
     * @param bulkIdToUsernameMap a map to relate bulk operation IDs to usernames.
     * @return ResponseEntity containing the processed response for the frontend.
     */
//    private CustomExportUsersResponse processWebexResponse(ResponseEntity<UserBulkResponse> webexResponse, Map<String, String> bulkIdToUsernameMap) {
//        // TODO fix
//        CustomExportUsersResponse customResponse = new CustomExportUsersResponse();
//
//        if (webexResponse.getStatusCode() == HttpStatus.OK) {
//            UserBulkResponse bulkResponse = webexResponse.getBody();
//            List<UserOperationResponse> operations = bulkResponse.getOperations();  // Extract operations from the response
//
//            if (operations == null || operations.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No users were created.");  // TODO edit this
//            }
//        }
//        return ResponseEntity.status(webexResponse.getStatusCode()).body(webexResponse);// TODO this is temporarily here to get the code to compile
//    }
}
