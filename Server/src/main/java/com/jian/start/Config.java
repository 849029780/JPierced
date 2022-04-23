package com.jian.start;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.ClientInfoSaved;
import com.jian.utils.JsonUtils;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
     * 数据文件路径，默认在该jar包下
     */
    static String dataFileName = ".data";

    /***
     * 配置文件名
     */
    static String propertiesFileName = "server.properties";

    /***
     * 证书名
     */
    static String crtFileName = "pierced.crt";

    /***
     * 证书私钥
     */
    static String crtKeyFileName = "pierced.key";

    /***
     * 证书名
     */
    static String crtFileNameTransmit = "server.crt";

    /***
     * 证书私钥
     */
    static String crtKeyFileNameTransmit = "server.pkcs8.key";


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
     * 获取文件outputsream
     * @param fileName
     * @return
     */
    private static OutputStream getFileOutStream(String fileName, boolean notFoundCreateNow) {
        OutputStream outputStream = null;
        String filePath = dirPath + File.separator + fileName;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                if (notFoundCreateNow) {
                    file.createNewFile();
                    outputStream = Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE);
                }
            } else {
                outputStream = Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            log.error("获取文件outputSteam：" + fileName + "错误！", e);
        }
        return outputStream;
    }

    /***
     * 初始化配置
     */
    public static boolean initConfig() {
        boolean suc = false;
        try (InputStream resource = getFileInputStream(propertiesFileName, true)) {
            if (Objects.nonNull(resource)) {
                Constants.CONFIG.load(resource);
            }
            if (StringUtil.isNullOrEmpty(Constants.CONFIG.getProperty(Constants.WEB_PORT_PROPERTY))) {
                Constants.CONFIG.setProperty(Constants.WEB_PORT_PROPERTY, Constants.DEF_WEB_PORT);
            }
            if (StringUtil.isNullOrEmpty(Constants.CONFIG.getProperty(Constants.TRANSMIT_PORT_PROPERTY))) {
                Constants.CONFIG.setProperty(Constants.TRANSMIT_PORT_PROPERTY, Constants.DEF_TRANSMIT_PORT);
            }
            log.info("配置文件加载完成！传输端口:{}，web管理端口:{}", Constants.CONFIG.getProperty(Constants.WEB_PORT_PROPERTY), Constants.CONFIG.getProperty(Constants.TRANSMIT_PORT_PROPERTY));
            suc = initLocalPortSSL();
            suc = initTransmitPortSSL();
        } catch (IOException e) {
            log.error("加载config.properties文件错误！", e);
        }
        return suc;
    }


    /***
     * 初始化传输服务数据
     */
    public static void initTransmitData() {
        try (InputStream dataInputStream = getFileInputStream(dataFileName, false)) {
            if (Objects.isNull(dataInputStream)) {
                throw new FileNotFoundException();
            }
            Constants.CLIENTS = JsonUtils.JSON_PARSER.readValue(dataInputStream, new TypeReference<ConcurrentHashMap<Long, ClientInfo>>() {
            });
            log.info(".data文件数据加载成功！");
        } catch (IOException e) {
            log.error("初始化读取data文件:{}错误！跳过初始化服务数据", dataFileName, e);
        }
    }

    /***
     * 保存数据
     */
    public static void saveTransmitData() {
        try {
            //简单浅copy保存的数据
            Map<Long, ClientInfoSaved> mapSaved = new ConcurrentHashMap<>(Constants.CLIENTS.size());
            for (Map.Entry<Long, ClientInfo> clientInfoEntry : Constants.CLIENTS.entrySet()) {
                Long key = clientInfoEntry.getKey();
                ClientInfo clientInfo = clientInfoEntry.getValue();
                ClientInfoSaved clientInfoSaved = new ClientInfoSaved(clientInfo);
                mapSaved.put(key, clientInfoSaved);
            }
            //序列化浅copy对象为json
            String dataJson = JsonUtils.JSON_PARSER.writeValueAsString(mapSaved);
            OutputStream fileOutStream = getFileOutStream(dataFileName, true);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutStream));
            bufferedWriter.write(dataJson);
            bufferedWriter.flush();
            bufferedWriter.close();
            fileOutStream.close();
            log.info("保存客户数据:{}，成功！", dataJson);
        } catch (JsonProcessingException e) {
            log.error("保存客户数据失败！客户数据:{}，转换为Json错误！", Constants.CLIENTS, e);
        } catch (IOException e) {
            log.error("保存客户数据失败！客户数据:{}，创建.data文件错误！", Constants.CLIENTS, e);
        }
    }

    /***
     * 保存配置文件
     */
    public static void saveProperties() {
        try (OutputStream outputStream = getFileOutStream(propertiesFileName, true)) {
            Constants.CONFIG.store(outputStream, "修改时间:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            outputStream.flush();
        } catch (IOException e) {
            log.error("保存配置文件失败！写入配置文件错误！", e);
        }
    }


    /***
     * 初始化ssl
     * @return
     */
    public static boolean initLocalPortSSL() {
        String enableHttps = Constants.CONFIG.getProperty(Constants.ENABLE_HTTPS_PROPERTY_NAME, "false");
        Constants.IS_ENABLE_HTTPS = Boolean.parseBoolean(enableHttps);
        if (Constants.IS_ENABLE_HTTPS) {
            InputStream crtInput = getFileInputStream(crtFileName, true);
            InputStream crtKeyInput = getFileInputStream(crtKeyFileName, true);
            try {
                Constants.SSL_LOCAL_PORT_CONTEXT = SslContextBuilder.forServer(crtInput, crtKeyInput).build();
                return true;
            } catch (SSLException e) {
                log.error("sslContext构建错误！", e);
                Constants.IS_ENABLE_HTTPS = false;
                return false;
            }
        }
        return true;
    }

    /***
     * 传输数据端口ssl
     * @return
     */
    public static boolean initTransmitPortSSL() {
        InputStream crtInputTransmit = getFileInputStream(crtFileNameTransmit, true);
        InputStream crtKeyInputTransmit = getFileInputStream(crtKeyFileNameTransmit, true);
        try {
            Constants.SSL_TRANSMIT_PORT_CONTEXT = SslContextBuilder.forServer(crtInputTransmit, crtKeyInputTransmit).build();
            return true;
        } catch (SSLException e) {
            log.error("传输端口ssl构建错误！", e);
        }
        return false;
    }


}
