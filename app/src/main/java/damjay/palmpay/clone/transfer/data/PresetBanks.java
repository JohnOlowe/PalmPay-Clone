package damjay.palmpay.clone.transfer.data;

import java.util.Locale;

import damjay.palmpay.clone.transfer.model.BankInstitution;

/**
 * A small pre-conceived table for the institutions whose identity must never
 * depend on what a remote directory happens to return.
 *
 * When a bank sort code arrives (Paystack's bank codes or the CBN 3-digit
 * NUBAN codes) and it is listed here, the preset display name wins and the
 * returned bank name is ignored; the matching logo is always the bundled
 * local artwork. Codes that are not in the table fall through untouched,
 * and as a second line of defence the returned name is also normalised by
 * its tokens ("Paycom" still means OPay) so history rows and wallet probes
 * that carry no code at all stay consistent too.
 */
public final class PresetBanks {
    public static final class Entry {
        private final String displayName;
        private final String[] tokens;
        private final String[] codes;

        Entry(String displayName, String[] tokens, String[] codes) {
            this.displayName = displayName;
            this.tokens = tokens;
            this.codes = codes;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Entry[] ENTRIES = {
            new Entry("OPay",
                    new String[] {"opay", "paycom"},
                    new String[] {"999991", "999105"}),
            new Entry("PalmPay",
                    new String[] {"palmpay"},
                    new String[] {"999992", "100025"}),
            new Entry("SmartCash",
                    new String[] {"smartcash", "smart cash", "airtel"},
                    new String[] {"100035"}),
            new Entry("Moniepoint",
                    new String[] {"moniepoint"},
                    new String[] {"50515", "100035"}),
            new Entry("Access Bank",
                    new String[] {"access"},
                    new String[] {"011"}),
    };

    private PresetBanks() {
        // No instances.
    }

    /** Code match first (the sort code wins), then the name tokens. */
    public static Entry match(String code, String returnedName) {
        String normalizedCode = code == null ? "" : code.trim();
        if (!normalizedCode.isEmpty()) {
            for (Entry entry : ENTRIES) {
                for (String entryCode : entry.codes) {
                    if (entryCode.equals(normalizedCode)) {
                        return entry;
                    }
                }
            }
        }
        String name = returnedName == null
                ? "" : returnedName.toLowerCase(Locale.US);
        if (name.isEmpty()) {
            return null;
        }
        for (Entry entry : ENTRIES) {
            for (String token : entry.tokens) {
                if (name.contains(token)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /** The preset name for a code/name pair, or the returned name as-is. */
    public static String displayNameFor(String code, String returnedName) {
        Entry entry = match(code, returnedName);
        return entry == null ? returnedName : entry.getDisplayName();
    }

    /** True when the pair belongs to the preset table. */
    public static boolean isPreset(String code, String returnedName) {
        return match(code, returnedName) != null;
    }

    /** Returns the bank with its preset display name applied, if any. */
    public static BankInstitution apply(BankInstitution bank) {
        if (bank == null) {
            return null;
        }
        Entry entry = match(bank.getCode(), bank.getName());
        if (entry == null
                || entry.getDisplayName().equals(bank.getName())) {
            return bank;
        }
        return new BankInstitution(entry.getDisplayName(), bank.getSlug(),
                bank.getCode(), bank.getLogoUrl());
    }
}
