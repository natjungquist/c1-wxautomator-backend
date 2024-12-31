package c1wxautomator.backend.services;

// Author: Natalie Jungquist
//
// Service class for managing license operations with Webex APIs.
// Key features:
//      - list all the available licenses of an organization.
//      - retrieve details about specific licenses such as 'Contact center standard agent',
//        'contact center premium agent', and 'webex calling professional'.
//
// Usage:
// Used by any controller or service that needs to use Webex license API.

import c1wxautomator.backend.dtos.licenses.License;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LicenseService {

    /**
     * Calls the Webex API to get a list of all licenses owned by an organization.
     * Uses the RestTemplate to make the API call and handles authorization with an OAuth2 token.
     *
     * @param accessToken The token used for authenticating the request.
     * @param orgId id of the organization to export users to.
     * @return list of available licenses.
     */
    public List<License> listLicenses(String accessToken, String orgId) {
        return null;
    }

    /**
     * Creates a map of all the licenses given. This map makes it easier to lookup by a license name.
     *
     * @param allLicenses list of licenses to be put in a map.
     * @return map of licenses where the name is the key and a license object is the value,
     * else null if allLicenses has nothing in it.
     */
    public Map<String, License> makeLicensesMap(List<License> allLicenses) {
        if (allLicenses == null || allLicenses.isEmpty()) {
            return null;
        }
        Map<String, License> licenseMap = new HashMap<>();
        for (License license : allLicenses) {
            licenseMap.put(license.getName(), license);
        }
        return licenseMap;
    }
}
