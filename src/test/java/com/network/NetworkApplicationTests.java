package com.network;


import com.network.util.CertificateManager;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.encoders.Base64Encoder;
import sun.security.provider.X509Factory;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import static java.lang.System.out;

class NetworkApplicationTests {

    public static void main(String[] args) throws CertificateEncodingException, IOException {
        Map.Entry<PrivateKey, X509Certificate> certificate = CertificateManager.getInstance().getCertificate("www.jianshu.com");
        out.println(convertToPem(certificate.getValue()));
//        out.println(convertToPem(certificate.getKey()));
//        out.println(convertToPem(CertificateManager.getInstance().getServerKeyPair().getPublic()));
    }

    void contextLoads() {
    }

    public static  String convertToPem(Object x509Cert) throws IOException {
        StringWriter sw = new StringWriter();
        try (PEMWriter pw = new PEMWriter(sw)) {
            pw.writeObject(x509Cert);
        }
        return sw.toString();
    }

}
