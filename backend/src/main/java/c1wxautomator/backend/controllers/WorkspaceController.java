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
 *
 */
@RestController
public class WorkspaceController {

    private final LocationService locationService;
    private final WorkspaceService workspaceService;
    private final WxAuthorizationService wxAuthorizationService;
    private final LicenseService licenseService;

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
