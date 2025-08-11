package net.leaderos.shared.helpers;

import net.leaderos.shared.model.Response;
import net.leaderos.shared.model.request.impl.auth.LoginRequest;
import net.leaderos.shared.model.request.impl.auth.RegisterRequest;
import net.leaderos.shared.model.request.impl.auth.SessionRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuthUtil {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    public static CompletableFuture<AuthResponse> checkGameSession(String username, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SessionRequest request = new SessionRequest(username, ip);
                Response response = request.getResponse();
                return AuthResponse.valueOf(response.getResponseMessage().getString("status"));
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
                if (response.isStatus()) {
                    return AuthResponse.SUCCESS;
                }

                return AuthResponse.valueOf(response.getError().name());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

    public static CompletableFuture<AuthResponse> register(String username, String password, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RegisterRequest request = new RegisterRequest(username, password, ip);
                Response response = request.getResponse();
                if (response.isStatus()) {
                    return AuthResponse.SUCCESS;
                }

                return AuthResponse.valueOf(response.getError().name());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EXECUTOR);
    }

}
