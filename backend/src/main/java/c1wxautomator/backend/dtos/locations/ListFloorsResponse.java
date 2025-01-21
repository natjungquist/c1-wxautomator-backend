package c1wxautomator.backend.dtos.locations;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the response structure for GET from the Webex API to list the floors of a location at an organization.
 * *
 * Usage:
 * Services that make calls to the Webex API endpoint to list location floors will receive a response represented by this data structure.
 */
@Getter
@Setter
public class ListFloorsResponse {

    List<Floor> items = new ArrayList<>();

    /**
     * Shortcut to determine if the response has any data in it.
     *
     * @return true if there are floors in the items array.
     */
    public boolean hasFloors() {
        return !this.items.isEmpty();
    }

    /**
     *  Represents a floor that an organization may have.
     *  *
     *  Usage:
     *  To transfer floor data between different services and controllers.
     *  Floor info is needed when exporting workspaces.
     */
    @Getter
    @Setter
    public static class Floor {
        private String id;
        private String locationId;
        private Integer floorNumber;
        private String displayName;
    }
}
