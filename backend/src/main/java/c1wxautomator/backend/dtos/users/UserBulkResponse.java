package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class represents the response structure for a result as returned by the Webex API.
//
// Usage:
// Services that make calls to the Webex API endpoint to bulk create users will receive
// a response represented by this data structure.

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UserBulkResponse {
    private List<String> schemas;
    private List<UserOperationResponse> operations;

    public boolean hasOperations() {
        return operations != null && !operations.isEmpty();
    }
}
