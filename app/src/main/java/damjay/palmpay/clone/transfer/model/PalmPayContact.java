package damjay.palmpay.clone.transfer.model;

import androidx.annotation.DrawableRes;

/** An immutable PalmPay user shown on the To PalmPay screen. */
public final class PalmPayContact {
    private final String name;
    private final String accountNumber;
    @DrawableRes
    private final int avatarRes;
    private final boolean agent;
    private final String lastTransferDate;

    public PalmPayContact(
            String name, String accountNumber, int avatarRes,
            boolean agent, String lastTransferDate) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.avatarRes = avatarRes;
        this.agent = agent;
        this.lastTransferDate = lastTransferDate;
    }

    public String getName() {
        return name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    @DrawableRes
    public int getAvatarRes() {
        return avatarRes;
    }

    public boolean isAgent() {
        return agent;
    }

    /** Null when the list (e.g. contacts) does not show dates. */
    public String getLastTransferDate() {
        return lastTransferDate;
    }
}
