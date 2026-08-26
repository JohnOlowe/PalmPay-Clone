package damjay.palmpay.clone.transfer.data;

import java.util.ArrayList;
import java.util.List;

import damjay.palmpay.clone.transfer.model.BankInstitution;

/**
 * Implements the Central Bank of Nigeria NUBAN check-digit algorithm and uses
 * it to discover which institution a 10-digit account number belongs to.
 *
 * The approved format is ABC-DEFGHIJKL-M where ABC is the 3-digit bank code,
 * DEFGHIJKL the serial and M the check digit computed as
 * 10 - ((sum of the 12 payload digits weighted 3,7,3,3,7,3,3,7,3,3,7,3) mod 10),
 * with a result of 10 stored as 0. By replaying the algorithm for every known
 * bank code we can tell which bank an account number is attached to.
 */
public final class NubanBankResolver {
    private static final int[] WEIGHTS = {3, 7, 3, 3, 7, 3, 3, 7, 3, 3, 7, 3};

    private NubanBankResolver() {
        // No instances.
    }

    /** True when the account's check digit validates against the bank code. */
    public static boolean isValidNuban(String bankCode, String accountNumber) {
        if (bankCode == null || accountNumber == null
                || bankCode.length() != 3 || accountNumber.length() != 10) {
            return false;
        }
        for (int i = 0; i < 12; i++) {
            char c = (i < 3) ? bankCode.charAt(i) : accountNumber.charAt(i - 3);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return expectedCheckDigit(bankCode, accountNumber)
                == accountNumber.charAt(9) - '0';
    }

    /** Banks whose 3-digit code validates the supplied account number. */
    public static List<BankInstitution> candidateBanks(
            String accountNumber, List<BankInstitution> banks) {
        List<BankInstitution> matches = new ArrayList<>();
        if (accountNumber == null || accountNumber.length() != 10 || banks == null) {
            return matches;
        }
        for (BankInstitution bank : banks) {
            String code = bank.getCode();
            if (code != null && code.length() == 3 && isValidNuban(code, accountNumber)) {
                matches.add(bank);
            }
        }
        return matches;
    }

    private static int expectedCheckDigit(String bankCode, String accountNumber) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            char c = (i < 3) ? bankCode.charAt(i) : accountNumber.charAt(i - 3);
            sum += (c - '0') * WEIGHTS[i];
        }
        int remainder = sum % 10;
        int check = 10 - remainder;
        return check == 10 ? 0 : check;
    }
}
