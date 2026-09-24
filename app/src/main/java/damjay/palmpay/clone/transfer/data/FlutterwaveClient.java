package damjay.palmpay.clone.transfer.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Flutterwave v3 card charges - the third route into the wallet.
 *
 * Flutterwave is a Nigerian processor like Paystack, but it accepts
 * international card-not-present traffic (Visa CVV only, no PIN) the way
 * ordinary online stores do, and it hands back the issuer's 3-D Secure page
 * as a plain redirect URL that can be opened in the browser.
 *
 * Two things make it different from the other two clients:
 *
 *  - the payload must be 3DES-encrypted ({"client": "..."}), using the
 *    encryption key from Settings -> API on the Flutterwave dashboard;
 *  - the charge answer lives in meta.authorization.mode: "redirect" (open
 *    the URL and verify), "otp" (validate), "pin" (re-charge with the PIN)
 *    or nothing at all (verify straight away).
 *
 * Nothing is ever persisted; keys arrive from WalletStore at runtime.
 */
public final class FlutterwaveClient {
    public interface BodyCallback {
        void onBody(JSONObject body);
    }

    private static final String BASE_URL = "https://api.flutterwave.com";
    private static final String CHARGE_PATH = "/v3/charges?type=card";
    private static final String VALIDATE_PATH = "/v3/validate-charge";
    private static final String ALGORITHM = "DESede";
    private static final String TRANSFORMATION = "DESede/ECB/PKCS5Padding";

    private final String secretKey;
    private final String encryptionKey;
    private final OkHttpClient http;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** Kept so a PIN challenge can re-send the very same charge. */
    private JSONObject lastPayload;

    public FlutterwaveClient(String secretKey, String encryptionKey) {
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey.trim();
        this.http = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public boolean isConfigured() {
        return !secretKey.isEmpty();
    }

    /** Starts a card charge. Amounts are in naira (Flutterwave's unit). */
    public void chargeCard(double amount, String cardNumber, String cvv,
                           String expiryMonth, String expiryYear,
                           String email, String fullName, String txRef,
                           BodyCallback callback) {
        JSONObject payload = new JSONObject();
        put(payload, "card_number", cardNumber);
        put(payload, "cvv", cvv);
        put(payload, "expiry_month", expiryMonth);
        put(payload, "expiry_year", expiryYear);
        put(payload, "currency", "NGN");
        put(payload, "amount", amount);
        put(payload, "email", email);
        put(payload, "fullname", fullName);
        put(payload, "tx_ref", txRef);
        // The issuer's 3DS page bounces back here; the app polls instead.
        put(payload, "redirect_url", "https://flutterwave.com/ng/");
        lastPayload = payload;
        post(CHARGE_PATH,
                requestBody(payload, secretKey, encryptionKey), callback);
    }

    /**
     * Builds the {"client": "<3DES payload>"} body Flutterwave expects.
     * Split out from the HTTP call so it stays testable off a device.
     */
    static String requestBody(JSONObject payload, String secretKey,
                              String encryptionKey) {
        JSONObject wrapper = new JSONObject();
        put(wrapper, "client",
                encryptPayload(payload.toString(), secretKey, encryptionKey));
        return wrapper.toString();
    }

    /** Re-submits the last charge carrying the PIN the issuer asked for. */
    public void submitPin(String pin, BodyCallback callback) {
        if (lastPayload == null) {
            callback.onBody(null);
            return;
        }
        JSONObject authorization = new JSONObject();
        put(authorization, "mode", "pin");
        put(authorization, "pin", pin);
        put(lastPayload, "authorization", authorization);
        post(CHARGE_PATH,
                requestBody(lastPayload, secretKey, encryptionKey), callback);
    }

    /** Submits an OTP against the charge's flw_ref. */
    public void validateCharge(String flwRef, String otp,
                               BodyCallback callback) {
        JSONObject payload = new JSONObject();
        put(payload, "otp", otp);
        put(payload, "flw_ref", flwRef);
        post(VALIDATE_PATH, payload.toString(), callback);
    }

    /** Server-side check of the final transaction state. */
    public void verifyTransaction(String id, BodyCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/v3/transactions/" + id + "/verify")
                .header("Authorization", "Bearer " + secretKey)
                .build();
        execute(request, callback);
    }

    // ---------------------------------------------------------------- crypto

    /**
     * Flutterwave's documented key rule: the first 12 characters of the
     * secret key with the last 12 characters of its MD5 hex digest appended,
     * giving exactly 24 bytes for 3DES.
     */
    static String derivedKey(String secretKey) {
        String adjusted = secretKey.replace("FLWSECK-", "");
        String first12 = adjusted.length() > 12
                ? adjusted.substring(0, 12) : adjusted;
        return first12 + last12(md5Hex(secretKey));
    }

    /** Uses the dashboard encryption key when it is already 24 bytes. */
    static String resolveKey(String secretKey, String encryptionKey) {
        byte[] given = encryptionKey.getBytes(StandardCharsets.UTF_8);
        return given.length == 24 ? encryptionKey : derivedKey(secretKey);
    }

    /**
     * 3DES-ECB + Base64, exactly as Flutterwave's own snippets do it.
     * java.util.Base64 (not android.util) so the cipher stays pure Java:
     * same NO_WRAP alphabet, but it also runs in plain unit tests.
     */
    static String encryptPayload(String plainJson, String secretKey,
                                 String encryptionKey) {
        try {
            byte[] key = resolveKey(secretKey, encryptionKey)
                    .getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM));
            byte[] encrypted = cipher.doFinal(
                    plainJson.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            return "";
        }
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            return "";
        }
    }

    private static String last12(String value) {
        return value.length() > 12
                ? value.substring(value.length() - 12) : value;
    }

    // ----------------------------------------------------------------- http

    private static void put(JSONObject target, String key, Object value) {
        try {
            target.put(key, value);
        } catch (Exception ignored) {
            // JSONObject only throws on NaN/Infinity or null keys.
        }
    }

    private void post(String path, String body, final BodyCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                        body, okhttp3.MediaType.parse("application/json")))
                .build();
        execute(request, callback);
    }

    private void execute(Request request, final BodyCallback callback) {
        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                mainHandler.post(() -> callback.onBody(null));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                JSONObject parsed = null;
                try (Response closed = response) {
                    if (closed.body() != null) {
                        parsed = new JSONObject(closed.body().string());
                    }
                } catch (Exception ignored) {
                    // Unparseable bodies are reported as null.
                }
                final JSONObject result = parsed;
                mainHandler.post(() -> callback.onBody(result));
            }
        });
    }
}
