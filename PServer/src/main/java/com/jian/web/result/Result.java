package com.jian.web.result;

import lombok.Data;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/04
 */
@Data
public class Result {

    private Integer resultCode;

    private Object data;

    private String msg;

    public static Result newInstance(int resultCode, Object data, String msg) {
        Result tResult = new Result();
        tResult.setResultCode(resultCode);
        tResult.setData(data);
        tResult.setMsg(msg);
        return tResult;
    }

    public static Result SUCCESS(Object data, String msg) {
        return newInstance(SUCCESS_CODE, data, msg);
    }

    public static Result SUCCESS(Object data) {
        return SUCCESS(data, null);
    }

    public static Result SUCCESS(String msg) {
        return SUCCESS(null, msg);
    }

    public static Result SUCCESS() {
        return SUCCESS(null, null);
    }

    public static Result FAIL(Object data, String msg) {
        return newInstance(FAIL_CODE, data, msg);
    }

    public static Result FAIL(Object data) {
        return FAIL(data, null);
    }

    public static Result FAIL(String msg) {
        return FAIL(null, msg);
    }

    public static Result FAIL() {
        return FAIL(null, null);
    }

    public static Result UN_LOGIN(String msg) {
        return newInstance(UN_LOGIN, null, msg);
    }

    public static Result TOKEN_EXPIRE(String msg) {
        return newInstance(TOKEN_EXPIRE, null, msg);
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

    /***
     * token已失效
     */
    public static final int TOKEN_EXPIRE = 206;


}
