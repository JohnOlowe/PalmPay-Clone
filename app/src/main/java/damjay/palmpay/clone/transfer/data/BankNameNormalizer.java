package damjay.palmpay.clone.transfer.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import damjay.palmpay.clone.transfer.model.BankInstitution;

/**
 * Canonicalises bank names so the same institution never appears twice under
 * spelling variants ("Guaranty Trust Bank" / "GTBank", "OPay Digital Services
 * Limited" / "OPay"). Generic suffixes are stripped and a small alias table
 * maps well-known variants onto one key.
 */
public final class BankNameNormalizer {
    private BankNameNormalizer() {
        // No instances.
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toUpperCase(Locale.US)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\b(LIMITED|LTD|PLC|LLC|NIGERIA|NG|THE|OF|MICROFINANCE|"
                        + "MFB|PSB|BANK|SERVICE|SERVICES|PAYMENT|PAYMENTS|MOBILE|"
                        + "COMPANY|CO|CORPORATION|DIGITAL|FINANCE|FINANCIAL)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String canonical(String name) {
        String normalized = normalize(name);
        String[][] aliases = {
                {"GUARANTY", "GT"}, {"GTB", "GT"},
                {"SMARTCASH", "SMARTCASH"}, {"AIRTEL SMART", "SMARTCASH"},
                {"OPAY", "OPAY"}, {"PALMPAY", "PALMPAY"},
                {"MONIEPOINT", "MONIEPOINT"}, {"ACCESS", "ACCESS"},
                {"FIRST", "FIRST"}, {"UNITED", "UBA"}, {"WEMA", "WEMA"},
                {"ZENITH", "ZENITH"}, {"FIDELITY", "FIDELITY"},
                {"ECOBANK", "ECOBANK"}, {"STERLING", "STERLING"},
                {"FIRST CITY", "FCMB"}, {"FCMB", "FCMB"},
                {"KUDA", "KUDA"}, {"STANBIC", "STANBIC"},
                {"9MOBILE", "9PSB"}, {"9PAYMENT", "9PSB"},
        };
        for (String[] alias : aliases) {
            if (normalized.contains(alias[0])) {
                return alias[1];
            }
        }
        return normalized;
    }

    /** Removes duplicates by bank code first, then canonical name. */
    public static List<BankInstitution> dedupe(List<BankInstitution> banks) {
        List<BankInstitution> result = new ArrayList<>();
        Set<String> codes = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        for (BankInstitution bank : banks) {
            String code = bank.getCode() == null ? "" : bank.getCode();
            String key = canonical(bank.getName());
            if (key.isEmpty()) {
                continue;
            }
            if (!code.isEmpty() && !codes.add(code)) {
                continue;
            }
            if (!names.add(key)) {
                continue;
            }
            result.add(bank);
        }
        return result;
    }
}
