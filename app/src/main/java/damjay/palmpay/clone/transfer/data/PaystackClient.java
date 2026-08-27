package damjay.palmpay.clone.transfer.data;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import damjay.palmpay.clone.transfer.model.BankInstitution;

/**
 * Minimal Paystack client used for automatic bank retrieval, built on OkHttp.
 *
 * NUBAN check-digit arithmetic only identifies traditional 3-digit-code
 * banks, so wallet providers such as OPay or PalmPay are resolved through
 * Paystack's account-resolution endpoint. Note that, per Paystack's API,
 * the resolve endpoint expects snake_case parameters (account_number and
 * bank_code) while the institution list uses camelCase paging parameters.
 * The secret key is supplied at runtime from the Profile screen; without a
 * key the client reports itself unconfigured and the app falls back to the
 * transfer history and the NUBAN resolver.
 */
public final class PaystackClient {
    public interface BanksCallback {
        void onBanks(List<BankInstitution> banks);
    }

    public interface ResolveCallback {
        void onResolved(String accountName, BankInstitution bank);

        void onFailed();
    }

    private static final String BASE_URL = "https://api.paystack.co";

    private final String apiKey;
    private final OkHttpClient http;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile List<BankInstitution> cachedBanks;

    public PaystackClient(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.http = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** Fetches the Nigerian institution list (names + bank codes). */
    public void listBanks(final BanksCallback callback) {
        List<BankInstitution> cached = cachedBanks;
        if (cached != null) {
            callback.onBanks(cached);
            return;
        }
        HttpUrl url = HttpUrl.get(BASE_URL + "/bank").newBuilder()
                .addQueryParameter("country", "nigeria")
                .addQueryParameter("perPage", "500")
                .addQueryParameter("page", "1")
                .build();
        get(url, body -> {
            final List<BankInstitution> banks = new ArrayList<>();
            if (body != null && body.optBoolean("status")) {
                JSONArray data = body.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject bank = data.optJSONObject(i);
                        if (bank == null) {
                            continue;
                        }
                        String name = bank.optString("name", "");
                        String code = bank.optString("code", "");
                        if (!name.isEmpty() && !code.isEmpty()) {
                            banks.add(new BankInstitution(name, "", code, ""));
                        }
                    }
                }
            }
            if (!banks.isEmpty()) {
                cachedBanks = banks;
            }
            mainHandler.post(() -> callback.onBanks(banks));
        });
    }

    /** Resolves the holder name of an account at the given bank. */
    public void resolveAccount(
            String accountNumber, BankInstitution bank, final ResolveCallback callback) {
        HttpUrl url = HttpUrl.get(BASE_URL + "/bank/resolve").newBuilder()
                .addQueryParameter("account_number", accountNumber)
                .addQueryParameter("bank_code", bank.getCode())
                .build();
        get(url, body -> {
            String accountName = null;
            if (body != null && body.optBoolean("status")) {
                JSONObject data = body.optJSONObject("data");
                if (data != null) {
                    accountName = data.optString("account_name", "");
                }
            }
            final String name = accountName;
            mainHandler.post(() -> {
                if (name != null && !name.isEmpty()) {
                    callback.onResolved(name, bank);
                } else {
                    callback.onFailed();
                }
            });
        });
    }

    public interface BodyCallback {
        void onBody(JSONObject body);
    }

    private void get(HttpUrl url, final BodyCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .build();
        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException exception) {
                mainHandler.post(() -> callback.onBody(null));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                JSONObject body = null;
                try (Response closed = response) {
                    if (closed.isSuccessful() && closed.body() != null) {
                        body = new JSONObject(closed.body().string());
                    }
                } catch (Exception ignored) {
                    // Malformed payloads are reported as an empty body.
                }
                final JSONObject result = body;
                mainHandler.post(() -> callback.onBody(result));
            }
        });
    }
}
