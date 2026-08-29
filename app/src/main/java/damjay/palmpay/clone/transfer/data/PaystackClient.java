package damjay.palmpay.clone.transfer.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;
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
 *
 * The institution list is cached in memory and persisted on-device so that
 * repeat matching-bank lookups skip the network entirely, and the OkHttp
 * dispatcher is widened so every resolve probe flies at once instead of
 * being serialized five at a time.
 */
public final class PaystackClient {
    public interface BanksCallback {
        void onBanks(List<BankInstitution> banks);
    }

    public interface ResolveCallback {
        void onResolved(String accountName, BankInstitution bank);

        void onFailed();
    }

    public interface BodyCallback {
        void onBody(JSONObject body);
    }

    private void persistBanks(List<BankInstitution> banks) {
        if (context == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray();
            for (BankInstitution bank : banks) {
                array.put(new JSONObject()
                        .put("name", bank.getName())
                        .put("code", bank.getCode()));
            }
            context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(BANKS_KEY, array.toString())
                    .apply();
        } catch (Exception ignored) {
            // A failed cache write just means the next call refetches.
        }
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
