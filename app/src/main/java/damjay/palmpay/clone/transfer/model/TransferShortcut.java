package damjay.palmpay.clone.transfer.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** A reusable shortcut displayed below the transfer form. */
public final class TransferShortcut {
    @StringRes
    private final int titleRes;
    @DrawableRes
    private final int iconRes;

    public TransferShortcut(@StringRes int titleRes, @DrawableRes int iconRes) {
        this.titleRes = titleRes;
        this.iconRes = iconRes;
    }

    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }
}
