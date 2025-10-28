package net.leaderos.auth.shared.enums;

public enum SessionState {

    AUTHENTICATED,
    LOGIN_REQUIRED,
    REGISTER_REQUIRED,
    HAS_SESSION,
    TFA_REQUIRED,
    EMAIL_NOT_VERIFIED,
    USERNAME_CASE_MISMATCH

}
