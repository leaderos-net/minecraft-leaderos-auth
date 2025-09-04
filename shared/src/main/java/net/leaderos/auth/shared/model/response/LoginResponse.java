package net.leaderos.auth.shared.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.auth.shared.enums.ErrorCode;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private boolean status;
    private ErrorCode error;
    private String token;
    private boolean tfaRequired;
}
