package c1wxautomator.backend.dtos.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SearchUsersResponse {
    private List<String> schemas;
    private Integer totalResults;
    @JsonProperty("Resources")
    private List<User> users;
}
