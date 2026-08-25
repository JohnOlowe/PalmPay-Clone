package damjay.palmpay.clone.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.model.PromotionCard;
import damjay.palmpay.clone.model.QuickAction;
import damjay.palmpay.clone.model.ServiceAction;

/**
 * The local catalogue for the home screen.
 *
 * Keeping the catalogue separate from the activity makes replacing these static
 * entries with a repository or an API response straightforward later on.
 */
public final class HomeCatalog {
    private HomeCatalog() {
        // No instances.
    }

    public static List<QuickAction> quickActions() {
        return Collections.unmodifiableList(Arrays.asList(
                new QuickAction(
                        R.string.quick_action_to_bank,
                        R.drawable.ic_to_bank,
                        R.color.quick_card_surface,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_palmpay,
                        R.drawable.ic_palmpay_transfer,
                        R.color.quick_card_surface,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_savings,
                        R.drawable.ic_savings,
                        R.color.quick_card_surface,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_cards,
                        R.drawable.ic_card,
                        R.color.quick_card_surface,
                        R.color.icon_purple)
        ));
    }

    public static List<ServiceAction> services() {
        return Collections.unmodifiableList(Arrays.asList(
                new ServiceAction(R.string.service_airtime, R.drawable.ic_airtime,
                        R.color.service_card_surface, R.color.icon_blue),
                new ServiceAction(R.string.service_data, R.drawable.ic_data,
                        R.color.service_card_surface, R.color.icon_green),
                new ServiceAction(R.string.service_betting, R.drawable.ic_betting,
                        R.color.service_card_surface, R.color.icon_teal),
                new ServiceAction(R.string.service_electricity, R.drawable.ic_electricity,
                        R.color.service_card_surface, R.color.icon_green),
                new ServiceAction(R.string.service_refer_earn, R.drawable.ic_refer_earn,
                        R.color.service_card_surface, R.color.icon_purple),
                new ServiceAction(R.string.service_insurance, R.drawable.ic_insurance,
                        R.color.service_card_surface, R.color.icon_blue),
                new ServiceAction(R.string.service_loan, R.drawable.ic_loan_service,
                        R.color.service_card_surface, R.color.icon_teal),
                new ServiceAction(R.string.service_more, R.drawable.ic_more,
                        R.color.service_card_surface, R.color.icon_purple)
        ));
    }

    public static List<PromotionCard> promotions() {
        return Collections.unmodifiableList(Arrays.asList(
                new PromotionCard(
                        R.string.team_save_eyebrow,
                        R.string.team_save_subtitle,
                        R.string.team_save_amount,
                        R.string.team_save_amount_caption,
                        R.string.team_save_action,
                        0,
                        R.color.promo_surface),
                new PromotionCard(
                        R.string.cashbox_eyebrow,
                        R.string.cashbox_subtitle,
                        R.string.cashbox_amount,
                        R.string.cashbox_amount_caption,
                        R.string.cashbox_action,
                        R.drawable.gift_reference,
                        R.color.promo_surface)
        ));
    }
}
