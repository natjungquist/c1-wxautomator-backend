package c1wxautomator.backend.dtos.users;

// Author: Natalie Jungquist
//
// This class encapsulates metadata associated with a user.
// It provides various attributes and methods for handling user-related information.
// Key features:
//      - UserRequest stores the information provided by the client when they request to create a new user.
//              This data is what is passed to the Webex API to create the user.
//
// Usage:
// This class is utilized by services that handle exporting users.

import c1wxautomator.backend.dtos.licenses.License;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class UserMetadata {
    private UserRequest userRequest;
    private int webexId;
    private List<License> licenses = new ArrayList<>();
    private String location;

    /**
     * Method to get the user's phone extension.
     *
     * @return String of digits representing the user's extension, or null if they don't have one.
     */
    public String getExtension() {
        if (!userRequest.getPhoneNumbers().isEmpty()) {
            for (UserRequest.PhoneNumber num : userRequest.getPhoneNumbers()) {
                if (num.getType().equals("work_extension")) {
                    return num.getValue();
                }
            }
        }
        return "";
    }
}
