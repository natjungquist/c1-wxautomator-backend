package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for processing CSV files and mapping the data to application-specific objects.
// Key Features:
//      - Parses and validates user data from CSV files.
//      - Maps CSV records to UserRequest objects for further processing.
//      - Tracks user metadata such as licenses and locations.
//      - Ensures data integrity by validating file contents and logical consistency.
//
// Usage:
//      - Utilized by controllers or other services requiring CSV file processing for user data import.
//      - Facilitates the bulk creation of users, license assignments, and other related workflows.

import c1wxautomator.backend.dtos.licenses.License;
import c1wxautomator.backend.dtos.users.UserMetadata;
import c1wxautomator.backend.dtos.users.UserRequest;
import c1wxautomator.backend.exceptions.CsvProcessingException;
import c1wxautomator.backend.exceptions.LicenseNotAvailableException;
import c1wxautomator.backend.exceptions.LogicalProgrammingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CsvProcessor {
    /**
     * Reads user data from the CSV file and:
     * 1. maps the information to UserRequest objects.
     * 2. tracks licenses for each user in UserMetadata objects, populating usersMetadataMap.
     *
     * @param file the CSV file to read.
     * @return List of UserRequest objects created from the CSV file, else a custom exception class if there is an error.
     * @throws CsvProcessingException if there is an error processing the CSV file.
     * @throws LogicalProgrammingException if there is a logical error in the code.
     */
    public List<UserRequest> readUsersFromCsv(MultipartFile file, Map<String, UserMetadata> usersMetadataMap,Map<String, License> licenses)
            throws CsvProcessingException, LogicalProgrammingException, LicenseNotAvailableException {
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

                userRequest.setActive(record.get("Status").equalsIgnoreCase("active"));

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
                if (record.get("Webex Contact Center Premium Agent").equalsIgnoreCase("true")) {
                    if (licenses.get("Contact Center Premium Agent") == null) { // Handle if a license needs to be assigned but the org does not have that license
                        throw new LicenseNotAvailableException("Contact Center Premium Agent license is not available at this organization, so it cannot be assigned to any users.");
                    }
                    userMetadata.addLicense(licenses.get("Contact Center Premium Agent"));
                }
                if (record.get("Webex Contact Center Standard Agent").equalsIgnoreCase("true")) {
                    if (licenses.get("Contact Center Standard Agent") == null) {
                        throw new LicenseNotAvailableException("Contact Center Standard Agent license is not available at this organization, so it cannot be assigned to any users.");
                    }
                    userMetadata.addLicense(licenses.get("Contact center Standard Agent"));  // NOTE the Webex API spells them differently (yes, this is confusing)
                }
                if (record.get("Webex Calling - Professional").equalsIgnoreCase("true")) {
                    if (licenses.get("Webex Calling - Professional") == null) {
                        throw new LicenseNotAvailableException("Webex Calling - Professional license is not available at this organization, so it cannot be assigned to any users.");
                    }
                    userMetadata.addLicense(licenses.get("Webex Calling - Professional"));
                }

                userMetadata.setUserRequest(userRequest);
                usersMetadataMap.put(userRequest.getEmail(), userMetadata);
            }
        } catch (IOException e) {
            throw new CsvProcessingException("An error occurred processing the CSV file: " + e.getMessage());
        }
        // TODO also catch exceptions if there is undesirable data in the CSV file

        // The usersMetadataMap should hold all the same users as the userRequests list
        if (usersMetadataMap.size() != userRequests.size()) {
            throw new LogicalProgrammingException("Logical error: usersMetadataMap size does not match userRequests size.");
        } else {
            return userRequests;
        }
    }
}
