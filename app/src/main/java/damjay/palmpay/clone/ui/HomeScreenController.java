package damjay.palmpay.clone.ui;

import android.content.Context;
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

import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.data.HomeCatalog;
import damjay.palmpay.clone.data.WalletStore;
import damjay.palmpay.clone.profile.ProfileActivity;
import damjay.palmpay.clone.databinding.ActivityMainBinding;
import damjay.palmpay.clone.databinding.BottomNavItemBinding;
import damjay.palmpay.clone.databinding.PromoCardItemBinding;
import damjay.palmpay.clone.databinding.QuickActionItemBinding;
import damjay.palmpay.clone.databinding.ServiceActionItemBinding;
import damjay.palmpay.clone.model.PromotionCard;
import damjay.palmpay.clone.model.QuickAction;
import damjay.palmpay.clone.model.ServiceAction;
import damjay.palmpay.clone.transfer.ui.TransferActivity;

/**
 * Coordinates the home screen without putting catalogue or interaction logic in
 * the activity. The XML files define the visual contract while this class turns
 * the reusable models into views and wires up their behaviour.
 */
public final class HomeScreenController {
    private boolean carouselAlive;
    private static final int BADGE_NONE = 0;
    private static final int BADGE_DOT = 1;
    private static final int BADGE_NEW = 2;
    private final Context context;
    private final ActivityMainBinding binding;
    private final LayoutInflater inflater;
    private final WalletStore walletStore;
    private boolean balanceVisible = true;

    public HomeScreenController(Context context, ActivityMainBinding binding) {
        this.context = context;
        this.binding = binding;
        this.inflater = LayoutInflater.from(context);
        this.walletStore = new WalletStore(context);
    }

    public void bind() {
        renderQuickActions(HomeCatalog.quickActions());
        renderServices(HomeCatalog.services());
        renderPromotions(HomeCatalog.promotions());
        bindBalanceCard();
        bindHeader();
        bindClaimCard();
        bindPromoCarousel();
        bindNavigation();
    }

