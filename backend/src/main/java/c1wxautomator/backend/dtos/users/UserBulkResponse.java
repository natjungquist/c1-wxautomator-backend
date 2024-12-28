package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class represents the response structure for a result as returned by the Webex API.

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
}
