package net.leaderos.auth.shared.enums;

public enum ErrorCode {

    // login
    USER_NOT_FOUND,
    WRONG_PASSWORD,

    // register
    USERNAME_ALREADY_EXIST,
    EMAIL_ALREADY_EXIST,
    REGISTER_LIMIT,
    INVALID_USERNAME,
    INVALID_EMAIL,
    INVALID_PASSWORD,

    // tfa
    WRONG_CODE,
    SESSION_NOT_FOUND,
    TFA_VERIFICATION_FAILED,

    UNKNOWN_ERROR

}
