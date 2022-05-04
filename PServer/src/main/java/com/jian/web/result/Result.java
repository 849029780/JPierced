package com.jian.web.result;

import lombok.Data;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/04
 */
@Data
public class Result<T> {

    private Integer resultCode;

    private T data;

    private String msg;

    public static <T> Result<T> newInstance(int resultCode, T data, String msg) {
        Result<T> tResult = new Result<>();
        tResult.setResultCode(resultCode);
        tResult.setData(data);
        tResult.setMsg(msg);
        return tResult;
    }

    public static <T> Result<T> SUCCESS(T data, String msg) {
        return newInstance(SUCCESS_CODE, data, msg);
    }

    public static <T> Result<T> SUCCESS(T data) {
        return SUCCESS(data, null);
    }

    public static <T> Result<T> SUCCESS(String msg) {
        return SUCCESS(null, msg);
    }

    public static <T> Result<T> SUCCESS() {
        return SUCCESS(null, null);
    }

    public static <T> Result<T> FAIL(T data, String msg) {
        return newInstance(FAIL_CODE, data, msg);
    }

    public static <T> Result<T> FAIL(T data) {
        return FAIL(data, null);
    }

    public static <T> Result<T> FAIL(String msg) {
        return FAIL(null, msg);
    }

    public static <T> Result<T> FAIL() {
        return FAIL(null, null);
    }

    public static <T> Result<T> UN_LOGIN(String msg){
        return newInstance(UN_LOGIN, null, msg);
    }


    /***
     * 成功状态
     */
    public static final int SUCCESS_CODE = 200;

    /***
     * 失败状态
     */
    public static final int FAIL_CODE = 201;

    /***
     * 未登录
     */
    public static final int UN_LOGIN = 205;


}
