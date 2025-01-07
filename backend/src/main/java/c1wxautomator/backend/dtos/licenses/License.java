package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist
//
// Represents a License entity that a Webex organization may have.
// Includes details such as license ID, name, total units, consumed units, units consumed
// by users and workspaces, subscription ID, site URL, and site type.
//
// Usage:
// To transfer location data between different services and controllers to transfer location data.

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class License {
    private String id;
    private String name;
    private Integer totalUnits;
    private Integer consumedUnits;
    private Integer consumedByUsers;
    private Integer consumedByWorkspaces;
    private String subscriptionId;
    private String siteUrl;
    private String siteType;
}
