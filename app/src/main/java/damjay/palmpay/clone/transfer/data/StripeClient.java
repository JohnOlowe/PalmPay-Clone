package damjay.palmpay.clone.transfer.data;

import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Stripe card charges - the route ordinary international online stores use:
 * card-not-present on the Visa network with CVV only (no PIN); when the
 * issuer wants 3-D Secure the PaymentIntent returns requires_action with a
 * next_action URL to open in the browser.
 */
public final class StripeClient {
    public interface BodyCallback {
        void onBody(JSONObject body);
    }

    private static final String BASE_URL = "https://api.stripe.com";

    private final String apiKey;
    private final OkHttpClient http;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StripeClient(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.http = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** Creates and confirms a PaymentIntent with the raw card details. */
    public void chargeCard(long amountKobo, String cardNumber, String cvv,
                           String expiryMonth, String expiryYear,
                           String email, BodyCallback callback) {
        FormBody.Builder form = new FormBody.Builder()
                .add("amount", String.valueOf(amountKobo))
                .add("currency", "ngn")
                .add("confirm", "true")
                .add("return_url", "https://example.com/return")
                .add("description", "PalmPay wallet top-up")
                .add("payment_method_data[type]", "card")
                .add("payment_method_data[card][number]", cardNumber)
                .add("payment_method_data[card][cvc]", cvv)
                .add("payment_method_data[card][exp_month]", expiryMonth)
                .add("payment_method_data[card][exp_year]", expiryYear)
                .add("receipt_email", email);
        post("/v1/payment_intents", form.build(), callback);
    }

    /** Polls a PaymentIntent after a 3-D Secure round trip. */
    public void getPaymentIntent(String id, BodyCallback callback) {
        get(BASE_URL + "/v1/payment_intents/" + id, callback);
    }

    private void post(String path, okhttp3.RequestBody body,
                      final BodyCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
        execute(request, callback);
    }

    private void get(String url, final BodyCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
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
