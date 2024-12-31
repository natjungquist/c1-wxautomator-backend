package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for managing location operations with Webex APIs.
// Key features:
//      - list the available locations of an organization.
//      - retrieve details about a specific location.
//
// Usage:
// Used by any controller or service that needs to use Webex location API.

import c1wxautomator.backend.dtos.locations.Location;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    // location
    // https://license-r.wbx2.com/license/api/v1/organization/5ef33965-19a4-48d0-b47d-61eb390f2f3d/user/1370b869-82a7-47f6-bdcb-6ac9df3dbf01/location/4acf4b7d-6f57-4eee-8dde-9cfc5a881706
    // Request Method:PUT

    /**
     * Calls the Webex API to get a list of all locations of an organization.
     * Uses the RestTemplate to make the API call and handles authorization with an OAuth2 token.
     *
     * @param accessToken The token used for authenticating the request.
     * @param orgId id of the organization to export users to.
     * @return list of locations at the org.
     */
    public List<Location> listLocations(String accessToken, String orgId) {
        return null;
    }

    /**
     * Creates a map of all the locations given. This map makes it easier to lookup by a location name.
     *
     * @param allLocations list of locations to be put in a map.
     * @return map of locations where the name is the key and a location object is the value.
     */
    public Map<String, Location> makeLocationsMap(List<Location> allLocations) {
        Map<String, Location> locationMap = new HashMap<>();
        for (Location loc : allLocations) {
            locationMap.put(loc.getName(), loc);
        }
        return locationMap;
    }

    public void getLocation(String locationName) {

    }
}
