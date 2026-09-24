package damjay.palmpay.clone.transfer.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
    /** Exactly 24 bytes, so it must be used as the 3DES key verbatim. */
    private static final String ENC_KEY_24 = "FLWSECKa1b2c3d4e5f6g7h8i";

    @Test
    public void derivedKeyIsFirst12OfSecretPlusLast12OfItsMd5()
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(SECRET.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        // MD5 renders as 32 hex characters, so the last 12 exist.
        assertEquals(32, hex.length());

        String expected =
                "FLWSECK_TEST" + hex.substring(hex.length() - 12);
        String derived = FlutterwaveClient.derivedKey(SECRET);

        assertEquals(expected, derived);
        assertEquals(24, derived.length());
        // 3DES needs exactly 24 key bytes.
        assertEquals(24, derived.getBytes(StandardCharsets.UTF_8).length);
        // The prefix really is the secret key with its FLWSECK- marker gone.
        assertTrue(derived.startsWith(SECRET.replace("FLWSECK-", "")
                .substring(0, 12)));
    }

    @Test
    public void encryptionKeyOf24BytesIsUsedVerbatim() {
        assertEquals(24, ENC_KEY_24.length());
        assertEquals(24, ENC_KEY_24.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(ENC_KEY_24,
                FlutterwaveClient.resolveKey(SECRET, ENC_KEY_24));
    }

    @Test
    public void wrongSizedEncryptionKeyFallsBackToDerivation() {
        String tooShort = ENC_KEY_24.substring(0, 23);
        String tooLong = ENC_KEY_24 + "j";
        assertEquals(23, tooShort.length());
        assertEquals(25, tooLong.length());

        String derived = FlutterwaveClient.derivedKey(SECRET);
        assertEquals(derived, FlutterwaveClient.resolveKey(SECRET, ""));
        assertEquals(derived, FlutterwaveClient.resolveKey(SECRET, tooShort));
        assertEquals(derived, FlutterwaveClient.resolveKey(SECRET, tooLong));
    }

    @Test
    public void payloadRoundTripsThrough3Des() throws Exception {
        String payload = "{\"card_number\":\"4242424242424242\",\"cvv\":\"812\"}";

        String encrypted = FlutterwaveClient.encryptPayload(payload, SECRET, "");

        assertFalse(encrypted.isEmpty());
        assertFalse(encrypted.contains("4242"));

        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(
                FlutterwaveClient.derivedKey(SECRET)
                        .getBytes(StandardCharsets.UTF_8), "DESede"));
        byte[] decrypted = cipher.doFinal(
                java.util.Base64.getDecoder().decode(encrypted));

        assertEquals(payload, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    public void differentSecretsProduceDifferentCiphertext() {
        String payload = "{\"amount\":1000}";
        String first = FlutterwaveClient.encryptPayload(payload, SECRET, "");
        String second = FlutterwaveClient.encryptPayload(payload,
                "FLWSECK_OTHER-0986a1b9b202c12cb43e7d9588228929-X", "");
        assertFalse(first.isEmpty());
        assertFalse(second.isEmpty());
        assertNotEquals(first, second);
    }

    @Test
    public void requestBodyWrapsTheEncryptedPayloadOnly() throws Exception {
        JSONObject payload = new JSONObject()
                .put("card_number", "4242424242424242")
                .put("cvv", "812")
                .put("amount", 2500.0)
                .put("currency", "NGN");

        String body = FlutterwaveClient.requestBody(payload, SECRET, "");

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
        assertEquals(2500.0, decrypted.getDouble("amount"), 0.001);
    }
}
