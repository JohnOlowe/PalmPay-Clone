package damjay.palmpay.clone.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Content and styling for one of the two savings promotions. */
public final class PromotionCard {
    @StringRes
    private final int eyebrowRes;
    @StringRes
    private final int titleRes;
    @StringRes
    private final int subtitleRes;
    @StringRes
    private final int actionRes;
    @DrawableRes
    private final int illustrationRes;
    @ColorRes
    private final int backgroundColorRes;

    public PromotionCard(
            @StringRes int eyebrowRes,
            @StringRes int titleRes,
            @StringRes int subtitleRes,
            @StringRes int actionRes,
            @DrawableRes int illustrationRes,
            @ColorRes int backgroundColorRes) {
        this.eyebrowRes = eyebrowRes;
        this.titleRes = titleRes;
        this.subtitleRes = subtitleRes;
        this.actionRes = actionRes;
        this.illustrationRes = illustrationRes;
        this.backgroundColorRes = backgroundColorRes;
    }

    @StringRes
    public int getEyebrowRes() {
        return eyebrowRes;
    }

    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    @StringRes
    public int getSubtitleRes() {
        return subtitleRes;
    }

    @StringRes
    public int getActionRes() {
        return actionRes;
    }

    @DrawableRes
    public int getIllustrationRes() {
        return illustrationRes;
    }

    @ColorRes
    public int getBackgroundColorRes() {
        return backgroundColorRes;
    }
}
