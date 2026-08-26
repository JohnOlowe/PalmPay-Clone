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
        return "";
    }

    @DrawableRes
    public static int fallbackForProvider(String provider) {
        String value = normalize(provider);
        if (value.contains("opay")) {
            return R.drawable.ic_opay_logo;
        }
        if (value.contains("moniepoint")) {
            return R.drawable.ic_moniepoint_logo;
        }
        return R.drawable.ic_bank_building;
    }

    private static String normalize(String provider) {
        return provider == null ? "" : provider.toLowerCase(Locale.US);
    }
}
