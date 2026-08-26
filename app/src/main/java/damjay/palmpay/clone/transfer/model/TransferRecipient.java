package damjay.palmpay.clone.transfer.model;

/** Display data for a recent transfer recipient. */
public final class TransferRecipient {
    private final String name;
    private final String accountNumber;
    private final String provider;
    private final String lastTransferDate;

    public TransferRecipient(
            String name,
            String accountNumber,
            String provider,
            String lastTransferDate) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.provider = provider;
        this.lastTransferDate = lastTransferDate;
    }

    public String getName() {
        return name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getProvider() {
        return provider;
    }

    public String getLastTransferDate() {
        return lastTransferDate;
    }
}
