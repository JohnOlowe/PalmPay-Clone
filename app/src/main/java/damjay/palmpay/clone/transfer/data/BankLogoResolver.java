package damjay.palmpay.clone.transfer.data;

import java.util.Locale;

import androidx.annotation.DrawableRes;

import damjay.palmpay.clone.R;

/** Resolves logo sources for providers already present in recent transfers. */
public final class BankLogoResolver {
    private BankLogoResolver() {
        // No instances.
    }

    public static String forProvider(String provider) {
        String value = normalize(provider);
        if (value.contains("opay")) {
            return "https://dl.svgcdn.com/png/arcticons/opay-800.png";
        }
        if (value.contains("moniepoint")) {
            return "https://logo.clearbit.com/moniepoint.com";
        }
        if (value.contains("smartcash")) {
            return "https://res.cloudinary.com/dweovytuc/image/upload/f_auto,q_auto/v1731834962/Airtel_Smartcash_PSB_oxqa5c.png";
        }
        return "";
    }

    @DrawableRes
    public static int fallbackForProvider(String provider) {
        String value = normalize(provider);
        if (value.contains("opay")) {
            return R.drawable.recipient_opay;
        }
        if (value.contains("moniepoint")) {
            return R.drawable.recipient_moniepoint;
        }
        if (value.contains("smartcash")) {
            return R.drawable.recipient_smartcash;
        }
        return R.drawable.ic_bank_building;
    }

    private static String normalize(String provider) {
        return provider == null ? "" : provider.toLowerCase(Locale.US);
    }
}
