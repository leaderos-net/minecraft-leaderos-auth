package net.leaderos.shared.error;

public enum Error {

    // login
    USER_NOT_FOUND,
    WRONG_PASSWORD,

    // register
    USERNAME_ALREADY_EXIST,
    EMAIL_ALREADY_EXIST,
    REGISTER_LIMIT,
    INVALID_USERNAME,

    // session
    ACCOUNT_NOT_FOUND,
    LOGIN_REQUIRED,
    HAS_SESSION,

    ;

}
