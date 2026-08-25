package damjay.palmpay.clone.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Content and styling for one savings promotion tile. */
public final class PromotionCard {
    @StringRes
    private final int headingRes;
    @StringRes
    private final int subtitleRes;
    @StringRes
    private final int amountRes;
    @StringRes
    private final int amountCaptionRes;
    @StringRes
    private final int actionRes;
    @DrawableRes
    private final int illustrationRes;
    @ColorRes
    private final int backgroundColorRes;

    public PromotionCard(
            @StringRes int headingRes,
            @StringRes int subtitleRes,
            @StringRes int amountRes,
            @StringRes int amountCaptionRes,
            @StringRes int actionRes,
            @DrawableRes int illustrationRes,
            @ColorRes int backgroundColorRes) {
        this.headingRes = headingRes;
        this.subtitleRes = subtitleRes;
        this.amountRes = amountRes;
        this.amountCaptionRes = amountCaptionRes;
        this.actionRes = actionRes;
        this.illustrationRes = illustrationRes;
        this.backgroundColorRes = backgroundColorRes;
    }

    @StringRes
    public int getHeadingRes() {
        return headingRes;
    }

    @StringRes
    public int getSubtitleRes() {
        return subtitleRes;
    }

    @StringRes
    public int getAmountRes() {
        return amountRes;
    }

    @StringRes
    public int getAmountCaptionRes() {
        return amountCaptionRes;
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
