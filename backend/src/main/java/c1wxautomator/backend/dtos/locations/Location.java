package c1wxautomator.backend.dtos.locations;

// Author: Natalie Jungquist

import lombok.Getter;
import lombok.Setter;

/**
 *  Represents a Location that an organization may have.
 *  Includes details such as location ID, name, organization ID, address, time zone,
 *  preferred language, latitude, longitude, and additional notes. Also encapsulates
 *  an Address class with fields for detailed address information.
 *  *
 *  Usage:
 *  To transfer location data between different services and controllers.
 */
@Getter
@Setter
public class Location {
    private String id;
    private String name;
    private String orgId;
    private Address address;
    private String timeZone;
    private String preferredLanguage;
    private String latitude;
    private String longitude;
    private String notes;

    /**
     * Nested class that represents the address data of a location.
     */
    @Getter
    @Setter
    public static class Address {
        private String address1;
        private String address2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
}
