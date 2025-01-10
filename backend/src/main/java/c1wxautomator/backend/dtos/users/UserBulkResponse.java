package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *  This class represents the response structure for a result as returned by the Webex API.
 *  *
 *  Usage:
 *  Services that make calls to the Webex API endpoint to bulk create users will receive
 *  a response represented by this data structure.
 */
@Setter
@Getter
@NoArgsConstructor
public class UserBulkResponse {
    private List<String> schemas;
    private List<UserOperationResponse> operations;

    /**
     * Shortcut to determine if the response has any data in it.
     *
     * @return true if there are users in the operations array.
     */
    public boolean hasOperations() {
        return operations != null && !operations.isEmpty();
    }
}
