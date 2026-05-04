package com.nqt.common_starter.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public enum ErrorCode {
    //  =======================================================
    FIELD_REQUIRED("cannot be blank!", HttpStatus.BAD_REQUEST),
    INVALID_PATTERN_PASSWORD(
            "Password must contain at least one uppercase letter, one lowercase letter, one number, and"
                    + " one special character",
            HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED("User does not exist", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED("Email already exists", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED("Phone number already exists", HttpStatus.BAD_REQUEST),
    INVALID_KEY("Uncategorized error", HttpStatus.BAD_REQUEST),
    EMPTY_TOKEN("Empty token", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("Expired token!", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Invalid token!", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("Invalid refresh token!", HttpStatus.UNAUTHORIZED),
    INVALID_CODE("Invalid message code!", HttpStatus.BAD_REQUEST),
    NOT_MATCH_TOKEN("Token does not match locally computed signature!", HttpStatus.UNAUTHORIZED),
    OTHER(null, HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(null, HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED("Unauthenticated!", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("You do not have permission!", HttpStatus.FORBIDDEN),
    DATA_INVALID("Invalid data", HttpStatus.BAD_REQUEST),
    DB_ERROR("Database error", HttpStatus.INTERNAL_SERVER_ERROR),

    ACCOUNT_BANNED("account is banned", HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE("account is inactive", HttpStatus.FORBIDDEN),
    STAFF_BUSY("Staff is busy", HttpStatus.BAD_REQUEST),
    FIELDS_EMPTY("Black or missing fileds", HttpStatus.BAD_REQUEST),
    NOT_FOUND_ENUM("Field enum not found", HttpStatus.NOT_FOUND),
    INVALID_STATUS("Invalid status", HttpStatus.BAD_REQUEST),
    EXPIRED_CONTRACT("Contract is expired", HttpStatus.BAD_REQUEST),
    INVENTORY_INVALID("Inventory is invalid", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY("Quantity is invalid", HttpStatus.BAD_REQUEST),
    VEHICLE_DUPLICATED("Vehicle is duplicated", HttpStatus.BAD_REQUEST),
    VEHICLE_PRICE_NOT_SET("Vehicle price not set", HttpStatus.BAD_REQUEST),
    TOO_MANY_OTP("You sent otp too much", HttpStatus.BAD_REQUEST),
    INVALID_DEPOSIT("Deposit is invalid", HttpStatus.BAD_REQUEST),
    INVALID_TERM("Month term is invalid", HttpStatus.BAD_REQUEST),
    NOT_FOUND("Not found!", HttpStatus.NOT_FOUND);


    final String message;
    final HttpStatus status;

    public int getCode() {
        return status.value();
    }
}
