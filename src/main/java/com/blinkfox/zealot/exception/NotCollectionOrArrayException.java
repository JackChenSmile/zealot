package com.blinkfox.zealot.exception;

/**
 * 不是集合或者数组的自定义异常.
 * @author blinkfox on 2017-04-01.
 */
public class NotCollectionOrArrayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 附带日志消息参数的构造方法.
     * @param msg 日志消息
     */
    public NotCollectionOrArrayException(String msg) {
        super(msg);
    }

}