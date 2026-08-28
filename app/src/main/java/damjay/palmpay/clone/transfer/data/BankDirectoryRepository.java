package damjay.palmpay.clone.transfer.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import damjay.palmpay.clone.transfer.model.BankInstitution;

/**
 * Loads the Nigerian bank directory and its logo URLs without blocking the UI.
 * The local catalogue is deliberately retained as a fallback for offline use.
 */
public final class BankDirectoryRepository {
    public static final String DIRECTORY_URL =
            "https://jsanwo64.github.io/Nigeria-Banks-Logo-API/Banks.json";

    private static final int CONNECT_TIMEOUT_MS = 12_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onBanksLoaded(List<BankInstitution> banks, boolean fromNetwork);
    }

    public void load(Callback callback) {
        executor.execute(() -> {
            List<BankInstitution> banks = Collections.emptyList();
            boolean fromNetwork = false;
            try {
                banks = fetchOnlineBanks();
                fromNetwork = !banks.isEmpty();
            } catch (Exception ignored) {
                // A bank picker should still be useful without connectivity.
            }
            if (banks.isEmpty()) {
                banks = fallbackBanks();
            }
            List<BankInstitution> result = BankNameNormalizer.dedupe(banks);
            boolean online = fromNetwork;
            mainHandler.post(() -> callback.onBanksLoaded(result, online));
        });
    }

    public void close() {
        executor.shutdownNow();
    }

    private List<BankInstitution> fetchOnlineBanks() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(DIRECTORY_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                return Collections.emptyList();
            }
            StringBuilder json = new StringBuilder();
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }
            return parseBanks(new JSONArray(json.toString()));
        } finally {
            connection.disconnect();
        }
    }

    private List<BankInstitution> parseBanks(JSONArray array) {
        List<BankInstitution> banks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject object = array.optJSONObject(index);
            if (object == null) {
                continue;
            }
            String name = clean(object.optString("name"));
            String slug = clean(object.optString("slug"));
            String code = clean(object.optString("code"));
            String logo = clean(object.optString("logo"));
            if (name.isEmpty()) {
                continue;
            }
            String key = name.toLowerCase(Locale.US) + "|" + code;
            if (seen.add(key)) {
                banks.add(new BankInstitution(name, slug, code, safeHttpsUrl(logo)));
            }
        }
        return banks;
    }

    private String safeHttpsUrl(String url) {
        return url.startsWith("https://") ? url : "";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public List<BankInstitution> fallbackBanks() {
        String logoBase = "https://supermx1.github.io/nigerian-banks-api/logos/";
        String[][] entries = {
                {"5TT MFB", "5tt-mfb", "51364"},
                {"78 Finance Company Limited", "78-finance-company-ltd-bank78", "110072"},
                {"9 PSB", "9mobile-9payment-service-bank-ng", "120001"},
                {"9jaPay", "9japay", "120001"},
                {"AAA FINANCE", "aaa-finance", "51265"},
                {"AACB MFB", "aacb-mfb", "51289"},
                {"AB Microfinance Bank", "ab-microfinance-bank", "090270"},
                {"9mobile 9Payment Service Bank", "9mobile-9payment-service-bank-ng", "120001"},
                {"Access Bank", "access-bank", "044"},
                {"ALAT by WEMA", "alat-by-wema", "035A"},
                {"Carbon", "carbon", "100026"},
                {"Ecobank Nigeria", "ecobank-nigeria", "050"},
                {"FCMB", "fcmb", "214"},
                {"Fidelity Bank", "fidelity-bank", "070"},
                {"First Bank of Nigeria", "first-bank-of-nigeria", "011"},
                {"First City Monument Bank", "fcmb", "214"},
                {"Globus Bank", "globus-bank", "103"},
                {"GoMoney", "gomoney", "100022"},
                {"Guaranty Trust Bank", "guaranty-trust-bank", "058"},
                {"JAIZ Bank", "jaiz-bank", "301"},
                {"Keystone Bank", "keystone-bank", "082"},
                {"Kuda Microfinance Bank", "kuda-microfinance-bank", "090267"},
                {"Lotus Bank", "lotus-bank", "303"},
                {"Moniepoint Microfinance Bank", "moniepoint-microfinance-bank", "090405"},
                {"OPay", "opay", "999992"},
                {"Palmpay", "palmpay", "999991"},
                {"Parallex Bank", "parallex-bank", "104"},
                {"Polaris Bank", "polaris-bank", "076"},
                {"Premium Trust Bank", "premium-trust-bank", "105"},
                {"Providus Bank", "providus-bank", "101"},
                {"Stanbic IBTC Bank", "stanbic-ibtc-bank", "221"},
                {"Sterling Bank", "sterling-bank", "232"},
                {"Union Bank of Nigeria", "union-bank-of-nigeria", "032"},
                {"United Bank for Africa", "united-bank-for-africa", "033"},
                {"Unity Bank", "unity-bank", "215"},
                {"VFD Microfinance Bank", "vfd-microfinance-bank", "566"},
                {"Wema Bank", "wema-bank", "035"},
                {"Zenith Bank", "zenith-bank", "057"}
        };
        List<BankInstitution> banks = new ArrayList<>();
        for (String[] entry : entries) {
            banks.add(new BankInstitution(
                    entry[0], entry[1], entry[2], logoBase + entry[1] + ".png"));
        }
        return banks;
    }
}
