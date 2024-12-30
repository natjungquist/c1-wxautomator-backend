package c1wxautomator.backend.dtos.wrappers;
// Author: Natalie Jungquist
//
// This is a custom class representing
// Key features include:
//      - message if further details about the response need to be relayed.
//      -
//
// Usage:
// This class is intended to be used in services that ... TODO

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

    public boolean isSuccess() {
        return this.status == 200;
    }
}
