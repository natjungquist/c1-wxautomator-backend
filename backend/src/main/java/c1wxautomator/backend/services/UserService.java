package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for managing user operations with Webex APIs, including user creation and license assignment.
// This class interacts with Webex APIs to create users in bulk, assign licenses, and handle CSV file uploads.
// It processes CSV files containing user data, validates required columns, and manages the bulk creation process.

import c1wxautomator.backend.dtos.users.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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
    public ResponseEntity<?> exportUsers(MultipartFile file) {
        // Checks that the file is valid
        if (!CsvValidator.isCsvFile(file)) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "File provided is not a CSV file.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Required CSV columns
//        Set<String> requiredCols = new HashSet<>(
//                Set.of("First Name", "Display Name", "Status", "Email", "Location",
//                        "Webex Contact Center Premium Agent", "Webex Contact Center Standard Agent", "Webex Calling - Professional")
//        );
//        if (!(CsvValidator.csvContainsRequiredCols(file, requiredCols))) {
//            Map<String, String> response = new HashMap<>();
//            response.put("message", "File provided does not contain all of the columns required to process the request.");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//        }

        // NOTE that the csv file will contain the license to be granted to the user, but the User object will not
        // contain this license because the Webex APIs for create user and assign license are separate.
        // Instead, the license assignment is processed in a separate request.
        // NOTE that creating the user with the bulk API automatically sets all licenses to false.

        // TODO determine the response entity type
        //ResponseEntity<?> response = processCsvAndCreateUsers(file);

        Map<String, String> response = new HashMap<>();
        response.put("message", "dummy");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
