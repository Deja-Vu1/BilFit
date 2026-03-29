package database;

public enum DbStatus {
    SUCCESS,
    CONNECTION_ERROR,
    AUTHENTICATION_FAILED,
    DATA_NOT_FOUND,
    QUERY_ERROR,
    DUPLICATE_ENTRY, // Same entry already exists in the database
    INVALID_CODE     // Wrong code provided for verification
}