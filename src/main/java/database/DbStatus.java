package database;

public enum DbStatus {
    SUCCESS,
    CONNECTION_ERROR,
    AUTHENTICATION_FAILED,
    DATA_NOT_FOUND,
    QUERY_ERROR,
    DUPLICATE_ENTRY, // Same entry already exists in the database
    INVALID_CODE,     // Wrong code provided for verification
    EXPIRED_CODE,     // Code provided has expired
    EMAIL_ALREADY_EXISTS, // Email is already registered in the system
    ID_ALREADY_EXISTS, // Student ID is already registered in the system
    ACCOUNT_NOT_ACTIVATED, // Account exists but is not activated yet
    INVALID_CREDENTIALS, // Wrong email or password provided during login
    UNAVAILABLE, // Requested facility is not available for the given time slot
    SAME_PASSWORD, // New password is the same as the old password during password change
    FILE_TOO_LARGE, // Uploaded file exceeds the allowed size limit
    ACCOUNT_BANNED // Account has been banned due to policy violations or other reasons
}