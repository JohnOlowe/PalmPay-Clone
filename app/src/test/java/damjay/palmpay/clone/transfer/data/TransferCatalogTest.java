package damjay.palmpay.clone.transfer.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/** Contract tests for the local transfer data shown by the reference screen. */
public class TransferCatalogTest {

    @Test
    public void transferShortcutsMatchTheFourReferenceActions() {
        List<TransferShortcut> shortcuts = TransferCatalog.shortcuts();

        assertEquals(4, shortcuts.size());
        assertEquals(R.string.schedule_transfer, shortcuts.get(0).getTitleRes());
        assertEquals(R.string.success_rate, shortcuts.get(1).getTitleRes());
        assertEquals(R.string.transfer_settings, shortcuts.get(2).getTitleRes());
        assertEquals(R.string.withdraw_cash, shortcuts.get(3).getTitleRes());
        for (TransferShortcut shortcut : shortcuts) {
            assertNotNull(shortcut);
            assertFalse(shortcut.getIconRes() == 0);
        }
    }

    @Test
    public void recentRecipientsPreserveTheReferenceOrdering() {
        List<TransferRecipient> recipients = TransferCatalog.recentRecipients();

        assertEquals(10, recipients.size());
        assertEquals("JOHN OLUWADAMILARE OLOWE", recipients.get(0).getName());
        assertEquals("9112413798", recipients.get(0).getAccountNumber());
        assertEquals("AISHAT OPEYEMI LATEEF", recipients.get(9).getName());
        for (TransferRecipient recipient : recipients) {
            assertFalse(recipient.getName().isEmpty());
            assertEquals(10, recipient.getAccountNumber().length());
            assertFalse(recipient.getProvider().isEmpty());
            assertFalse(recipient.getLastTransferDate().isEmpty());
        }
    }
}
