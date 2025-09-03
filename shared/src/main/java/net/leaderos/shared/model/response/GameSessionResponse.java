package net.leaderos.shared.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.shared.enums.SessionStatus;

@Getter
@Setter
@AllArgsConstructor
public class GameSessionResponse {
    private SessionStatus status;
    private String token;

    public boolean isAuthenticated() {
        return status == SessionStatus.AUTHENTICATED;
    }
}
