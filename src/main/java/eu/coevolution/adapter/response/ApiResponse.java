package eu.coevolution.adapter.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApiResponse {

    private Object data;
    private Boolean success;

    public ApiResponse(Object data, Boolean success) {
        this.data = data;
        this.success = success;
    }

}
