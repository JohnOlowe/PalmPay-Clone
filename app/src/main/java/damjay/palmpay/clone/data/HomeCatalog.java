package damjay.palmpay.clone.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.model.PromotionCard;
import damjay.palmpay.clone.model.QuickAction;
import damjay.palmpay.clone.model.ServiceAction;

/**
 * The local catalogue for the demo home screen.
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
                        R.color.pastel_lilac,
                        R.color.icon_purple),
                new QuickAction(
                        R.string.quick_action_palmpay,
                        R.drawable.ic_palmpay_transfer,
                        R.color.pastel_green,
                        R.color.icon_green),
                new QuickAction(
                        R.string.quick_action_savings,
                        R.drawable.ic_savings,
                        R.color.pastel_teal,
                        R.color.icon_teal),
                new QuickAction(
                        R.string.quick_action_cards,
                        R.drawable.ic_card,
                        R.color.pastel_orange,
                        R.color.icon_orange)
        ));
    }

    public static List<ServiceAction> services() {
        return Collections.unmodifiableList(Arrays.asList(
                new ServiceAction(R.string.service_airtime, R.drawable.ic_airtime,
                        R.color.pastel_lilac, R.color.icon_purple),
                new ServiceAction(R.string.service_data, R.drawable.ic_data,
                        R.color.pastel_blue, R.color.icon_blue),
                new ServiceAction(R.string.service_betting, R.drawable.ic_betting,
                        R.color.pastel_red, R.color.icon_red),
                new ServiceAction(R.string.service_electricity, R.drawable.ic_electricity,
                        R.color.pastel_yellow, R.color.icon_yellow),
                new ServiceAction(R.string.service_cable_tv, R.drawable.ic_tv,
                        R.color.pastel_teal, R.color.icon_teal),
                new ServiceAction(R.string.service_education, R.drawable.ic_education,
                        R.color.pastel_slate, R.color.icon_slate),
                new ServiceAction(R.string.service_loans, R.drawable.ic_loan,
                        R.color.pastel_orange, R.color.icon_orange),
                new ServiceAction(R.string.service_more, R.drawable.ic_more,
                        R.color.pastel_lilac, R.color.icon_purple)
        ));
    }

    public static List<PromotionCard> promotions() {
        return Collections.unmodifiableList(Arrays.asList(
                new PromotionCard(
                        R.string.cashbox_eyebrow,
                        R.string.cashbox_title,
                        R.string.cashbox_subtitle,
                        R.string.cashbox_action,
                        R.drawable.ic_cashbox,
                        R.color.cashbox_green),
                new PromotionCard(
                        R.string.fixed_savings_eyebrow,
                        R.string.fixed_savings_title,
                        R.string.fixed_savings_subtitle,
                        R.string.fixed_savings_action,
                        R.drawable.ic_fixed_savings,
                        R.color.fixed_savings_blue)
        ));
    }
}
