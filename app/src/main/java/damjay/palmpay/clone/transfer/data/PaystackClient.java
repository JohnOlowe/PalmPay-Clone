package damjay.palmpay.clone.transfer.data;

import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import damjay.palmpay.clone.BuildConfig;
import damjay.palmpay.clone.transfer.model.BankInstitution;

/**
 * Minimal Paystack client used for automatic bank retrieval.
 *
 * NUBAN check-digit arithmetic only identifies traditional 3-digit-code
 * banks, so wallet providers such as OPay or PalmPay are resolved through
 * Paystack's free account-resolution endpoint: the account number is probed
 * against the institution list until a bank validates it, which also returns
 * the account holder's name. The secret key is injected at build time via
 * PAYSTACK_API_KEY in local.properties or the environment; without a key the
 * client simply reports itself as unconfigured and the app falls back to the
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
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<BankInstitution> cachedBanks;

    public PaystackClient() {
        this(BuildConfig.PAYSTACK_API_KEY);
    }

    public PaystackClient(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** Fetches the Nigerian institution list (names + bank codes). */
    public void listBanks(BanksCallback callback) {
        if (cachedBanks != null) {
            callback.onBanks(cachedBanks);
            return;
        }
        executor.execute(() -> {
            final List<BankInstitution> banks = new ArrayList<>();
            try {
                JSONObject body = get("/bank?country=nigeria&perPage=500&page=1");
                if (body != null && body.optBoolean("status")) {
                    JSONArray data = body.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject bank = data.getJSONObject(i);
                        String name = bank.optString("name", "");
                        String code = bank.optString("code", "");
                        if (!name.isEmpty() && !code.isEmpty()) {
                            banks.add(new BankInstitution(name, "", code, ""));
                        }
                    }
                }
            } catch (Exception ignored) {
                // Network or parsing failure: report an empty list.
            }
            if (!banks.isEmpty()) {
                cachedBanks = banks;
            }
            mainHandler.post(() -> callback.onBanks(banks));
        });
    }

    /** Resolves the holder name of an account at the given bank. */
    public void resolveAccount(
            final String accountNumber, final BankInstitution bank,
            final ResolveCallback callback) {
        executor.execute(() -> {
            String accountName = null;
            try {
                JSONObject body = get("/bank/resolve?accountNumber="
                        + accountNumber + "&bankCode=" + bank.getCode());
                if (body != null && body.optBoolean("status")) {
                    accountName = body.getJSONObject("data")
                            .optString("account_name", "");
                }
            } catch (Exception ignored) {
                // A failed probe simply means "not this bank".
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

    public void close() {
        executor.shutdownNow();
    }

    private JSONObject get(String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL(BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) {
                return null;
            }
            try (InputStream stream = connection.getInputStream()) {
                return new JSONObject(new String(
                        readAll(stream), java.nio.charset.StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream stream) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
