package c1wxautomator.backend.controllers;

import c1wxautomator.backend.dtos.customResponses.CustomExportResponse;
import c1wxautomator.backend.dtos.locations.ListFloorsResponse;
import c1wxautomator.backend.dtos.wrappers.ApiResponseWrapper;
import c1wxautomator.backend.services.CsvValidator;
import c1wxautomator.backend.services.LocationService;
import c1wxautomator.backend.services.WorkspaceService;
import c1wxautomator.backend.services.WxAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@RestController
public class WorkspaceController {

    private final LocationService locationService;
    private final WorkspaceService workspaceService;
    private final WxAuthorizationService wxAuthorizationService;


    public WorkspaceController(LocationService locationService, WorkspaceService workspaceService, WxAuthorizationService wxAuthorizationService) {
        this.locationService = locationService;
        this.workspaceService = workspaceService;
        this.wxAuthorizationService = wxAuthorizationService;
    }

    @PostMapping("/export-workspaces")
    public ResponseEntity<CustomExportResponse> exportWorkspaces(@RequestParam("file") MultipartFile file) {
        CustomExportResponse customResponse = new CustomExportResponse();

//        if (file == null || file.isEmpty()) {
//            customResponse.setError(HttpStatus.BAD_REQUEST.value(), "File is required and cannot be empty.");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customResponse);
//        }
//        if (!CsvValidator.isCsvFile(file)) {
//            customResponse.setError(HttpStatus.BAD_REQUEST.value(), "The wrong type of file was provided. Must be a CSV file.");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customResponse);
//        }
//        Set<String> requiredCols = new HashSet<>(
//                Set.of("First Name", "Display Name", "Status", "Email", "Extension", "Location",
//                        "Webex Contact Center Premium Agent", "Webex Contact Center Standard Agent", "Webex Calling - Professional")
//        );
//        if (!(CsvValidator.csvContainsRequiredCols(file, requiredCols))) {
//            customResponse.setError(HttpStatus.BAD_REQUEST.value(), "File provided does not contain all the columns required to process the request.");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customResponse);
//        }

        String accessToken = wxAuthorizationService.getAccessToken();


        return ResponseEntity.ok(customResponse);
    }
}
