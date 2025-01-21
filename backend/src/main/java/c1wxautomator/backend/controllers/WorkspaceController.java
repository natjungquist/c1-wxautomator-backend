package c1wxautomator.backend.controllers;

import c1wxautomator.backend.dtos.customResponses.CustomExportResponse;
import c1wxautomator.backend.dtos.licenses.License;
import c1wxautomator.backend.dtos.licenses.ListLicensesResponse;
import c1wxautomator.backend.dtos.locations.Location;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import c1wxautomator.backend.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  This controller handles workspace-related endpoints for the application.
 *  Key features include:
 *       - Export workspaces to Webex given a csv file as input.
 * //
 *  Dependencies:
 *       - WorkspaceService to execute the operations.
 *       - LocationService to get the possible locations a user can be at.
 *       - LicenseService to get the possible license a user can be assigned.
 *       - WxAuthorizationService to get the access token for the app to work and the id of the org to add users to.
 *       - Spring Framework's MultipartFile for receiving file as input.
 *       - CsvValidator class to validate that the proper csv file is sent in the request.
 * //
 *  Usage:
 *  Endpoint for client to export workspaces.
 */
@RestController
public class WorkspaceController {

    private final LocationService locationService;
    private final WorkspaceService workspaceService;
    private final WxAuthorizationService wxAuthorizationService;
    private final LicenseService licenseService;

    /**
     * Constructor with dependency injection.
     *
     * @param locationService to retrieve locations of the customer
     * @param workspaceService to export workspaces for the customer
     * @param wxAuthorizationService to retrieve client's authorization details
     * @param licenseService to retrieve license details of the customer
     */
    public WorkspaceController(LocationService locationService, WorkspaceService workspaceService, WxAuthorizationService wxAuthorizationService, LicenseService licenseService) {
        this.locationService = locationService;
        this.workspaceService = workspaceService;
        this.wxAuthorizationService = wxAuthorizationService;
        this.licenseService = licenseService;
    }

    /**
     * Endpoint for exporting users to Webex customer
     *
     * @param file with workspace information
     * @return response with success or failure messages
     */
    @PostMapping("/export-workspaces")
    public ResponseEntity<CustomExportResponse> exportWorkspaces(@RequestParam("file") MultipartFile file) {
        CustomExportResponse customResponse = new CustomExportResponse();

        Set<String> requiredCols = new HashSet<>(
                Set.of("Name", "Location")
        );
        if (CsvValidator.isInvalidCsvForResponse(file, customResponse, requiredCols))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customResponse);

        String accessToken = wxAuthorizationService.getAccessToken();
        String orgId = wxAuthorizationService.getAuthorizedOrgId();

        License wxCallingLicense;
        ApiResponseWrapper<ListLicensesResponse> listLicensesFromWebex = licenseService.listLicenses(accessToken, orgId);
        if (listLicensesFromWebex.is2xxSuccess() && listLicensesFromWebex.hasData()) {
            ListLicensesResponse listLicensesResponse = listLicensesFromWebex.getData();
            if (listLicensesResponse.hasLicenses() && listLicensesResponse.getWxCallingLicense() != null) {
                wxCallingLicense = listLicensesResponse.getWxCallingLicense();
            } else {
                customResponse.setError(HttpStatus.SERVICE_UNAVAILABLE.value(), "The organization does not have the Webex Calling license required.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(customResponse);
            }
        }

        Map<String, Location> locations = locationService.getLocationsMap(accessToken, orgId);

        workspaceService.exportWorkspaces();

        return ResponseEntity.ok(customResponse);
    }
}
