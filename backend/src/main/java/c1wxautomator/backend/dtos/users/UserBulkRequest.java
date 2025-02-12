package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *  This class holds data for the bulk request that is sent to the Webex API.
 *  Its fields are named to fit Webex API request specifications.
 *  *
 *  Usage:
 *  UserService will use this class to send data to Webex API.
 */
@Setter
@Getter
@NoArgsConstructor
public class UserBulkRequest {
    private Integer failOnErrors;  //  The maximum number of errors that the service provider will accept before the operation is terminated and an error response is returned.
    private List<String> schemas;  // Provided by Webex API and set to each request in UserService
    private List<UserOperationRequest> operations;  // The operations to be requested
}
