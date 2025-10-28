package net.leaderos.auth.shared.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.auth.shared.enums.ErrorCode;
import net.leaderos.auth.shared.enums.SessionState;

@Getter
@Setter
@AllArgsConstructor
public class GameSessionResponse {
    private boolean status;
    private ErrorCode error;
    private SessionState state;
    private String token;
    private String username;

    public boolean isAuthenticated() {
        return state == SessionState.AUTHENTICATED;
    }
}
