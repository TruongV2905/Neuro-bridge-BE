package com.nqt.common_starter.exception;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GlobalException extends RuntimeException{
    Integer code;
    //tính đa hình
    //overloading
    public GlobalException(String message) {
        super(message);
    }

    public GlobalException(int code, String message) {
        super(message);
        this.code = code;
    }
}
