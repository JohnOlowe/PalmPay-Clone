package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.AccountSuggestionItemBinding;
import damjay.palmpay.clone.databinding.ActivityTransferBinding;
import damjay.palmpay.clone.databinding.RecipientItemBinding;
import damjay.palmpay.clone.databinding.TransferShortcutItemBinding;
import damjay.palmpay.clone.databinding.TransferTabsBinding;
import damjay.palmpay.clone.transfer.data.BankDirectoryRepository;
import damjay.palmpay.clone.transfer.data.BankLogoLoader;
import damjay.palmpay.clone.transfer.data.BankLogoResolver;
import damjay.palmpay.clone.transfer.data.NubanBankResolver;
import damjay.palmpay.clone.transfer.data.PaystackClient;
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
    private final BankDirectoryRepository directoryRepository =
            new BankDirectoryRepository();
    private final List<TransferRecipient> transferHistory = new ArrayList<>();
    private List<BankInstitution> directoryBanks;
    private boolean directoryLoading;
    private final PaystackClient paystackClient = new PaystackClient();
    private boolean paystackProbing;
    private TransferRecipient resolvedRecipient;
    private boolean bankSelected;
    private boolean formattingAccount;
    private TransferRecipient trustedRecipient;

    public TransferScreenController(
            Context context,
            ActivityTransferBinding binding,
            TransferRepository repository) {
        this.context = context;
        this.binding = binding;
        this.inflater = LayoutInflater.from(context);
        this.repository = repository;
    }

    public void bind() {
        transferHistory.clear();
        transferHistory.addAll(repository.getRecentRecipients());
        directoryRepository.load((banks, fromNetwork) -> {
            directoryBanks = banks;
            String current = digitsOnly(binding.accountNumberInput.getText());
            if (current.length() == DIGITS_REQUIRED && trustedRecipient == null) {
                autoResolveBank(current);
            }
        });
        renderShortcuts(repository.getShortcuts());
        renderRecipients(transferHistory);
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
            applyProviderLogo(item.recipientIcon, recipient.getProvider());
            item.getRoot().setContentDescription(recipient.getName());
            item.getRoot().setOnClickListener(view -> selectRecentRecipient(recipient));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(88));
            binding.recipientsContainer.addView(item.getRoot(), params);
        }
    }

    private void bindForm() {
        binding.transferProtectionButton.setOnClickListener(view ->
                showMessage("Transfer protection selected"));
        binding.bankField.setOnClickListener(view -> showBankPicker());
        binding.clearAccountButton.setOnClickListener(view -> {
            binding.accountNumberInput.setText("");
            binding.accountNumberInput.requestFocus();
        });
        binding.nextButton.setOnClickListener(view -> {
            if (!isFormReady()) {
                showMessage("Choose a matching transfer history suggestion first");
                return;
            }
            openAmountScreen(effectiveRecipient());
        });
        binding.accountNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // Formatting is intentionally done in afterTextChanged so the cursor
                // can be restored after the account number is grouped.
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (formattingAccount) {
                    return;
                }
                String digits = digitsOnly(editable);
                String formatted = formatAccountNumber(digits);
                if (!formatted.contentEquals(editable)) {
                    formattingAccount = true;
                    binding.accountNumberInput.setText(formatted);
                    binding.accountNumberInput.setSelection(formatted.length());
                    formattingAccount = false;
                }
                binding.clearAccountButton.setVisibility(
                        digits.isEmpty() ? View.GONE : View.VISIBLE);
                updateAccountMatch(digits);
            }
        });
        binding.clearAccountButton.setVisibility(View.GONE);
        refreshNextState();
    }

    private void updateAccountMatch(String digits) {
        if (digits.length() == DIGITS_REQUIRED) {
            TransferRecipient match = trustedRecipientFor(digits);
            if (match != null) {
                selectTrustedRecipient(match, true);
                return;
            }
            autoResolveBank(digits);
        }

        trustedRecipient = null;
        bankSelected = false;
        hideConfirmation();
        resetBankField();
        if (digits.length() > 0 && digits.length() < 10) {
            renderAccountSuggestions(digits);
        } else {
            hideAccountSuggestions();
        }
        refreshNextState();
    }

    private void renderAccountSuggestions(String digits) {
        binding.accountSuggestionsContainer.removeAllViews();
        int count = 0;
        for (TransferRecipient recipient : transferHistory) {
            if (!recipient.getAccountNumber().startsWith(digits)) {
                continue;
            }
            AccountSuggestionItemBinding item = AccountSuggestionItemBinding.inflate(
                    inflater, binding.accountSuggestionsContainer, false);
            item.suggestionName.setText(recipient.getName());
            applyProgressiveAccountColor(
                    item.suggestionAccount, recipient.getAccountNumber(), digits);
            item.suggestionProvider.setText(recipient.getProvider());
            applyProviderLogo(item.suggestionLogo, recipient.getProvider());
            item.getRoot().setContentDescription(recipient.getName());
            item.getRoot().setOnClickListener(view -> selectSuggestedRecipient(recipient));
            binding.accountSuggestionsContainer.addView(item.getRoot(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
            count++;
        }
        binding.accountSuggestionsContainer.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    private void applyProgressiveAccountColor(
            android.widget.TextView accountView,
            String accountNumber,
            String typedDigits) {
        SpannableString account = new SpannableString(accountNumber);
        int highlightedLength = Math.min(typedDigits.length(), accountNumber.length());
        if (highlightedLength > 0) {
            account.setSpan(
                    new ForegroundColorSpan(color(R.color.brand_purple)),
                    0,
                    highlightedLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (highlightedLength < accountNumber.length()) {
            account.setSpan(
                    new ForegroundColorSpan(color(R.color.ink_secondary)),
                    highlightedLength,
                    accountNumber.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        accountView.setText(account);
    }

    private TransferRecipient trustedRecipientFor(String digits) {
        TransferRecipient firstMatch = null;
        for (TransferRecipient recipient : transferHistory) {
            if (!recipient.getAccountNumber().equals(digits)) {
                continue;
            }
            // The reference history contains the same account on more than one
            // provider. SmartCash is the trusted row for this demo flow.
            if (recipient.getProvider().toLowerCase().contains("smartcash")) {
                return recipient;
            }
            if (firstMatch == null) {
                firstMatch = recipient;
            }
        }
        return firstMatch;
    }

    private void selectRecentRecipient(TransferRecipient recipient) {
        populateTrustedRecipient(recipient);
        selectTrustedRecipient(recipient, true);
        openAmountScreen(recipient);
    }

    private void selectSuggestedRecipient(TransferRecipient recipient) {
        populateTrustedRecipient(recipient);
        selectTrustedRecipient(recipient, true);
    }

    private void populateTrustedRecipient(TransferRecipient recipient) {
        String formatted = formatAccountNumber(recipient.getAccountNumber());
        formattingAccount = true;
        binding.accountNumberInput.setText(formatted);
        binding.accountNumberInput.setSelection(formatted.length());
        formattingAccount = false;
        binding.clearAccountButton.setVisibility(View.VISIBLE);
    }

    private void selectTrustedRecipient(TransferRecipient recipient, boolean showConfirmation) {
        trustedRecipient = recipient;
        bankSelected = true;
        hideAccountSuggestions();
        showResolvedBank(recipient.getProvider(), null);
        if (showConfirmation) {
            binding.recipientConfirmationItem.confirmationName.setText(recipient.getName());
            binding.recipientConfirmationItem.getRoot().setVisibility(View.VISIBLE);
        } else {
            hideConfirmation();
        }
        refreshNextState();
    }

    private void showBankPicker() {
        if (context instanceof TransferActivity) {
            ((TransferActivity) context).openBankPicker();
        } else {
            BankPickerDialog.show(context, bank -> selectBank(bank));
        }
    }

    public void selectBank(BankInstitution bank) {
        trustedRecipient = null;
        resolvedRecipient = null;
        bankSelected = true;
        hideAccountSuggestions();
        hideConfirmation();
        showResolvedBank(bank.getName(), bank.getLogoUrl());
        refreshNextState();
    }

    /**
     * Automatically retrieves the bank attached to a complete account number.
     * A matching transfer history wins; otherwise the NUBAN check-digit
     * algorithm is replayed against the loaded bank directory.
     */
    private void autoResolveBank(String digits) {
        if (directoryBanks == null) {
            if (!directoryLoading) {
                directoryLoading = true;
                directoryRepository.load((banks, fromNetwork) -> {
                    directoryLoading = false;
                    directoryBanks = banks;
                    String current = digitsOnly(binding.accountNumberInput.getText());
                    if (current.equals(digits) && trustedRecipient == null) {
                        autoResolveBank(digits);
                    }
                });
            }
            return;
        }
        List<BankInstitution> candidates =
                NubanBankResolver.candidateBanks(digits, directoryBanks);
        if (candidates.isEmpty()) {
            paystackResolve(digits);
            return;
        }
        BankInstitution chosen = candidates.get(0);
        outer:
        for (TransferRecipient recipient : transferHistory) {
            if (!recipient.getAccountNumber().equals(digits)) {
                continue;
            }
            String provider = recipient.getProvider().toLowerCase(Locale.US);
            for (BankInstitution bank : candidates) {
                String name = bank.getName().toLowerCase(Locale.US);
                if (name.contains(provider) || provider.contains(name)) {
                    chosen = bank;
                    break outer;
                }
            }
        }
        bankSelected = true;
        showResolvedBank(chosen.getName(), chosen.getLogoUrl());
        refreshNextState();
    }

    /**
     * Paystack-backed retrieval for institutions the NUBAN algorithm cannot
     * identify (OPay, PalmPay, Moniepoint and other wallet providers): the
     * account number is probed against the Paystack institution list, the
     * first bank that validates it wins, and the returned holder name is
     * shown as the verified recipient.
     */
    private void paystackResolve(final String digits) {
        if (!paystackClient.isConfigured() || paystackProbing) {
            return;
        }
        paystackProbing = true;
        paystackClient.listBanks(banks -> {
            if (banks.isEmpty()) {
                paystackProbing = false;
                return;
            }
            probeWithPaystack(digits, priorityOrdered(banks), 0);
        });
    }

    private List<BankInstitution> priorityOrdered(List<BankInstitution> banks) {
        String[] priorities = {"opay", "palmpay", "moniepoint", "kuda",
                "smartcash", "momo", "access", "gtb", "zenith", "uba", "first"};
        List<BankInstitution> ordered = new ArrayList<>();
        for (String priority : priorities) {
            for (BankInstitution bank : banks) {
                if (bank.getName().toLowerCase(Locale.US).contains(priority)
                        && !ordered.contains(bank)) {
                    ordered.add(bank);
                }
            }
        }
        for (BankInstitution bank : banks) {
            if (!ordered.contains(bank)) {
                ordered.add(bank);
            }
        }
        return ordered;
    }

    private void probeWithPaystack(
            final String digits, final List<BankInstitution> banks, final int index) {
        String current = digitsOnly(binding.accountNumberInput.getText());
        if (index >= banks.size() || !current.equals(digits)) {
            paystackProbing = false;
            return;
        }
        paystackClient.resolveAccount(digits, banks.get(index),
                new PaystackClient.ResolveCallback() {
                    @Override
                    public void onResolved(String accountName, BankInstitution bank) {
                        paystackProbing = false;
                        bankSelected = true;
                        resolvedRecipient = new TransferRecipient(
                                accountName, digits, bank.getName(), "");
                        showResolvedBank(bank.getName(), logoUrlForBank(bank));
                        binding.recipientConfirmationItem.confirmationName
                                .setText(accountName);
                        binding.recipientConfirmationItem.getRoot()
                                .setVisibility(View.VISIBLE);
                        refreshNextState();
                    }

                    @Override
                    public void onFailed() {
                        probeWithPaystack(digits, banks, index + 1);
                    }
                });
    }

    private String logoUrlForBank(BankInstitution bank) {
        if (directoryBanks != null) {
            String name = bank.getName().toLowerCase(Locale.US);
            for (BankInstitution known : directoryBanks) {
                String knownName = known.getName().toLowerCase(Locale.US);
                if (knownName.contains(name) || name.contains(knownName)) {
                    return known.getLogoUrl();
                }
            }
        }
        return null;
    }

    private void showResolvedBank(String name, String logoUrl) {
        binding.selectedBankText.setText(name);
        binding.selectedBankText.setTextColor(color(R.color.ink));
        binding.selectedBankLogo.setVisibility(View.VISIBLE);
        ImageViewCompat.setImageTintList(binding.selectedBankLogo, null);
        if (logoUrl != null && !logoUrl.isEmpty()) {
            binding.selectedBankLogo.setImageResource(R.drawable.ic_bank_building);
            logoLoader.load(logoUrl, binding.selectedBankLogo);
        } else {
            int fallback = BankLogoResolver.fallbackForProvider(name);
            binding.selectedBankLogo.setImageResource(fallback);
            if (fallback == R.drawable.ic_bank_building) {
                ImageViewCompat.setImageTintList(binding.selectedBankLogo,
                        ColorStateList.valueOf(color(android.R.color.white)));
            }
        }
    }

    private void refreshNextState() {
        boolean ready = isFormReady();
        binding.nextButton.setBackgroundResource(
                ready ? R.drawable.bg_transfer_next_enabled : R.drawable.bg_transfer_next);
        binding.nextButton.setTextColor(color(
                ready ? R.color.transfer_next_text_enabled : R.color.transfer_next_text_disabled));
    }

    private boolean isFormReady() {
        return digitsOnly(binding.accountNumberInput.getText()).length() == 10
                && bankSelected
                && (trustedRecipient != null || resolvedRecipient != null);
    }

    private TransferRecipient effectiveRecipient() {
        return trustedRecipient != null ? trustedRecipient : resolvedRecipient;
    }

    private void openAmountScreen(TransferRecipient recipient) {
        if (context instanceof TransferActivity) {
            ((TransferActivity) context).openAmount(recipient);
        }
    }

    private void hideAccountSuggestions() {
        binding.accountSuggestionsContainer.removeAllViews();
        binding.accountSuggestionsContainer.setVisibility(View.GONE);
    }

    private void hideConfirmation() {
        binding.recipientConfirmationItem.getRoot().setVisibility(View.GONE);
    }

    private void resetBankField() {
        binding.selectedBankText.setText(R.string.select_bank);
        binding.selectedBankText.setTextColor(color(R.color.transfer_hint));
        binding.selectedBankLogo.setVisibility(View.GONE);
        resolvedRecipient = null;
    }

    private void applyProviderLogo(android.widget.ImageView image, String provider) {
        int fallback = BankLogoResolver.fallbackForProvider(provider);
        image.setImageResource(fallback);
        if (fallback == R.drawable.ic_bank_building) {
            ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(
                    color(android.R.color.white)));
        } else {
            ImageViewCompat.setImageTintList(image, null);
        }
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

    private String digitsOnly(CharSequence text) {
        StringBuilder digits = new StringBuilder();
        if (text != null) {
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (Character.isDigit(character)) {
                    digits.append(character);
                }
            }
        }
        return digits.toString();
    }

    private String formatAccountNumber(String digits) {
        StringBuilder formatted = new StringBuilder(digits.length() + 2);
        for (int index = 0; index < digits.length(); index++) {
            if (index == 3 || index == 6) {
                formatted.append(' ');
            }
            formatted.append(digits.charAt(index));
        }
        return formatted.toString();
    }

    @ColorInt
    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
