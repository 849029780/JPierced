package com.jian.start;

import com.jian.commons.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/05
 */
@Slf4j
public class Config {

    //获取当前jar包所在路径
    static String dirPath = System.getProperty("user.dir");

    //配置文件路径，默认在该jar包下
    static String propertiesPath = dirPath + File.separator + "client.properties";

    /***
     * 初始化配置
     */
    public static boolean initConfig() {
        File file = new File(propertiesPath);
        if (!file.exists()) {
            log.error("当前目录不存在:{}文件，启动失败！", propertiesPath);
            return false;
        }
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            Constants.CONFIG.load(inputStream);
            log.info("配置文件加载成功！");
            return true;
        } catch (IOException e) {
            log.error("加载config.properties文件错误！", e);
            return false;
        }
    }

}
