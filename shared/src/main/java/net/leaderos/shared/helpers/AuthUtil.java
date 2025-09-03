package net.leaderos.shared.helpers;

import net.leaderos.shared.enums.ErrorCode;
import net.leaderos.shared.enums.SessionStatus;
import net.leaderos.shared.model.Response;
import net.leaderos.shared.model.request.impl.auth.LoginRequest;
import net.leaderos.shared.model.request.impl.auth.RegisterRequest;
import net.leaderos.shared.model.request.impl.auth.SessionRequest;
import net.leaderos.shared.model.request.impl.auth.VerifyTfaRequest;
import net.leaderos.shared.model.response.GameSessionResponse;
import net.leaderos.shared.model.response.LoginResponse;
import net.leaderos.shared.model.response.RegisterResponse;
import net.leaderos.shared.model.response.VerifyTfaResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuthUtil {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    public static CompletableFuture<GameSessionResponse> checkGameSession(String username, String ip, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SessionRequest request = new SessionRequest(username, ip, userAgent);
                Response response = request.getResponse();

                return new GameSessionResponse(
                        SessionStatus.valueOf(response.getResponseMessage().getString("status")),
                        response.getResponseMessage().optString("token", null)
                );

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<LoginResponse> login(String username, String password, String ip, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LoginRequest request = new LoginRequest(username, password, ip, userAgent);
                Response response = request.getResponse();

                if (response.isStatus() && response.getError() == null) {
                    // Successful response
                    return new LoginResponse(
                            true,
                            null,
                            response.getResponseMessage().getJSONObject("data").getString("token"),
                            response.getResponseMessage().getJSONObject("data").getBoolean("isTfaRequired")
                    );
                }

                // Error response
                return new LoginResponse(
                        false,
                        ErrorCode.valueOf(response.getError().name()),
                        null,
                        false
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<RegisterResponse> register(String username, String password, String email, String ip, String userAgent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RegisterRequest request = new RegisterRequest(username, password, email, ip, userAgent);
                Response response = request.getResponse();

                if (response.isStatus() && response.getError() == null) {
                    // Successful response
                    return new RegisterResponse(
                            true,
                            null,
                            response.getResponseMessage().getJSONObject("data").getString("token")
                    );
                }

                // Error response
                return new RegisterResponse(
                        false,
                        ErrorCode.valueOf(response.getError().name()),
                        null
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<VerifyTfaResponse> verifyTfa(String code, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                VerifyTfaRequest request = new VerifyTfaRequest(code, token);
                Response response = request.getResponse();
                if (response.isStatus() && response.getError() == null) {
                    return new VerifyTfaResponse(true, null);
                }

                return new VerifyTfaResponse(false, ErrorCode.valueOf(response.getError().name()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

}
