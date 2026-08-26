package damjay.palmpay.clone.transfer.data;

import java.util.List;

import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/** Offline implementation used until the bank directory/API is connected. */
public final class LocalTransferRepository implements TransferRepository {
    @Override
    public List<TransferShortcut> getShortcuts() {
        return TransferCatalog.shortcuts();
    }

    @Override
    public List<TransferRecipient> getRecentRecipients() {
        return TransferCatalog.recentRecipients();
    }
}
