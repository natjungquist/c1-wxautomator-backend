package c1wxautomator.backend.services;

// Author: Natalie Jungquist

import c1wxautomator.backend.dtos.customResponses.CustomExportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 *  This class provides utility methods for validating CSV files uploaded via multipart form-data.
 *  It includes methods to:
 *       - Verify if a file is a valid CSV based on content type and file extension.
 *       - Check if the CSV file contains specific required columns.
 *       - Count the number of rows in a CSV file.
 *  *
 *  Dependencies:
 *       - Spring Framework's MultipartFile for file handling.
 *  *
 *  Usage:
 *  This class is intended to be used in services or controllers that process CSV uploads.
 */
public class CsvValidator {

    /**
     * Validates whether the given file is a CSV based on its content type and file extension.
     *
     * @param file the MultipartFile representing the uploaded file.
     * @return true if the file is a valid CSV, false otherwise.
     */
    public static boolean isCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Check Content Type
        String contentType = file.getContentType();
        if (contentType != null && contentType.equals("text/csv")) {
            return true;
        }

        // Check File Extension
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    /**
     * Checks whether the provided CSV file contains all the specified required columns in its header.
     *
     * @param file         the MultipartFile representing the uploaded CSV file.
     * @param requiredCols a Set of Strings representing the required column names.
     * @return true if the file contains all the required columns, false otherwise.
     */
    public static boolean csvContainsRequiredCols(MultipartFile file, Set<String> requiredCols) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Read the first line as the header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return false; // No header, invalid file
            }

            // Split the header line into columns
            String[] header = headerLine.split(",");

            // Convert the header into a Set for easy comparison
            Set<String> headerSet = new HashSet<>(Set.of(header));

            // Check if required columns are in the header
            return headerSet.containsAll(requiredCols);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the file is valid for further processing.
     * If not valid, updates the CustomExportResponse with value and message.
     * Use in controllers that receive csv files as request parameters.
     *
     * @param file input from client
     * @param customResponse to be sent back to client
     * @param requiredCols required columns in the csv file
     * @return true if the file is NOT valid, false if it is valid
     */
    public static boolean isInvalidCsvForResponse(@RequestParam("file") MultipartFile file, CustomExportResponse customResponse, Set<String> requiredCols) {
        if (file == null || file.isEmpty()) {
            customResponse.setError(HttpStatus.BAD_REQUEST.value(), "File is required and cannot be empty.");
            return true;
        }
        if (!isCsvFile(file)) {
            customResponse.setError(HttpStatus.BAD_REQUEST.value(), "The wrong type of file was provided. Must be a CSV file.");
            return true;
        }
        if (!csvContainsRequiredCols(file, requiredCols)) {
            customResponse.setError(HttpStatus.BAD_REQUEST.value(), "File provided does not contain all the columns required to process the request.");
            return true;
        }
        return false;
    }
}
