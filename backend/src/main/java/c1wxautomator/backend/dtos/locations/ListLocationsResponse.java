package c1wxautomator.backend.dtos.locations;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class represents the response structure for GET from the Webex API to list locations at an organization.
 *  *
 *  Usage:
 *  Services that make calls to the Webex API endpoint to list locations will receive a response represented by this data structure.
 */
@Getter
@Setter
public class ListLocationsResponse {

    private List<Location> items = new ArrayList<>();

    /**
     * Shortcut to determine if the response has any data in it.
     *
     * @return true if there are locations in the items array.
     */
    public boolean hasLocations() {
        return !this.items.isEmpty();
    }
}
