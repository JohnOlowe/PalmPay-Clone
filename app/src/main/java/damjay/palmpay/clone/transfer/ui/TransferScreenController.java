package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityTransferBinding;
import damjay.palmpay.clone.databinding.RecipientItemBinding;
import damjay.palmpay.clone.databinding.TransferShortcutItemBinding;
import damjay.palmpay.clone.databinding.TransferTabsBinding;
import damjay.palmpay.clone.transfer.data.BankLogoLoader;
import damjay.palmpay.clone.transfer.data.BankLogoResolver;
import damjay.palmpay.clone.transfer.data.TransferRepository;
import damjay.palmpay.clone.transfer.model.BankInstitution;
import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/** Binds transfer data to XML and owns the small amount of form state. */
public final class TransferScreenController {
    private final Context context;
    private final ActivityTransferBinding binding;
    private final LayoutInflater inflater;
    private final TransferRepository repository;
    private final BankLogoLoader logoLoader = new BankLogoLoader();
    private boolean bankSelected;

    public TransferScreenController(
            Context context,
            ActivityTransferBinding binding,
            TransferRepository repository) {
        this.context = context;
        this.binding = binding;
        this.repository = repository;
        this.inflater = LayoutInflater.from(context);
    }

    public void bind() {
        renderShortcuts(repository.getShortcuts());
        renderRecipients(repository.getRecentRecipients());
        bindForm();
        bindToolbar();
        bindTabs(binding.transferTabsInline);
        bindTabs(binding.transferTabsSticky);
        bindStickyTabs();
    }

    private void renderShortcuts(List<TransferShortcut> shortcuts) {
        binding.transferShortcutsContainer.removeAllViews();
        for (TransferShortcut shortcut : shortcuts) {
            TransferShortcutItemBinding item = TransferShortcutItemBinding.inflate(
                    inflater, binding.transferShortcutsContainer, false);
            item.shortcutIcon.setImageResource(shortcut.getIconRes());
            item.shortcutTitle.setText(shortcut.getTitleRes());
            item.getRoot().setContentDescription(context.getString(shortcut.getTitleRes()));
            item.getRoot().setOnClickListener(view -> showMessage(
                    context.getString(shortcut.getTitleRes()) + " selected"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dp(87), 1f);
            binding.transferShortcutsContainer.addView(item.getRoot(), params);
        }
    }

    private void renderRecipients(List<TransferRecipient> recipients) {
        binding.recipientsContainer.removeAllViews();
        for (TransferRecipient recipient : recipients) {
            RecipientItemBinding item = RecipientItemBinding.inflate(
                    inflater, binding.recipientsContainer, false);
            item.recipientName.setText(recipient.getName());
            item.recipientAccount.setText(recipient.getAccountNumber());
            item.recipientProvider.setText(recipient.getProvider());
            item.recipientDate.setText(context.getString(
                    R.string.last_transfer_on, recipient.getLastTransferDate()));
            item.recipientIcon.setTag(null);
            int fallbackLogo = BankLogoResolver.fallbackForProvider(recipient.getProvider());
            item.recipientIcon.setImageResource(fallbackLogo);
            if (fallbackLogo == R.drawable.ic_bank_building) {
                ImageViewCompat.setImageTintList(item.recipientIcon, ColorStateList.valueOf(
                        color(android.R.color.white)));
            } else {
                ImageViewCompat.setImageTintList(item.recipientIcon, null);
            }
            if (fallbackLogo == R.drawable.ic_bank_building) {
                logoLoader.load(
                        BankLogoResolver.forProvider(recipient.getProvider()), item.recipientIcon);
            }
            item.getRoot().setContentDescription(recipient.getName());
            item.getRoot().setOnClickListener(view -> {
                binding.accountNumberInput.setText(recipient.getAccountNumber());
                binding.accountNumberInput.setSelection(binding.accountNumberInput.length());
                showMessage(recipient.getName() + " selected");
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(88));
            binding.recipientsContainer.addView(item.getRoot(), params);
        }
    }

    private void bindForm() {
        binding.transferProtectionButton.setOnClickListener(view ->
                showMessage("Transfer protection selected"));
        binding.bankField.setOnClickListener(view -> showBankPicker());
        binding.nextButton.setOnClickListener(view -> {
            if (!isFormReady()) {
                showMessage("Enter an account number and select a bank");
            } else {
                showMessage("Transfer details ready");
            }
        });
        binding.accountNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                refreshNextState();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No-op.
            }
        });
        refreshNextState();
    }

    private void showBankPicker() {
        if (context instanceof TransferActivity) {
            ((TransferActivity) context).openBankPicker();
        } else {
            BankPickerDialog.show(context, this::selectBank);
        }
    }

    public void selectBank(BankInstitution bank) {
        bankSelected = true;
        binding.selectedBankText.setText(bank.getName());
        binding.selectedBankText.setTextColor(color(R.color.ink));
        refreshNextState();
    }

    private void refreshNextState() {
        boolean ready = isFormReady();
        binding.nextButton.setBackgroundResource(
                ready ? R.drawable.bg_transfer_next_enabled : R.drawable.bg_transfer_next);
        binding.nextButton.setTextColor(color(
                ready ? R.color.transfer_next_text_enabled : R.color.transfer_next_text_disabled));
    }

    private boolean isFormReady() {
        return binding.accountNumberInput.length() == 10 && bankSelected;
    }

    private void bindToolbar() {
        binding.transferBackButton.setOnClickListener(view -> closeScreen());
        binding.transferSupportButton.setOnClickListener(view -> showMessage("Transfer support selected"));
        binding.transferHistoryButton.setOnClickListener(view -> showMessage("Transfer history selected"));
        binding.viewAllButton.setOnClickListener(view -> showMessage("All contacts selected"));
    }

    private void bindTabs(TransferTabsBinding tabs) {
        tabs.recentTab.setOnClickListener(view -> showMessage("Recent selected"));
        tabs.favoritesTab.setOnClickListener(view -> showMessage("Favorites selected"));
        tabs.contactsTab.setOnClickListener(view -> showMessage("PalmPay Contacts selected"));
        tabs.searchButton.setOnClickListener(view -> showMessage("Search contacts selected"));
    }

    private void bindStickyTabs() {
        binding.transferTabsSticky.getRoot().setVisibility(View.GONE);
        binding.transferScroll.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                        (view, scrollX, scrollY, oldScrollX, oldScrollY) -> updateStickyTabs());
        binding.transferScroll.post(this::updateStickyTabs);
    }

    private void updateStickyTabs() {
        if (binding.transferScroll.getChildCount() == 0) {
            return;
        }
        int recentTop = binding.recentCard.getTop();
        boolean shouldStick = binding.transferScroll.getScrollY() >= recentTop;
        binding.transferTabsSticky.getRoot().setVisibility(
                shouldStick ? View.VISIBLE : View.GONE);
    }

    private void closeScreen() {
        if (context instanceof TransferActivity) {
            ((TransferActivity) context).finishFromTransfer();
        }
    }

    public void onDestroy() {
        logoLoader.close();
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @ColorInt
    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
