package damjay.palmpay.clone.transfer.data;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Live probe of the supplied Paystack TEST key, executed on the CI runner
 * (the only place with outbound network): does account 0001234567 at
 * Guaranty Trust Bank (058) resolve to a holder name in test mode?
 * Green run = a name came back; red run = it did not (message says why).
 */
public class PaystackLiveProbeTest {
    private static final String KEY =
            "sk_test_59907d1708ffa4117e75f560fe2990ecd3c138f3";

    // Ignored after the first live run (33015542579, red): test mode returned
    // no holder name. Re-enable to probe again once a live key is available.
    @Ignore("diagnostic already captured; see CI run 33015542579")
    @Test
    public void probeAccountResolution() throws Exception {
        JSONObject banks = get("/bank?country=nigeria&perPage=500&page=1");
        assertTrue("bank list call failed: " + banks,
                banks != null && banks.optBoolean("status"));
        JSONArray data = banks.getJSONArray("data");
        assertTrue("bank list empty", data.length() > 0);

        String gtbCode = "058";
        for (int i = 0; i < data.length(); i++) {
            JSONObject bank = data.getJSONObject(i);
            if (bank.optString("name", "").toLowerCase().contains("guaranty")) {
                gtbCode = bank.optString("code", "058");
                break;
            }
        }

        JSONObject resolve = get("/bank/resolve?accountNumber=0001234567&bankCode="
                + gtbCode);
        String detail = resolve == null ? "null response" : resolve.toString();
        assertTrue("no holder name for 0001234567 @" + gtbCode + " -> " + detail,
                resolve != null
                        && resolve.optBoolean("status")
                        && !resolve.optJSONObject("data")
                                .optString("account_name", "").isEmpty());
    }

    private JSONObject get(String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("https://api.paystack.co" + path).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Authorization", "Bearer " + KEY);
            if (connection.getResponseCode() != 200) {
                return null;
            }
            try (InputStream stream = connection.getInputStream()) {
                java.io.ByteArrayOutputStream out =
                        new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return new JSONObject(out.toString("UTF-8"));
            }
        } finally {
            connection.disconnect();
        }
    }
}
