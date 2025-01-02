package c1wxautomator.backend.dtos.licenses;

// Author: Natalie Jungquist
//
// TODO
//
// Usage:
// This class is used

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
