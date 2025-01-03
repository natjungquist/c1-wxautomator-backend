package c1wxautomator.backend.dtos.locations;

// Author: Natalie Jungquist
//
// This class represents the response structure for GET from the Webex API to list locations at an organization.
// https://developer.webex.com/docs/api/v1/locations/list-locations
//
// Usage:
// Services that make calls to the Webex API endpoint to list locations will receive a response represented by this data structure.

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ListLocationsResponse {

    private List<Location> items = new ArrayList<>();

    public boolean hasLocations() {
        return !this.items.isEmpty();
    }
}
