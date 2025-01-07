package c1wxautomator.backend.dtos.wrappers;
// Author: Natalie Jungquist
//
// This is a custom class representing
// Key features include:
//      - status of the response that will be meaningful to sent to the client.
//      - message if further details about the response need to be relayed.
//      - data containing possible response body
//      - method to see if the response was 2xx successful
//      - method to see if the response included data
//
// Usage:
// This class is designed for services that interact with external APIs and need to provide
// informative feedback to clients about the external API responses.
//
// How to use:
// In methods that call external APIs: The return type of the method is the ApiResponseWrapper<T>.
//      - T a class that represents the structure of the response body. It is included to enforce
//      that the method returns a certain response body structure.
//      - Set the ApiResponseWrapper 'status' and 'message' according to how the
//      external API responds. This makes the status and message customizable.
//      - If the external API responds successfully, set the ApiResponseWrapper 'data' to the
//      object (T) representing the response body. Otherwise, 'data' is null.
// After calling a method the returns ApiResponseWrapper<T>: Check what the status code was
//      using .is2xxSuccessful() or .getStatus(); Check what the data in the response was
//      using .hasData() and .getData(); Get the message using .getMessage()

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Setter
@Getter
@NoArgsConstructor
public class ApiResponseWrapper<T> {
    private Integer status;
    private String message;
    private T data;

    public boolean is2xxSuccess() {
        return this.status != null && this.status >= 200 && this.status < 300;
    }
    public boolean hasData() { return this.data != null; }
}
