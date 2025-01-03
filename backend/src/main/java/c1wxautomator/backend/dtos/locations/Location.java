package c1wxautomator.backend.dtos.locations;

// Author: Natalie Jungquist
//
// TODO
//
// Usage:
// This class is used

import lombok.Getter;
import lombok.Setter;

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
