package net.leaderos.shared.helpers;

import net.leaderos.shared.enums.AuthResponse;
import net.leaderos.shared.model.Response;
import net.leaderos.shared.model.request.impl.auth.LoginRequest;
import net.leaderos.shared.model.request.impl.auth.RegisterRequest;
import net.leaderos.shared.model.request.impl.auth.SessionRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class AuthUtil {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    public static CompletableFuture<AuthResponse> checkGameSession(String username, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SessionRequest request = new SessionRequest(username, ip);
                Response response = request.getResponse();
                
                if (response == null) {
                    return AuthResponse.LOGIN_REQUIRED;
                }
                
                if (response.isStatus()) {
                    JSONObject responseMessage = response.getResponseMessage();
                    if (responseMessage != null && responseMessage.has("status")) {
                        try {
                            return AuthResponse.valueOf(responseMessage.getString("status"));
                        } catch (IllegalArgumentException e) {
                            return AuthResponse.LOGIN_REQUIRED;
                        }
                    }
                    return AuthResponse.SUCCESS;
                }
                
                // If there's an error, try to map it to AuthResponse
                if (response.getError() != null) {
                    try {
                        return AuthResponse.valueOf(response.getError().name());
                    } catch (IllegalArgumentException e) {
                        return AuthResponse.LOGIN_REQUIRED;
                    }
                }
                
                return AuthResponse.LOGIN_REQUIRED;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<AuthResponse> login(String username, String password, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LoginRequest request = new LoginRequest(username, password, ip);
                Response response = request.getResponse();
                
                if (response == null) {
                    return AuthResponse.WRONG_PASSWORD;
                }
                
                if (response.isStatus()) {
                    return AuthResponse.SUCCESS;
                }

                if (response.getError() != null) {
                    try {
                        return AuthResponse.valueOf(response.getError().name());
                    } catch (IllegalArgumentException e) {
                        return AuthResponse.WRONG_PASSWORD;
                    }
                }
                
                return AuthResponse.WRONG_PASSWORD;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<AuthResponse> register(String username, String password, String email, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RegisterRequest request = new RegisterRequest(username, password, email, ip);
                Response response = request.getResponse();
                
                if (response == null) {
                    return AuthResponse.USERNAME_ALREADY_EXIST;
                }
                
                if (response.isStatus()) {
                    return AuthResponse.SUCCESS;
                }

                if (response.getError() != null) {
                    try {
                        return AuthResponse.valueOf(response.getError().name());
                    } catch (IllegalArgumentException e) {
                        return AuthResponse.USERNAME_ALREADY_EXIST;
                    }
                }
                
                return AuthResponse.USERNAME_ALREADY_EXIST;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

}
