package zk.rgw.dashboard.web.bean.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDto {

    @JsonProperty(required = true)
    private String username;

    @JsonProperty(required = true)
    private String password;

}
