package net.leaderos.auth.shared.helpers;

import net.leaderos.auth.shared.enums.ErrorCode;
import net.leaderos.auth.shared.enums.SessionState;
import net.leaderos.auth.shared.model.Response;
import net.leaderos.auth.shared.model.request.impl.auth.LoginRequest;
import net.leaderos.auth.shared.model.request.impl.auth.RegisterRequest;
import net.leaderos.auth.shared.model.request.impl.auth.SessionRequest;
import net.leaderos.auth.shared.model.request.impl.auth.VerifyTfaRequest;
import net.leaderos.auth.shared.model.response.GameSessionResponse;
import net.leaderos.auth.shared.model.response.LoginResponse;
import net.leaderos.auth.shared.model.response.RegisterResponse;
import net.leaderos.auth.shared.model.response.VerifyTfaResponse;

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

                if (response.isStatus() && response.getError() == null && response.getResponseMessage().getJSONObject("data") != null) {
                    String sessionUsername = null;
                    if (response.getResponseMessage().getJSONObject("data").optJSONObject("user") != null) {
                        sessionUsername = response.getResponseMessage().getJSONObject("data").getJSONObject("user").optString("realname", null);
                    }

                    return new GameSessionResponse(
                            true,
                            null,
                            SessionState.valueOf(response.getResponseMessage().getJSONObject("data").getString("state")),
                            response.getResponseMessage().getJSONObject("data").optString("token", null),
                            sessionUsername
                    );
                }

                // Error response
                return new GameSessionResponse(
                        false,
                        response.getError() != null ? ErrorCode.valueOf(response.getError().name()) : ErrorCode.UNKNOWN_ERROR,
                        null,
                        null,
                        null
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

                if (response.isStatus() && response.getError() == null && response.getResponseMessage().getJSONObject("data") != null) {
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
                        response.getError() != null ? ErrorCode.valueOf(response.getError().name()) : ErrorCode.UNKNOWN_ERROR,
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

                if (response.isStatus() && response.getError() == null && response.getResponseMessage().getJSONObject("data") != null) {
                    // Successful response
                    return new RegisterResponse(
                            true,
                            null,
                            response.getResponseMessage().getJSONObject("data").getString("token"),
                            response.getResponseMessage().getJSONObject("data").getBoolean("isEmailVerificationRequired")
                    );
                }

                // Error response
                return new RegisterResponse(
                        false,
                        response.getError() != null ? ErrorCode.valueOf(response.getError().name()) : ErrorCode.UNKNOWN_ERROR,
                        null,
                        false
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

                return new VerifyTfaResponse(
                        false,
                        response.getError() != null ? ErrorCode.valueOf(response.getError().name()) : ErrorCode.UNKNOWN_ERROR
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

}
