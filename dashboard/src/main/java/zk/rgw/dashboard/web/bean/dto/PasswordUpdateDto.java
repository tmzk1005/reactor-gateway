package zk.rgw.dashboard.web.bean.dto;

import lombok.Getter;
import lombok.Setter;

import zk.rgw.dashboard.framework.validate.NotBlank;
import zk.rgw.dashboard.framework.validate.Size;
import zk.rgw.dashboard.framework.validate.ValidatableDto;

@Getter
@Setter
public class PasswordUpdateDto implements ValidatableDto {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "新密码不能超过32个字符长度，且最少需要8个字符长度")
    private String newPassword;

}
