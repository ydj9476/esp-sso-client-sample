package com.example.sso.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.core.io.Resource;

/**
 * PEM 파일에서 RSA 키를 읽는다.
 * <ul>
 *   <li>공개키: {@code -----BEGIN PUBLIC KEY-----} (X.509)</li>
 *   <li>개인키: {@code -----BEGIN PRIVATE KEY-----} (PKCS#8)</li>
 * </ul>
 */
public final class PemKeys {

    private PemKeys() {
    }

    public static PublicKey readPublicKey(Resource resource) {
        byte[] der = readDer(resource, "PUBLIC KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("공개키 파싱 실패: " + resource, e);
        }
    }

    public static PrivateKey readPrivateKey(Resource resource) {
        byte[] der = readDer(resource, "PRIVATE KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("개인키 파싱 실패: " + resource, e);
        }
    }

    private static byte[] readDer(Resource resource, String type) {
        String pem;
        try (InputStream in = resource.getInputStream()) {
            pem = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new IllegalStateException("키 파일을 읽을 수 없음: " + resource, e);
        }
        String body = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
