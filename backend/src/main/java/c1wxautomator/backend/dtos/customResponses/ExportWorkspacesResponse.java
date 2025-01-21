package c1wxautomator.backend.dtos.customResponses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 *  This is a custom class representing a response that the application sends to the client.
 *  Key features include:
 *       - results list storing information about each workspace processed during the export.
 *       - methods to add success and failure results to the list of results,
 *  *
 *  The WorkspaceResult is a nested static class that represents individual
 *  results of API operations, each containing relevant details for the operation.
 *  *
 *  Usage:
 *  This class is intended to be used in services and controllers that export workspaces to Webex.
 *  Controllers will send this as the response body to the client.
 */
@Getter
@Setter
@NoArgsConstructor
public class ExportWorkspacesResponse extends CustomExportResponse {

    List<WorkspaceResult> results = new ArrayList<>();

    public void addSuccess(String workspaceName) {
        incrementNumSuccessfullyCreated();
        incrementTotalCreateAttempts();
    }
    public void addFailure(String workspaceName) {
        incrementTotalCreateAttempts();
    }

    /**
     * Nested class to represent the data to send back to the client about the user's creation status.
     */
    @Getter
    @Setter
    public static class WorkspaceResult {
        private Integer status;
        private String message = "";
        private String name;
    }
}
