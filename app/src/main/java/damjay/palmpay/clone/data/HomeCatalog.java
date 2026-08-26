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
                        R.drawable.home_action_bank,
                        R.color.quick_card_surface,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_palmpay,
                        R.drawable.home_action_palmpay,
                        R.color.quick_card_surface,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_savings,
                        R.drawable.home_action_savings,
                        R.color.quick_card_surface,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_cards,
                        R.drawable.home_action_cards,
                        R.color.quick_card_surface,
                        R.color.icon_purple)
        ));
    }

    public static List<ServiceAction> services() {
        return Collections.unmodifiableList(Arrays.asList(
                new ServiceAction(R.string.service_airtime, R.drawable.home_service_airtime,
                        R.color.service_card_surface, R.color.icon_blue),
                new ServiceAction(R.string.service_data, R.drawable.home_service_data,
                        R.color.service_card_surface, R.color.icon_green),
                new ServiceAction(R.string.service_betting, R.drawable.home_service_betting,
                        R.color.service_card_surface, R.color.icon_teal),
                new ServiceAction(R.string.service_electricity, R.drawable.home_service_electricity,
                        R.color.service_card_surface, R.color.icon_green),
                new ServiceAction(R.string.service_refer_earn, R.drawable.home_service_refer,
                        R.color.service_card_surface, R.color.icon_purple),
                new ServiceAction(R.string.service_insurance, R.drawable.home_service_insurance,
                        R.color.service_card_surface, R.color.icon_blue),
                new ServiceAction(R.string.service_loan, R.drawable.home_service_loan,
                        R.color.service_card_surface, R.color.icon_teal),
                new ServiceAction(R.string.service_more, R.drawable.home_service_more,
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
