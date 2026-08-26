package damjay.palmpay.clone.transfer.data;

import java.util.List;

import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/** Data boundary for the transfer screen. A network implementation can replace the local one. */
public interface TransferRepository {
    List<TransferShortcut> getShortcuts();

    List<TransferRecipient> getRecentRecipients();
}
