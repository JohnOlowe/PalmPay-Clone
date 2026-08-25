package damjay.palmpay.clone.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** A small, reusable action shown directly below the balance card. */
public final class QuickAction {
    @StringRes
    private final int titleRes;
    @DrawableRes
    private final int iconRes;
    @ColorRes
    private final int backgroundColorRes;
    @ColorRes
    private final int iconColorRes;

    public QuickAction(
            @StringRes int titleRes,
            @DrawableRes int iconRes,
            @ColorRes int backgroundColorRes,
            @ColorRes int iconColorRes) {
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.backgroundColorRes = backgroundColorRes;
        this.iconColorRes = iconColorRes;
    }

    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    @ColorRes
    public int getBackgroundColorRes() {
        return backgroundColorRes;
    }

    @ColorRes
    public int getIconColorRes() {
        return iconColorRes;
    }
}
