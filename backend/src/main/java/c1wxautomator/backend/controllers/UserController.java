package c1wxautomator.backend.controllers;


// Author: Natalie Jungquist
//
// This controller handles user-related endpoints for the application.
// Key features include:
//      - Export users to Webex given a csv file as input.
//
// Dependencies:
//      - UserService to execute the operations.
//      - Spring Framework's MultipartFile for receiving file as input.
//
// Usage:
// Endpoint for client to export users.

import c1wxautomator.backend.dtos.users.CustomExportUsersResponse;
import c1wxautomator.backend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    /**
     * Endpoint for exporting users to Webex customer
     *
     * @param file with user information
     * @return success or failure message
     */
    @PostMapping("/export-users")
    public ResponseEntity<?> exportUsersCsv(@RequestParam("file") MultipartFile file) {
        try {
            CustomExportUsersResponse response = userService.exportUsers(file);

            Map<String, String> dummy = new HashMap<>();
            dummy.put("message", "dummy");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dummy);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("??");  // TODO some message

            // TODO response will have:
            // if 403: forbidden message
            // if some other conflict on Webex's side: return the message given by Webex
        }
    }
}