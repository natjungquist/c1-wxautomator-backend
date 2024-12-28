package c1wxautomator.backend.dtos.users;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class UserBulkResponse {
    private List<String> schemas;
    private List<UserOperationResponse> operations;
}
