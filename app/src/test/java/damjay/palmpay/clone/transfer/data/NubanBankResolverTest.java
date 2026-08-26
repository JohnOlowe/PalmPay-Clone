package damjay.palmpay.clone.transfer.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import damjay.palmpay.clone.transfer.model.BankInstitution;

/** Vectors come from the CBN NUBAN circular examples (First Bank 011). */
public class NubanBankResolverTest {

    @Test
    public void validatesCircularExamples() {
        assertTrue(NubanBankResolver.isValidNuban("011", "0000014579"));
        assertTrue(NubanBankResolver.isValidNuban("011", "0000000220"));
    }

    @Test
    public void rejectsWrongBankOrTypo() {
        assertFalse(NubanBankResolver.isValidNuban("044", "0000014579"));
        assertFalse(NubanBankResolver.isValidNuban("011", "0000014578"));
        assertFalse(NubanBankResolver.isValidNuban("011", "000001457"));
        assertFalse(NubanBankResolver.isValidNuban("0110", "0000014579"));
    }

    @Test
    public void discoversBankFromAccountNumber() {
        List<BankInstitution> banks = Arrays.asList(
                new BankInstitution("First Bank Of Nigeria", "first-bank", "011", ""),
                new BankInstitution("Access Bank", "access", "044", ""),
                new BankInstitution("Guaranty Trust Bank", "gtb", "058", ""));
        List<BankInstitution> matches =
                NubanBankResolver.candidateBanks("0000014579", banks);
        assertEquals(1, matches.size());
        assertEquals("First Bank Of Nigeria", matches.get(0).getName());
    }
}
