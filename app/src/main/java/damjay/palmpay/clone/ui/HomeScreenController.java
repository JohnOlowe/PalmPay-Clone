package damjay.palmpay.clone.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.data.HomeCatalog;
import damjay.palmpay.clone.databinding.ActivityMainBinding;
import damjay.palmpay.clone.databinding.PromoCardItemBinding;
import damjay.palmpay.clone.databinding.QuickActionItemBinding;
import damjay.palmpay.clone.databinding.ServiceActionItemBinding;
import damjay.palmpay.clone.model.PromotionCard;
import damjay.palmpay.clone.model.QuickAction;
import damjay.palmpay.clone.model.ServiceAction;

/**
 * Coordinates the home screen without putting catalogue or interaction logic in
 * the activity.  The XML files define the visual contract while this class
 * turns the reusable models into views and wires up their behaviour.
 */
public final class HomeScreenController {
    private final Context context;
    private final ActivityMainBinding binding;
    private final LayoutInflater inflater;
    private boolean balanceVisible;

    public HomeScreenController(Context context, ActivityMainBinding binding) {
        this.context = context;
        this.binding = binding;
        this.inflater = LayoutInflater.from(context);
    }

    public void bind() {
        renderQuickActions(HomeCatalog.quickActions());
        renderServices(HomeCatalog.services());
        renderPromotions(HomeCatalog.promotions());
        bindBalanceCard();
        bindHeader();
        bindNavigation();
    }

    private void renderQuickActions(List<QuickAction> actions) {
        binding.quickActionsContainer.removeAllViews();

        for (QuickAction action : actions) {
            QuickActionItemBinding item = QuickActionItemBinding.inflate(
                    inflater, binding.quickActionsContainer, false);
            item.actionTitle.setText(action.getTitleRes());
            item.actionIcon.setImageResource(action.getIconRes());
            tint(item.actionIcon, action.getIconColorRes());
            setRoundedBackground(
                    item.actionIconContainer,
                    color(action.getBackgroundColorRes()),
                    dp(13));
            item.getRoot().setContentDescription(context.getString(action.getTitleRes()));
            item.getRoot().setOnClickListener(view -> showSelection(action.getTitleRes()));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            binding.quickActionsContainer.addView(item.getRoot(), layoutParams);
        }
    }

    private void renderServices(List<ServiceAction> services) {
        binding.servicesGrid.removeAllViews();

        for (ServiceAction service : services) {
            ServiceActionItemBinding item = ServiceActionItemBinding.inflate(
                    inflater, binding.servicesGrid, false);
            item.serviceTitle.setText(service.getTitleRes());
            item.serviceIcon.setImageResource(service.getIconRes());
            tint(item.serviceIcon, service.getIconColorRes());
            setRoundedBackground(
                    item.serviceIconContainer,
                    color(service.getBackgroundColorRes()),
                    dp(21));
            item.getRoot().setContentDescription(context.getString(service.getTitleRes()));
            item.getRoot().setOnClickListener(view -> showSelection(service.getTitleRes()));

            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            layoutParams.width = 0;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.setGravity(Gravity.FILL_HORIZONTAL);
            binding.servicesGrid.addView(item.getRoot(), layoutParams);
        }
    }

    private void renderPromotions(List<PromotionCard> promotions) {
        binding.promotionsContainer.removeAllViews();

        for (int index = 0; index < promotions.size(); index++) {
            PromotionCard promotion = promotions.get(index);
            PromoCardItemBinding item = PromoCardItemBinding.inflate(
                    inflater, binding.promotionsContainer, false);
            item.promoEyebrow.setText(promotion.getEyebrowRes());
            item.promoTitle.setText(promotion.getTitleRes());
            item.promoSubtitle.setText(promotion.getSubtitleRes());
            item.promoAction.setText(promotion.getActionRes());
            item.promoIllustration.setImageResource(promotion.getIllustrationRes());
            item.promoIllustration.setContentDescription(context.getString(promotion.getTitleRes()));
            item.getRoot().setCardBackgroundColor(color(promotion.getBackgroundColorRes()));
            item.getRoot().setContentDescription(context.getString(promotion.getTitleRes()));
            item.getRoot().setOnClickListener(view -> showSelection(promotion.getTitleRes()));
            item.promoAction.setOnClickListener(view -> showSelection(promotion.getTitleRes()));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0, dp(126), 1f);
            if (index > 0) {
                layoutParams.setMarginStart(dp(6));
                layoutParams.setMarginEnd(0);
            }
            binding.promotionsContainer.addView(item.getRoot(), layoutParams);
        }
    }

    private void bindBalanceCard() {
        binding.balanceVisibilityButton.setOnClickListener(view -> {
            balanceVisible = !balanceVisible;
            binding.balanceAmount.setText(balanceVisible
                    ? R.string.visible_balance
                    : R.string.hidden_balance);
            binding.balanceVisibilityButton.setImageResource(balanceVisible
                    ? R.drawable.ic_eye_off
                    : R.drawable.ic_eye_visible);
            binding.balanceVisibilityButton.setContentDescription(context.getString(
                    balanceVisible
                            ? R.string.balance_visible_description
                            : R.string.balance_hidden_description));
        });
        binding.addMoneyButton.setOnClickListener(view -> showMessage("Add Money selected"));
        binding.historyButton.setOnClickListener(view -> showMessage("Transaction History selected"));
    }

    private void bindHeader() {
        binding.profileButton.setOnClickListener(view -> showMessage("Profile selected"));
        binding.supportButton.setOnClickListener(view -> showMessage("Customer support selected"));
        binding.notificationsButton.setOnClickListener(view -> showMessage("No new notifications"));
    }

    private void bindNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() != R.id.nav_home) {
                showMessage(item.getTitle() + " selected");
            }
            return true;
        });
    }

    private void showSelection(@StringRes int titleRes) {
        showMessage(context.getString(titleRes) + " selected");
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void tint(android.widget.ImageView imageView, @ColorRes int colorRes) {
        @ColorInt int tintColor = color(colorRes);
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tintColor));
    }

    private void setRoundedBackground(View view, @ColorInt int backgroundColor, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(radius);
        view.setBackground(drawable);
    }

    @ColorInt
    private int color(@ColorRes int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
