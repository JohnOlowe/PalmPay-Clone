package damjay.palmpay.clone.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Small persistent wallet data source shared by the home, profile, and amount screens.
 * It deliberately stores only a demo balance; no payment credentials are persisted.
 */
public final class WalletStore {
    private static final String PREFERENCES_NAME = "palmpay_clone_wallet";
    private static final String BALANCE_KEY = "available_balance";
    private static final String DEFAULT_BALANCE = "0.62";
    private static final String NAME_KEY = "display_name";
    private static final String DEFAULT_NAME = "JOHN";
    private static final String PAYSTACK_KEY = "paystack_api_key";
    private static final String EMAIL_KEY = "paystack_email";
    private static final String STRIPE_KEY = "stripe_api_key";
    private static final String FLUTTERWAVE_KEY = "flutterwave_api_key";
    private static final String FLUTTERWAVE_ENC_KEY = "flutterwave_enc_key";
    private static final String DEFAULT_EMAIL = "customer@email.com";

    private final SharedPreferences preferences;

    public WalletStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public String getBalanceDisplay() {
        return formatBalance(readBalance());
    }

    public String getDisplayName() {
        String name = preferences.getString(NAME_KEY, DEFAULT_NAME);
        return name == null || name.trim().isEmpty() ? DEFAULT_NAME : name.trim();
    }

    public void saveDisplayName(String name) {
        preferences.edit().putString(NAME_KEY,
                name == null ? "" : name.trim()).commit();
    }

    /** Email used as the Paystack customer for charges; editable in Profile. */
    public String getPaystackEmail() {
        String email = preferences.getString(EMAIL_KEY, DEFAULT_EMAIL);
        return email == null || email.trim().isEmpty() ? DEFAULT_EMAIL : email.trim();
    }

    public void savePaystackEmail(String email) {
        preferences.edit().putString(EMAIL_KEY,
                email == null ? "" : email.trim()).commit();
    }

    public String getStripeApiKey() {
        return preferences.getString(STRIPE_KEY, "");
    }

    public void saveStripeApiKey(String key) {
        preferences.edit().putString(STRIPE_KEY,
                key == null ? "" : key.trim()).commit();
    }

    public String getFlutterwaveApiKey() {
        return preferences.getString(FLUTTERWAVE_KEY, "");
    }

    public void saveFlutterwaveApiKey(String key) {
        preferences.edit().putString(FLUTTERWAVE_KEY,
                key == null ? "" : key.trim()).commit();
    }

    /** Optional 3DES key from Settings -> API; derived from the secret key
     * when it is left blank. */
    public String getFlutterwaveEncKey() {
        return preferences.getString(FLUTTERWAVE_ENC_KEY, "");
    }

    public void saveFlutterwaveEncKey(String key) {
        preferences.edit().putString(FLUTTERWAVE_ENC_KEY,
                key == null ? "" : key.trim()).commit();
    }

    public String getPaystackApiKey() {
        return preferences.getString(PAYSTACK_KEY, "");
    }

    public void savePaystackApiKey(String key) {
        preferences.edit().putString(PAYSTACK_KEY,
                key == null ? "" : key.trim()).commit();
    }

    public boolean saveBalance(String userInput) {
        BigDecimal value = parseBalance(userInput);
        if (value == null || value.signum() < 0) {
            return false;
        }
        preferences.edit()
                .putString(BALANCE_KEY, value.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .commit();
        return true;
    }

    private BigDecimal readBalance() {
        BigDecimal parsed = parseBalance(preferences.getString(BALANCE_KEY, DEFAULT_BALANCE));
        return parsed == null || parsed.signum() < 0 ? new BigDecimal(DEFAULT_BALANCE) : parsed;
    }

    private BigDecimal parseBalance(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value
                .replace("₦", "")
                .replace(",", "")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String formatBalance(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return "₦" + formatter.format(value);
    }
}
