package net.leaderos.shared.enums;

public enum ErrorCode {

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