    private void renderQuickActions(List<QuickAction> actions) {
        binding.quickActionsContainer.removeAllViews();

        for (int index = 0; index < actions.size(); index++) {
            QuickAction action = actions.get(index);
            QuickActionItemBinding item = QuickActionItemBinding.inflate(
                    inflater, binding.quickActionsContainer, false);
            item.actionTitle.setText(action.getTitleRes());
            item.actionIcon.setImageResource(action.getIconRes());
            item.actionBadge.setVisibility(
                    action.getTitleRes() == R.string.quick_action_to_bank
                            ? View.VISIBLE : View.GONE);
            setRoundedBackground(
                    item.getRoot(),
                    color(action.getBackgroundColorRes()),
                    dp(14));
            item.getRoot().setContentDescription(context.getString(action.getTitleRes()));
            item.getRoot().setOnClickListener(view -> {
                if (action.getTitleRes() == R.string.quick_action_to_bank) {
                    TransferActivity.start(context);
                } else {
                    showSelection(action.getTitleRes());
                }
            });

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0, dp(72), 1f);
            layoutParams.setMarginStart(index == 0 ? 0 : dp(3));
            layoutParams.setMarginEnd(index == actions.size() - 1 ? 0 : dp(3));
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
            if (service.getTitleRes() == R.string.service_refer_earn) {
                ViewGroup.LayoutParams iconParams = item.serviceIconContainer.getLayoutParams();
                iconParams.width = dp(45);
                iconParams.height = dp(30);
                item.serviceIconContainer.setLayoutParams(iconParams);
            }
            item.getRoot().setContentDescription(context.getString(service.getTitleRes()));
            item.getRoot().setOnClickListener(view -> showSelection(service.getTitleRes()));

            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            layoutParams.width = 0;
            layoutParams.height = dp(56);
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
            item.promoEyebrow.setText(promotion.getHeadingRes());
            item.promoTitle.setText(promotion.getSubtitleRes());
            item.promoSubtitle.setText(promotion.getAmountRes());
            item.promoAmountCaption.setText(promotion.getAmountCaptionRes());
            item.promoAction.setText(promotion.getActionRes());
            item.getRoot().setCardBackgroundColor(color(promotion.getBackgroundColorRes()));
            item.getRoot().setContentDescription(context.getString(promotion.getHeadingRes()));
            item.getRoot().setOnClickListener(view -> showSelection(promotion.getHeadingRes()));
            item.promoAction.setOnClickListener(view -> showSelection(promotion.getHeadingRes()));

            if (promotion.getIllustrationRes() == 0) {
                item.promoIllustration.setVisibility(View.GONE);
            } else {
                item.promoIllustration.setVisibility(View.VISIBLE);
                item.promoIllustration.setImageResource(promotion.getIllustrationRes());
                item.promoIllustration.setContentDescription(
                        context.getString(promotion.getHeadingRes()));
            }

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0, dp(148), 1f);
            if (index > 0) {
                layoutParams.setMarginStart(dp(6));
            }
            binding.promotionsContainer.addView(item.getRoot(), layoutParams);
        }
    }

    public void refreshBalance() {
        binding.balanceAmount.setText(balanceVisible
                ? walletStore.getBalanceDisplay()
                : context.getString(R.string.hidden_balance));
    }

    private void bindBalanceCard() {
        refreshBalance();
        binding.balanceVisibilityButton.setOnClickListener(view -> {
            balanceVisible = !balanceVisible;
            refreshBalance();
            binding.balanceVisibilityButton.setImageResource(balanceVisible
                    ? R.drawable.exact_eye_visible
                    : R.drawable.ic_eye_off);
            binding.balanceVisibilityButton.setContentDescription(context.getString(
                    balanceVisible
                            ? R.string.balance_visible_description
                            : R.string.balance_hidden_description));
        });
        binding.addMoneyButton.setOnClickListener(view -> showMessage("Add Money selected"));
        binding.historyButton.setOnClickListener(view -> showMessage("Transaction History selected"));
    }

    private void bindHeader() {
        binding.profileButton.setOnClickListener(view -> ProfileActivity.start(context));
        binding.supportButton.setOnClickListener(view -> showMessage("Customer support selected"));
        binding.notificationsButton.setOnClickListener(view -> showMessage("No new notifications"));
    }

    private void bindClaimCard() {
        binding.claimAction.setOnClickListener(view -> showMessage("Claim selected"));
    }

    private void bindPromoCarousel() {
        final android.widget.ViewFlipper flipper = binding.promoFlipper;
        flipper.setFlipInterval(4000);
        flipper.setInAnimation(context, android.R.anim.fade_in);
        flipper.setOutAnimation(context, android.R.anim.fade_out);
        flipper.startFlipping();
        carouselAlive = true;
        final android.os.Handler handler = new android.os.Handler(
                android.os.Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!carouselAlive) {
                    return;
                }
                updatePromoDots(flipper.getDisplayedChild());
                handler.postDelayed(this, 4000);
            }
        });
        binding.cashbackPage.cashbackAction.setOnClickListener(
                view -> showMessage("Cashback selected"));
        binding.moreWealthRow.setOnClickListener(
                view -> showMessage("More wealth products selected"));
        binding.borrowBanner.setOnClickListener(
                view -> showMessage("Borrow selected"));
    }

    private void updatePromoDots(int page) {
        android.widget.LinearLayout dots = binding.claimDots;
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setBackgroundResource(
                    i == page ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        }
    }

    /** Stops the carousel ticker when the activity goes away. */
    public void release() {
        carouselAlive = false;
        binding.promoFlipper.stopFlipping();
    }

    private void bindNavigation() {
        binding.bottomBar.removeAllViews();
        addNavItem(R.drawable.exact_nav_home, R.string.nav_home, BADGE_NONE, true);
        addNavItem(R.drawable.exact_nav_loan, R.string.nav_loan, BADGE_DOT, false);
        addNavItem(R.drawable.exact_nav_wealth, R.string.nav_wealth, BADGE_NEW, false);
        addNavItem(R.drawable.exact_nav_reward, R.string.nav_reward, BADGE_NONE, false);
        addNavItem(R.drawable.exact_nav_me, R.string.nav_me, BADGE_NONE, false);
    }

    private void addNavItem(
            int iconRes, int labelRes, int badgeKind, boolean selected) {
        BottomNavItemBinding item = BottomNavItemBinding.inflate(
                inflater, binding.bottomBar, false);
        int tint = color(selected ? R.color.brand_purple : R.color.nav_unselected);
        item.navIcon.setImageResource(iconRes);
        item.navIcon.setColorFilter(tint);
        item.navLabel.setText(labelRes);
        item.navLabel.setTextColor(tint);
        item.navDotBadge.setVisibility(badgeKind == BADGE_DOT ? View.VISIBLE : View.GONE);
        item.navNewBadge.setVisibility(badgeKind == BADGE_NEW ? View.VISIBLE : View.GONE);
        item.getRoot().setContentDescription(context.getString(labelRes));
        item.getRoot().setOnClickListener(view -> {
            if (!selected) {
                showSelection(labelRes);
            }
        });
        binding.bottomBar.addView(item.getRoot(), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
    }

    private void showSelection(@StringRes int titleRes) {
        showMessage(context.getString(titleRes) + " selected");
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
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
