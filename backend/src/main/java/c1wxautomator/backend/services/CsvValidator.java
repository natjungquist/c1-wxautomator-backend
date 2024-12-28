package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// This class provides utility methods for validating CSV files uploaded via multipart form-data.
// It includes methods to:
//      - Verify if a file is a valid CSV based on content type and file extension.
//      - Check if the CSV file contains specific required columns.
//      - Count the number of rows in a CSV file.
//
// Dependencies:
//      - Spring Framework's MultipartFile for file handling.
//
// Usage:
// This class is intended to be used in services or controllers that process CSV uploads.

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

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
     * @param file the MultipartFile representing the uploaded CSV file.
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
            e.printStackTrace();
            return false;
        }
    }
}
