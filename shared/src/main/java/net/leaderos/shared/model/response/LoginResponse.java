package net.leaderos.shared.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.shared.enums.ErrorCode;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private boolean status;
    private ErrorCode error;
    private String token;
    private boolean tfaRequired;
}
