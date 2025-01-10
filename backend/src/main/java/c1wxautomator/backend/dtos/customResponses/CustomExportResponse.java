package c1wxautomator.backend.dtos.customResponses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class CustomExportResponse {
    private Integer status;
    private Integer totalCreateAttempts = 0;
    private Integer numSuccessfullyCreated = 0;
    private String message = "";
    private List<CustomExportUsersResponse.CreateUserResult> results = new ArrayList<>();

    protected void incrementNumSuccessfullyCreated() {
        this.numSuccessfullyCreated++;
    }
    protected void incrementTotalCreateAttempts() {
        this.totalCreateAttempts++;
    }
}
