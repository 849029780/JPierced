package com.jian.start;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jian.commons.Constants;
import com.jian.commons.ServerConfig;
import com.jian.transmit.ClientInfo;
import com.jian.transmit.ClientInfoSaved;
import com.jian.utils.JsonUtils;
import com.jian.utils.YamlUtils;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProtocols;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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
    static final String dataFileName = ".data";

    /***
     * 配置文件名
     */
    static final String propertiesFileName = "server.yml";

    /***
     * 证书名
     */
    public static final String crtFileName = "pierced.crt";

    /***
     * 证书私钥
     */
    public static final String crtKeyFileName = "pierced.key";

    /***
     * ca证书名
     */
    static final String caCrtFileNameTransmit = "certs/ca.crt";

    /***
     * 证书名
     */
    static final String crtFileNameTransmit = "certs/server.crt";

    /***
     * 证书私钥
     */
    static final String crtKeyFileNameTransmit = "certs/server-pkcs8.key";


    static final String SERVER_KEYSTORE = "certs/server.keystore";

    static final String TRUST_STORE = "certs/truststore.jks";


    /***
     * 获取文件流
     */
    public static InputStream getFileInputStream(String fileName, boolean isFindClsspath) {
        InputStream inputStream = null;
        String filePath = dirPath + File.separator + fileName;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                if (isFindClsspath) {
                    inputStream = Config.class.getClassLoader().getResourceAsStream(fileName);
                }
            } else {
                inputStream = Files.newInputStream(file.toPath());
            }
        } catch (IOException e) {
            log.error("读取文件：{},错误！", fileName, e);
        }
        return inputStream;
    }

    /***
     * 获取https证书绝对路径
     */
    public static String getCertAbsolutePath() {
        return dirPath + File.separator + crtFileName;
    }

    /***
     * 获取https证书key绝对路径
     */
    public static String getCertKeyAbsolutePath() {
        return dirPath + File.separator + crtKeyFileName;
    }


    /***
     * 获取文件outputsream
     */
    private static OutputStream getFileOutStream(String fileName) {
        OutputStream outputStream = null;
        String filePath = dirPath + File.separator + fileName;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("获取文件outputSteam：{},错误！", fileName, e);
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
                ObjectMapper yamlObjectMapper = YamlUtils.getYamlObjectMapper();
                Constants.CONFIG = yamlObjectMapper.readValue(resource, ServerConfig.class);
                log.info("配置文件加载完成！");
            }
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
                log.warn("{}文件不存在！暂不加载已保存的数据！", dataFileName);
                return;
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
            String dataJson = JsonUtils.JSON_PARSER.writerWithDefaultPrettyPrinter().writeValueAsString(mapSaved);

            OutputStream fileOutStream = getFileOutStream(dataFileName);
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
        try (OutputStream outputStream = getFileOutStream(propertiesFileName)) {
            String yaml = YamlUtils.getYamlObjectMapper().writeValueAsString(Constants.CONFIG);
            byte[] bytes = yaml.getBytes(StandardCharsets.UTF_8);
            outputStream.write(bytes, 0, bytes.length);
            outputStream.flush();
        } catch (IOException e) {
            log.error("保存配置文件失败！写入配置文件错误！", e);
        }
    }


    /***
     * 初始化ssl
     */
    public static boolean initLocalPortSSL() {
        if (!Constants.CONFIG.getWeb().getUseHttps()) {
            log.warn("HTTPS未启用！");
            return true;
        }
        try {
            InputStream crtInput = getFileInputStream(crtFileName, true);
            InputStream crtKeyInput = getFileInputStream(crtKeyFileName, true);
            if (Objects.isNull(crtInput) || Objects.isNull(crtKeyInput)) {
                log.warn("未找到HTTPS的SSL证书，默认停用HTTPS穿透");
                Constants.IS_ENABLE_HTTPS = Boolean.FALSE;
                return true;
            }
            Constants.SSL_LOCAL_PORT_CONTEXT = SslContextBuilder.forServer(crtInput, crtKeyInput).build();
            crtInput.close();
            crtKeyInput.close();
            log.info("已启用HTTPS！");
            return true;
        } catch (SSLException e) {
            log.error("sslContext构建错误！", e);
            Constants.IS_ENABLE_HTTPS = Boolean.FALSE;
            return false;
        } catch (IOException e) {
            log.error("sslContext构建SSL错误！", e);
            Constants.IS_ENABLE_HTTPS = Boolean.FALSE;
            return false;
        }
    }

    /***
     * 传输数据端口ssl
     */
    public static boolean initTransmitPortSSL() {
        if (!Constants.CONFIG.getTransmit().getUseSsl()) {
            log.warn("传输通道未启用ssl加密.");
            return true;
        }

        try {
            /*InputStream caCrtInputTransmit = getFileInputStream(caCrtFileNameTransmit, true);
            InputStream crtInputTransmit = getFileInputStream(crtFileNameTransmit, true);
            InputStream crtKeyInputTransmit = getFileInputStream(crtKeyFileNameTransmit, true);*/

            InputStream serverKeyStoreIn = getFileInputStream(SERVER_KEYSTORE, true);
            InputStream trustStoreIn = getFileInputStream(TRUST_STORE, true);

            //必须经过SSL双向认证


            KeyStore clientKeyStore = KeyStore.getInstance("JKS");
            clientKeyStore.load(serverKeyStoreIn, "123456".toCharArray());

            // 创建 KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientKeyStore, "123456".toCharArray());

            // 加载信任库
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(trustStoreIn, "123456".toCharArray());
            tmf.init(trustStore);


            //Constants.SSL_TRANSMIT_PORT_CONTEXT = SslContextBuilder.forServer(crtInputTransmit,crtKeyInputTransmit).trustManager(caCrtInputTransmit).clientAuth(ClientAuth.REQUIRE).protocols(SslProtocols.TLS_v1_3).sslProvider(SslProvider.OPENSSL).build();
            Constants.SSL_TRANSMIT_PORT_CONTEXT = SslContextBuilder.forServer(kmf).trustManager(tmf).clientAuth(ClientAuth.REQUIRE).protocols(SslProtocols.TLS_v1_3).sslProvider(SslProvider.OPENSSL).build();

            return true;
        } catch (SSLException e) {
            log.error("传输端口ssl构建错误！", e);
        } catch (IOException e) {
            log.error("传输端口构建SSLContext失败，ssl证书错误！", e);
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


}
