package io.droidevs.mclub.ai.webhook.whatsapp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Meta webhook signature verification (X-Hub-Signature-256).
 *
 * <p>Verifies: sha256=<hex(hmac_sha256(appSecret, rawBody))>
 */
@Component
public class MetaCloudSignatureVerifier {

    @Value("${mclub.whatsapp.meta.signature-verification.enabled:true}")
    private boolean enabled;

    @Value("${mclub.whatsapp.meta.app-secret:}")
    private String appSecret;

    public boolean verify(HttpServletRequest request) {
        if (!enabled) return true;

        if (!StringUtils.hasText(appSecret)) {
            // misconfigured -> reject to avoid accepting spoofed webhooks
            return false;
        }

        String header = request.getHeader("X-Hub-Signature-256");
        if (!StringUtils.hasText(header) || !header.startsWith("sha256=")) {
            return false;
        }

        String expectedHex = header.substring("sha256=".length()).trim();
        byte[] bodyBytes = extractBodyBytes(request);
        if (bodyBytes == null) return false;

        String actualHex = hmacSha256Hex(appSecret, bodyBytes);
        return constantTimeEquals(expectedHex, actualHex);
    }

    private byte[] extractBodyBytes(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            return (buf.length == 0) ? null : buf;
        }
        return null;
    }

    private String hmacSha256Hex(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data);
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify webhook signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) {
            res |= a.charAt(i) ^ b.charAt(i);
        }
        return res == 0;
    }
}

