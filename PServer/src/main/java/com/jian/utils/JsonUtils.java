package com.jian.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/05
 */
@Slf4j
public class JsonUtils {

    /***
     * JSON转换
     */
    public static final ObjectMapper JSON_PARSER = new ObjectMapper();

    static{
        //属性为空时不转换
        JSON_PARSER.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String toJson(Object obj) {
        String result = null;
        if (Objects.isNull(obj)) {
            return null;
        }
        try {
            result = JsonUtils.JSON_PARSER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("转换为json错误！data:{}", obj, e);
        }
        return result;
    }

}
