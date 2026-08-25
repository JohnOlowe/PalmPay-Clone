package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityTransferBinding;
import damjay.palmpay.clone.databinding.RecipientItemBinding;
import damjay.palmpay.clone.databinding.TransferShortcutItemBinding;
import damjay.palmpay.clone.transfer.data.TransferRepository;
import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/** Binds transfer data to XML and owns the small amount of form state. */
public final class TransferScreenController {
    private final Context context;
    private final ActivityTransferBinding binding;
    private final LayoutInflater inflater;
    private final TransferRepository repository;
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
        bindTabs();
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
            item.recipientDate.setText(recipient.getLastTransferDate());
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
        String[] banks = {"OPay", "Moniepoint", "PalmPay", "Access Bank"};
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.choose_bank)
                .setItems(banks, (dialog, which) -> {
                    bankSelected = true;
                    binding.selectedBankText.setText(banks[which]);
                    binding.selectedBankText.setTextColor(color(R.color.ink));
                    refreshNextState();
                })
                .show();
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
        binding.searchButton.setOnClickListener(view -> showMessage("Search contacts selected"));
        binding.viewAllButton.setOnClickListener(view -> showMessage("All contacts selected"));
    }

    private void bindTabs() {
        binding.recentTab.setOnClickListener(view -> showMessage("Recent selected"));
        binding.favoritesTab.setOnClickListener(view -> showMessage("Favorites selected"));
        binding.contactsTab.setOnClickListener(view -> showMessage("PalmPay Contacts selected"));
    }

    private void closeScreen() {
        if (context instanceof TransferActivity) {
            ((TransferActivity) context).finishFromTransfer();
        }
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
