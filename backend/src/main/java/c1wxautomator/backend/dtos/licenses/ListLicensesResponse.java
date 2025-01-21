package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  This class represents the response structure for GET from the Webex API to list licenses at an organization.
 *  *
 *  Usage:
 *  Services that make calls to the Webex API endpoint to list licenses will receive a response represented by this data structure.
 */
@Setter
@Getter
public class ListLicensesResponse {
    private List<License> items = new ArrayList<>();

    /**
     * Shortcut to check if the response object has any data in it.
     *
     * @return true if there are licenses in the items array.
     */
    public boolean hasLicenses() { return !this.items.isEmpty(); }

    /**
     * Method to retrieve the Webex Calling - Professional license from the response
     *
     * @return the Webex Calling - Professional license, else null if there is not one
     */
    public License getWxCallingLicense() {
        for (License license : this.items) {
            if (Objects.equals(license.getName(), "Webex Calling - Professional")) {
                return license;
            }
        }
        return null;
    }
}
