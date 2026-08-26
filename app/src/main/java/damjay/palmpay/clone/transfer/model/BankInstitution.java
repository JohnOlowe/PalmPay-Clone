package damjay.palmpay.clone.transfer.model;

/** A bank directory entry returned by the online bank/logo catalogue. */
public final class BankInstitution {
    private final String name;
    private final String slug;
    private final String code;
    private final String logoUrl;

    public BankInstitution(String name, String slug, String code, String logoUrl) {
        this.name = name;
        this.slug = slug;
        this.code = code;
        this.logoUrl = logo;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getCode() {
        return code;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
