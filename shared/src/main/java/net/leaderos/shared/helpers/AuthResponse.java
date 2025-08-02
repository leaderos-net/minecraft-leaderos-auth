package net.leaderos.shared.helpers;

public enum AuthResponse {

    // Session statuses
    LOGIN_REQUIRED,
    ACCOUNT_NOT_FOUND,
    HAS_SESSION,

    // Login statuses
    USER_NOT_FOUND,
    WRONG_PASSWORD,

    // Register statuses
    USERNAME_ALREADY_EXIST,
    EMAIL_ALREADY_EXIST,
    REGISTER_LIMIT,
    INVALID_USERNAME,

    SUCCESS,

    ;

    public boolean isAuthenticated() {
        return this == HAS_SESSION || this == SUCCESS;
    }

}
