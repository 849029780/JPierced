package com.jian.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/***
 * yaml工具
 * @author Jian
 * @date 2024-12-30
 */
public class YamlUtils {

    public static ObjectMapper getYamlObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE); // 自动处理 - 转为驼峰
        return objectMapper;
    }


}
