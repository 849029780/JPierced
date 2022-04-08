package com.jian.start;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.jian.commons.Constants;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.ClientInfoSaved;
import com.jian.utils.JsonUtils;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    //数据文件路径，默认在该jar包下
    static String dataPath = dirPath + File.separator + ".data";

    //配置文件路径，默认在该jar包下
    static String propertiesPath = dirPath + File.separator + "server.properties";

    /***
     * 初始化配置
     */
    public static void initConfig() {
        File file = new File(propertiesPath);
        if (!file.exists()) {
            log.error("当前目录不存在:{}文件，将使用默认配置启动，web端口:{}，传输端口为:{}", propertiesPath, Constants.DEF_WEB_PORT, Constants.DEF_TRANSMIT_PORT);
            Constants.CONFIG.setProperty(Constants.WEB_PORT_PROPERTY, Constants.DEF_WEB_PORT);
            Constants.CONFIG.setProperty(Constants.TRANSMIT_PORT_PROPERTY, Constants.DEF_TRANSMIT_PORT);
            return;
        }
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            Constants.CONFIG.load(inputStream);
            log.info("配置文件加载成功！");
        } catch (IOException e) {
            log.error("加载config.properties文件错误！", e);
        }
    }

    /***
     * 初始化传输服务数据
     */
    public static void initTransmitData() {
        File dataFile = new File(dataPath);
        if (!dataFile.exists()) {
            log.info("当前data文件:{}不存在，跳过初始化服务数据", dataPath);
            return;
        }
        try {
            String dataJson = Files.readString(dataFile.toPath());
            if (StringUtil.isNullOrEmpty(dataJson)) {
                log.info("当前data文件:{}为空！跳过初始化服务数据", dataPath);
                return;
            }
            Constants.CLIENTS = JsonUtils.JSON_PARSER.readValue(dataJson, new TypeReference<ConcurrentHashMap<Long, ClientInfo>>() {
            });
            log.info(".data文件数据加载成功！");
        } catch (IOException e) {
            log.error("初始化读取data文件:{}错误！跳过初始化服务数据", dataPath, e);
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
            File dataFile = new File(dataPath);
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(dataFile));
            bufferedWriter.write(dataJson);
            bufferedWriter.flush();
            bufferedWriter.close();
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
        File propertiesFile = new File(propertiesPath);
        if (!propertiesFile.exists()) {
            try {
                propertiesFile.createNewFile();
            } catch (IOException e) {
                log.error("保存配置文件失败！创建配置文件错误！", e);
                return;
            }
        }
        Path path = propertiesFile.toPath();
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.WRITE)) {
            Constants.CONFIG.store(outputStream, "修改时间:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
            outputStream.flush();
        } catch (IOException e) {
            log.error("保存配置文件失败！写入配置文件错误！", e);
        }
    }


}
