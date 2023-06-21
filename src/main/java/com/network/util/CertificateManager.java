package com.network.util;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.Streams;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author wanghongen
 * 2023/5/25
 */
public class CertificateManager {

    /**
     * Field JCA_CERT_ID
     */
    private static final String JCA_CERT_ID = "X.509";
    /**
     * 证书缓存
     */
    private static final ConcurrentHashMap<String, X509Certificate> certificateMap = new ConcurrentHashMap<>();

    /**
     * ca证书
     */
    private X509Certificate caCert;
    /**
     * ca私钥
     */
    private PrivateKey caPriKey;
    /**
     * sslContext
     */
    private SslContext sslContext;
    /**
     * 服务端密钥
     */
    private KeyPair serverKeyPair;

    /**
     * 单例实例
     */
    private static final CertificateManager INSTANCE = new CertificateManager();

    public static CertificateManager getInstance() {
        return INSTANCE;
    }

    public CertificateManager() {
        try {
            this.initCAConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initCAConfig() throws Exception {
        //客户端sslContext
        sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        serverKeyPair = genKeyPair();
        //从项目目录加入ca根证书
        CertificateFactory cf = CertificateFactory.getInstance(JCA_CERT_ID);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        this.caCert = (X509Certificate) cf.generateCertificate(classLoader.getResourceAsStream("ca.crt"));

        //从项目目录加入ca私钥
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try (InputStream inputStream = classLoader.getResourceAsStream("ca_private.der")) {
            Objects.requireNonNull(inputStream, "ca_private.der证书私钥不能文件不存在");
            byte[] bytes = Streams.readAll(inputStream);
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(bytes);
            this.caPriKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        }

    }

    public SslContext getClientSslContext() {
        return sslContext;
    }

    /**
     * 获取域名自签名证书
     */
    public Map.Entry<PrivateKey, X509Certificate> getCertificate(String host) {
        X509Certificate x509Certificate = certificateMap.get(host);

        if (x509Certificate != null) {
            return new AbstractMap.SimpleEntry<>(serverKeyPair.getPrivate(), x509Certificate);
        }

        try {
            String issuer = getIssuerByCert(caCert);
            Date start = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
            Date end = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));
            X509Certificate certificate = generate(serverKeyPair.getPublic(), caPriKey, issuer, start, end, host);
            certificateMap.put(host, certificate);
            return new AbstractMap.SimpleEntry<>(serverKeyPair.getPrivate(), certificate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成证书
     */
    private static X509Certificate generate(PublicKey serverPubKey, PrivateKey caPriKey, String issuer, Date certStartTime, Date certEndTime, String host) throws Exception {

        //根据CA证书subject来动态生成目标服务器证书的issuer和subject
        String subject = "C=CN, ST=BJ, L=BJ, O=network, OU=Proxy, CN=" + host;

        JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(new X500Name(issuer),
                BigInteger.valueOf(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(9999)),
                certStartTime,
                certEndTime,
                new X500Name(subject),
                serverPubKey);
        //SAN扩展证书支持的域名，否则浏览器提示证书不安全
        GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, host));
        jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);

        //SHA256 用SHA1浏览器可能会提示证书不安全
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);
        return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
    }

    /**
     * 生成RSA公私密钥对,长度为2048
     */
    private static KeyPair genKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        caKeyPairGen.initialize(2048, new SecureRandom());
        return caKeyPairGen.genKeyPair();
    }


    /**
     * 获取证书中的Issuer信息
     */
    private static String getIssuerByCert(X509Certificate certificate) {
        //读出来顺序是反的需要反转下
        String[] split = certificate.getIssuerX500Principal().toString().split(", ");
        StringBuilder sb = new StringBuilder();
        for (int i = split.length - 1; i >= 0; i--) {
            sb.append(split[i]);
            if (i != 0) sb.append(", ");
        }
        return sb.toString();
    }
}
