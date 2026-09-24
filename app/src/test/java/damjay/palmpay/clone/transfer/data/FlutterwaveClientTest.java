package damjay.palmpay.clone.transfer.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Flutterwave wants the charge payload 3DES-encrypted before it is sent, so
 * the key rule and the cipher are covered here instead of only on a device.
 */
public class FlutterwaveClientTest {

    private static final String SECRET =
            "FLWSECK_TEST-0986a1b9b202c12cb43e7d9588228929-X";

    @Test
    public void derivedKeyIsFirst12OfSecretPlusLast12OfItsMd5()
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(SECRET.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        String expected =
                "FLWSECK_TEST" + hex.substring(hex.length() - 12);

        String derived = FlutterwaveClient.derivedKey(SECRET);

        assertEquals(expected, derived);
        // 3DES needs exactly 24 key bytes.
        assertEquals(24, derived.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    public void encryptionKeyOf24BytesIsUsedVerbatim() {
        String given = "FLWSECKa1b2c3d4e5f6g7h8";
        assertEquals(24, given.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(given, FlutterwaveClient.resolveKey(SECRET, given));
    }

    @Test
    public void wrongSizedEncryptionKeyFallsBackToDerivation() {
        assertEquals(FlutterwaveClient.derivedKey(SECRET),
                FlutterwaveClient.resolveKey(SECRET, ""));
        assertEquals(FlutterwaveClient.derivedKey(SECRET),
                FlutterwaveClient.resolveKey(SECRET, "FLWENC-32-hex-char-value-x"));
    }

    @Test
    public void payloadRoundTripsThrough3Des() throws Exception {
        String payload = "{\"card_number\":\"4242424242424242\",\"cvv\":\"812\"}";

        String encrypted = FlutterwaveClient.encryptPayload(payload, SECRET, "");

        assertFalse(encrypted.isEmpty());
        assertFalse(encrypted.contains("4242"));

        // The client encodes with android.util.Base64 (NO_WRAP), which is the
        // same alphabet as the JDK's basic encoder, so decode with that here.
        byte[] cipherText = java.util.Base64.getDecoder().decode(encrypted);
        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
                FlutterwaveClient.derivedKey(SECRET)
                        .getBytes(StandardCharsets.UTF_8), "DESede"));

        assertEquals(payload, new String(
                cipher.doFinal(cipherText), StandardCharsets.UTF_8));
    }

    @Test
    public void differentSecretsProduceDifferentCiphertext() {
        String payload = "{\"amount\":1000}";
        String first = FlutterwaveClient.encryptPayload(payload, SECRET, "");
        String second = FlutterwaveClient.encryptPayload(payload,
                "FLWSECK_OTHER-0986a1b9b202c12cb43e7d9588228929-X", "");
        assertNotEquals(first, second);
    }

    @Test
    public void requestBodyWrapsTheEncryptedPayloadOnly() throws Exception {
        JSONObject payload = new JSONObject()
                .put("card_number", "4242424242424242")
                .put("cvv", "812")
                .put("amount", 2500.0)
                .put("currency", "NGN");

        String body =
                FlutterwaveClient.requestBody(payload, SECRET, "");

        JSONObject wrapper = new JSONObject(body);
        assertTrue(wrapper.has("client"));
        assertEquals(1, wrapper.length());
        assertFalse(wrapper.getString("client").contains("4242"));

        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
                FlutterwaveClient.derivedKey(SECRET)
                        .getBytes(StandardCharsets.UTF_8), "DESede"));
        JSONObject decrypted = new JSONObject(new String(cipher.doFinal(
                java.util.Base64.getDecoder()
                        .decode(wrapper.getString("client"))),
                StandardCharsets.UTF_8));

        assertEquals("4242424242424242", decrypted.getString("card_number"));
        assertEquals("812", decrypted.getString("cvv"));
        assertEquals("NGN", decrypted.getString("currency"));
    }
}
