package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 *  This class holds data for the 'operations' portion of the request sent to the Webex API for bulk creating users.
 *  Its fields are named to fit Webex API request specifications.
 * //
 *  Usage:
 *  UserService uses this class to send data to Webex API.
 */
@Setter
@Getter
@NoArgsConstructor
public class UserOperationRequest {
    private String method;  // Such as POST, PATCH, or DELETE
    private String path;  // Set to '/User' to create a new user; or set to '/User/{user_id}' to update a current user
    private UserRequest data;  // The user object for the data
    private String bulkId;  // Needed for API call; generated by UserService
}
