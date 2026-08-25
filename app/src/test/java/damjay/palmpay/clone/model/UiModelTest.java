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
                R.color.pastel_orange,
                R.color.icon_orange);

        assertEquals(R.string.quick_action_cards, action.getTitleRes());
        assertEquals(R.drawable.ic_card, action.getIconRes());
        assertEquals(R.color.pastel_orange, action.getBackgroundColorRes());
        assertEquals(R.color.icon_orange, action.getIconColorRes());
    }

    @Test
    public void promotionCardExposesEveryConstructorValue() {
        PromotionCard promotion = new PromotionCard(
                R.string.cashbox_eyebrow,
                R.string.cashbox_title,
                R.string.cashbox_subtitle,
                R.string.cashbox_action,
                R.drawable.ic_cashbox,
                R.color.cashbox_green);

        assertEquals(R.string.cashbox_eyebrow, promotion.getEyebrowRes());
        assertEquals(R.string.cashbox_title, promotion.getTitleRes());
        assertEquals(R.string.cashbox_subtitle, promotion.getSubtitleRes());
        assertEquals(R.string.cashbox_action, promotion.getActionRes());
        assertEquals(R.drawable.ic_cashbox, promotion.getIllustrationRes());
        assertEquals(R.color.cashbox_green, promotion.getBackgroundColorRes());
    }
}
