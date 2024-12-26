package com.jian.start;

import com.jian.commons.Constants;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProtocols;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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
    static final String propertiesFileName = "client.properties";

    /***
     * ca证书名
     */
    static final String caCrtFileNameTransmit = "certs/ca.crt";

    /***
     * 证书名
     */
    static final String crtFileNameTransmit = "certs/client.crt";

    /***
     * 证书私钥
     */
    static final String crtKeyFileNameTransmit = "certs/client-pkcs8.key";


    static final String CLIENT_KEYSTORE_FILE = "certs/client.keystore";

    static final String TRUST_STORE = "certs/truststore.jks";


    /***
     * 获取文件流
     */
    private static InputStream getFileInputStream(String fileName) {
        InputStream inputStream = null;
        String filePath = dirPath + File.separator + fileName;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                inputStream = Config.class.getClassLoader().getResourceAsStream(fileName);
            } else {
                inputStream = Files.newInputStream(file.toPath());
            }
        } catch (IOException e) {
            log.error("读取文件：" + fileName + "错误！", e);
        }
        return inputStream;
    }

    /***
     * 初始化配置
     */
    public static boolean initConfig() {
        boolean suc = false;
        try (InputStream resource = getFileInputStream(propertiesFileName)) {
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
     * 传输数据端口ssl
     */
    public static boolean initTransmitPortSSL() {
        try {
            /*InputStream caCrtInputTransmit = getFileInputStream(caCrtFileNameTransmit);
            InputStream crtInputTransmit = getFileInputStream(crtFileNameTransmit);
            InputStream crtKeyInputTransmit = getFileInputStream(crtKeyFileNameTransmit);*/

            InputStream clientKeyStoreIn = getFileInputStream(CLIENT_KEYSTORE_FILE);
            InputStream trustStoreIn = getFileInputStream(TRUST_STORE);


            //和本地的https连接为单向认证
            Constants.LOCAL_SSL_CONTEXT = SslContextBuilder.forClient().build();
            //必须经过SSL双向认证


            KeyStore clientKeyStore = KeyStore.getInstance("JKS");
            clientKeyStore.load(clientKeyStoreIn, "123456".toCharArray());

            // 创建 KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientKeyStore, "123456".toCharArray());


            // 加载信任库
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(trustStoreIn, "123456".toCharArray());
            tmf.init(trustStore);


            //Constants.REMOTE_SSL_CONTEXT = SslContextBuilder.forClient().keyManager(crtInputTransmit,crtKeyInputTransmit).trustManager(caCrtInputTransmit).sslProvider(SslProvider.OPENSSL).protocols(SslProtocols.TLS_v1_3).startTls(true).build();
            Constants.REMOTE_SSL_CONTEXT = SslContextBuilder.forClient().keyManager(kmf).trustManager(tmf).sslProvider(SslProvider.OPENSSL).protocols(SslProtocols.TLS_v1_3).startTls(true).build();


            return true;
        } catch (SSLException e) {
            log.error("传输端口ssl构建错误！", e);
        }

        catch (IOException e) {
            log.error("传输端口构建SSLContext失败，ssl证书错误！", e);
        }
        catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


}
