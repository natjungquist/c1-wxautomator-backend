package c1wxautomator.backend.dtos.wrappers;
// Author: Natalie Jungquist
//
// This is a custom class representing
// Key features include:
//      - status of the response that will be meaningful to sent to the client.
//      - message if further details about the response need to be relayed.
//      - data containing possible response body
//
// Usage:
// This class is designed for services that interact with external APIs and need to provide
// informative feedback to clients about the external API responses.

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
public class ApiResponseWrapper {
    private Integer status;
    private String message;
    private Object data;

    public boolean is2xxSuccess() {
        return this.status != null && this.status >= 200 && this.status < 300;
    }
}
