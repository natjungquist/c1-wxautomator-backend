package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist
//
// This class represents the response structure for GET from the Webex API to list licenses at an organization.
// https://developer.webex.com/docs/api/v1/licenses/list-licenses
//
// Usage:
// Services that make calls to the Webex API endpoint to list licenses will receive a response represented by this data structure.

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ListLicensesResponse {
    private List<License> items = new ArrayList<>();

    public boolean hasLicenses() { return !this.items.isEmpty(); }
}
