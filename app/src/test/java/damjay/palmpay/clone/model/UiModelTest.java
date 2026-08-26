package damjay.palmpay.clone.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import damjay.palmpay.clone.R;

/** Small unit tests for the immutable view models used by the renderer. */
public class UiModelTest {

    @Test
    public void quickActionExposesEveryConstructorValue() {
        QuickAction action = new QuickAction(
                R.string.quick_action_cards,
                R.drawable.ic_card,
                R.color.quick_card_surface,
                R.color.icon_purple);

        assertEquals(R.string.quick_action_cards, action.getTitleRes());
        assertEquals(R.drawable.ic_card, action.getIconRes());
        assertEquals(R.color.quick_card_surface, action.getBackgroundColorRes());
        assertEquals(R.color.icon_purple, action.getIconColorRes());
    }

    @Test
    public void promotionCardExposesEveryConstructorValue() {
        PromotionCard promotion = new PromotionCard(
                R.string.cashbox_eyebrow,
                R.string.cashbox_subtitle,
                R.string.cashbox_amount,
                R.string.cashbox_amount_caption,
                R.string.cashbox_action,
                R.drawable.gift_reference,
                R.color.promo_surface);

        assertEquals(R.string.cashbox_eyebrow, promotion.getHeadingRes());
        assertEquals(R.string.cashbox_subtitle, promotion.getSubtitleRes());
        assertEquals(R.string.cashbox_amount, promotion.getAmountRes());
        assertEquals(R.string.cashbox_amount_caption, promotion.getAmountCaptionRes());
        assertEquals(R.string.cashbox_action, promotion.getActionRes());
        assertEquals(R.drawable.gift_reference, promotion.getIllustrationRes());
        assertEquals(R.color.promo_surface, promotion.getBackgroundColorRes());
    }
}
