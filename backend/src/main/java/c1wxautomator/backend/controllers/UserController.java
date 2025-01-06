package c1wxautomator.backend.controllers;


// Author: Natalie Jungquist
//
// This controller handles user-related endpoints for the application.
// Key features include:
//      - Export users to Webex given a csv file as input.
//
// Dependencies:
//      - UserService to execute the operations.
//      - LocationService to get the possible locations a user can be at.
//      - LicenseService to get the possible license a user can be assigned.
//      - WxAuthorizationService to get the access token for the app to work and the id of the org to add users to.
//      - Spring Framework's MultipartFile for receiving file as input.
//      - CsvValidator class to validate that the proper csv file is sent in the request.
//
// Usage:
// Endpoint for client to export users.

import c1wxautomator.backend.dtos.licenses.License;
import c1wxautomator.backend.dtos.licenses.ListLicensesResponse;
import c1wxautomator.backend.dtos.locations.ListLocationsResponse;
import c1wxautomator.backend.dtos.locations.Location;
import c1wxautomator.backend.dtos.users.CustomExportUsersResponse;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import c1wxautomator.backend.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
public class UserController {

    private final UserService userService;
    private final LocationService locationService;
    private final LicenseService licenseService;
    private final WxAuthorizationService wxAuthorizationService;

    public UserController(final UserService userService, LocationService locationService, LicenseService licenseService, WxAuthorizationService wxAuthorizationService) {
        this.userService = userService;
        this.locationService = locationService;
        this.licenseService = licenseService;
        this.wxAuthorizationService = wxAuthorizationService;
    }

    /**
     * Endpoint for exporting users to Webex customer
     *
     * @param file with user information
     * @return response with success or failure messages
     */
    @PostMapping("/export-users")
    public ResponseEntity<?> exportUsersCsv(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is required and cannot be empty.");
        }
        if (!CsvValidator.isCsvFile(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The wrong type of file was provided. Must be a CSV file.");
        }
        Set<String> requiredCols = new HashSet<>(
                Set.of("First Name", "Display Name", "Status", "Email", "Extension", "Location", "Phone Number",
                        "Webex Contact Center Premium Agent", "Webex Contact Center Standard Agent", "Webex Calling - Professional")
        );
        if (!(CsvValidator.csvContainsRequiredCols(file, requiredCols))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File provided does not contain all the columns required to process the request.");
        }

        String accessToken = wxAuthorizationService.getAccessToken();
        String orgId = wxAuthorizationService.getAuthorizedOrgId();

        Map<String, License> licenses = new HashMap<>();
        ApiResponseWrapper listLicensesFromWebex = licenseService.listLicenses(accessToken, orgId);
        if (listLicensesFromWebex.is2xxSuccess() && listLicensesFromWebex.hasData()) {
            ListLicensesResponse listLicensesResponse = (ListLicensesResponse) listLicensesFromWebex.getData();
            if (listLicensesResponse.hasLicenses()) {
                licenses = licenseService.makeLicensesMap(listLicensesResponse.getItems());
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("The organization does not have any licenses.");
            }
        }

        Map<String, Location> locations = new HashMap<>();
        ApiResponseWrapper listLocationsFromWebex = locationService.listLocations(accessToken, orgId);
        if (listLocationsFromWebex.is2xxSuccess() && listLocationsFromWebex.hasData()) {
            ListLocationsResponse listLocationsResponse = (ListLocationsResponse) listLocationsFromWebex.getData();
            if (listLocationsResponse.hasLocations()) {
                 locations = locationService.makeLocationsMap(listLocationsResponse.getItems());
            } else {
               return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("The organization does not have any locations, so no users can be assigned Webex Calling - Professional licenses.");
            }
        }
        // TODO there must be a location ONLY in order to assign webex calling professional
        // TODO so probably this check should be done in the user service

        CustomExportUsersResponse customResponse = userService.exportUsers(file, accessToken, orgId, licenses, locations);

        return ResponseEntity.status(customResponse.getStatus()).body(customResponse);
    }
}