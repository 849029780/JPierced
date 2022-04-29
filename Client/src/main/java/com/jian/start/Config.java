package com.jian.start;

import ch.qos.logback.core.net.ssl.SSL;
import com.jian.commons.Constants;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProtocols;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
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
     * ca证书名
     */
    static String caCrtFileNameTransmit = "certs/ca.crt";

    /***
     * 证书名
     */
    static String crtFileNameTransmit = "certs/client.crt";

    /***
     * 证书私钥
     */
    static String crtKeyFileNameTransmit = "certs/client-pkcs8.key";


    /***
     * 获取文件流
     * @param fileName
     * @return
     */
    private static InputStream getFileInputStream(String fileName, boolean isFindClsspath) {
        InputStream inputStream = null;
        String filePath = dirPath + File.separator + fileName;
        File file = new File(filePath);
        try {
            if (!file.exists() && isFindClsspath) {
                inputStream = Config.class.getClassLoader().getResourceAsStream(fileName);
            } else {
                inputStream = Files.newInputStream(file.toPath());
            }
        } catch (IOException e) {
            log.error("加载文件：" + fileName + "错误！", e);
        }
        return inputStream;
    }

    /***
     * 初始化配置
     */
    public static boolean initConfig() {
        boolean suc = false;
        try (InputStream resource = getFileInputStream(propertiesFileName, true)) {
            if (Objects.nonNull(resource)) {
                Constants.CONFIG.load(resource);
                log.info("配置文件加载成功！");
            }
            suc = initTransmitPortSSL();
        } catch (IOException e) {
            log.error("加载config.properties文件错误！", e);
        }
        return suc;
    }

    /***
     * 初始化配置
     */
    /*public static boolean initConfig() {
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
    }*/


    /***
     * 传输数据端口ssl
     * @return
     */
    public static boolean initTransmitPortSSL() {
        InputStream caCrtInputTransmit = getFileInputStream(caCrtFileNameTransmit, true);
        InputStream crtInputTransmit = getFileInputStream(crtFileNameTransmit, true);
        InputStream crtKeyInputTransmit = getFileInputStream(crtKeyFileNameTransmit, true);
        try {
            //和本地的https连接为单向认证
            Constants.LOCAL_SSL_CONTEXT = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            //必须经过SSL双向认证
            Constants.REMOTE_SSL_CONTEXT = SslContextBuilder.forClient().keyManager(crtInputTransmit, crtKeyInputTransmit).protocols(SslProtocols.TLS_v1_3).trustManager(caCrtInputTransmit).build();
            return true;
        } catch (SSLException e) {
            log.error("传输端口ssl构建错误！", e);
        }
        return false;
    }


}
