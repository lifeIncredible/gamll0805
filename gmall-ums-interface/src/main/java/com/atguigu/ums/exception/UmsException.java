package com.atguigu.ums.exception;

/**
 * @author shkstart
 * @create 2020-01-15 20:56
 */
public class UmsException extends RuntimeException {

    public UmsException(String message) {
        super(message);
    }

    public UmsException() {
        super();
    }
}
