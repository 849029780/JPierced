package com.jian.start;

import com.jian.commons.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * 描述
 *
 * @author Jian
 * @date 2022/04/05
 */
@Slf4j
public class Config {

    /***
     * 获取当前jar包所在路径
     */
    static String dirPath = System.getProperty("user.dir");

    /***
     * 配置文件
     */
    static String propertiesFileName = "client.properties";

    /***
     * 配置文件路径，默认在该jar包下
     */
    static String propertiesPath = dirPath + File.separator + propertiesFileName;

    /***
     * 初始化配置
     */
    public static boolean initConfig() {
        File file = new File(propertiesPath);
        InputStream resource = null;
        if (!file.exists()) {
            resource = Config.class.getClassLoader().getResourceAsStream(propertiesFileName);
            if (Objects.isNull(resource)) {
                log.error("配置文件不存在:{}文件，启动失败！", propertiesPath);
                return false;
            }
        }
        try {
            if (Objects.isNull(resource)) {
                resource = Files.newInputStream(file.toPath());
            }
            Constants.CONFIG.load(resource);
            log.info("配置文件加载成功！");
            resource.close();
            return true;
        } catch (IOException e) {
            log.error("加载config.properties文件错误！", e);
            return false;
        }
    }




}
