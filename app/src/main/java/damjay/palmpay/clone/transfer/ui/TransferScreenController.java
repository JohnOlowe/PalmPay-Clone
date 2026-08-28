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
import java.util.Locale;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.AccountSuggestionItemBinding;
import damjay.palmpay.clone.databinding.ActivityTransferBinding;
import damjay.palmpay.clone.databinding.MatchingBankItemBinding;
import damjay.palmpay.clone.databinding.RecipientItemBinding;
import damjay.palmpay.clone.databinding.TransferShortcutItemBinding;
import damjay.palmpay.clone.databinding.TransferTabsBinding;
import damjay.palmpay.clone.data.WalletStore;
import damjay.palmpay.clone.transfer.data.BankDirectoryRepository;
import damjay.palmpay.clone.transfer.data.BankNameNormalizer;
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
    private static final int DIGITS_REQUIRED = 10;

    private final Context context;
    private final ActivityTransferBinding binding;
    private final LayoutInflater inflater;
    private final TransferRepository repository;
    private final BankLogoLoader logoLoader;
    private final BankDirectoryRepository directoryRepository =
            new BankDirectoryRepository();
    private final List<TransferRecipient> transferHistory = new ArrayList<>();
    private List<BankInstitution> directoryBanks;
    private boolean directoryLoading;
    private static final String[] WALLET_PROVIDERS = {"opay", "palmpay",
            "moniepoint", "smartcash", "kuda", "momo"};
    private static final int MAX_VERIFIED_PROBES = 10;

    private final WalletStore walletStore;
    private final List<String> listedBankNames = new ArrayList<>();
    private final List<DirectoryCallback> pendingDirectoryCallbacks = new ArrayList<>();
    private final java.util.Map<String, String> resolvedNames = new java.util.HashMap<>();
    private final java.util.Set<String> failedBankKeys = new java.util.HashSet<>();
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
        this.walletStore = new WalletStore(context);
        this.logoLoader = new BankLogoLoader(context);
    }

    public void bind() {
        transferHistory.clear();
        transferHistory.addAll(repository.getRecentRecipients());
        directoryRepository.load((banks, fromNetwork) -> {
            directoryBanks = banks;
            List<DirectoryCallback> callbacks =
                    new ArrayList<>(pendingDirectoryCallbacks);
            pendingDirectoryCallbacks.clear();
            for (DirectoryCallback pending : callbacks) {
                pending.onDirectory(banks);
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
                // A disabled Next must stay silent and inert.
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
            trustedRecipient = null;
            resolvedRecipient = null;
            bankSelected = false;
            hideConfirmation();
            hideInvalidAccountBanner();
            resetBankField();
            renderAccountSuggestions(digits);
            showMatchingBanks(digits);
            refreshNextState();
            return;
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
            ((TransferActivity) context).openBankPicker(
                    digitsOnly(binding.accountNumberInput.getText()));
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
        String digits = digitsOnly(binding.accountNumberInput.getText());
        if (digits.length() == DIGITS_REQUIRED) {
            String cachedName = cachedNameFor(bank);
            if (cachedName != null) {
                // Already verified while the matching list was built: no
                // second query needed.
                resolvedRecipient = new TransferRecipient(
                        cachedName, digits, bank.getName(), "");
                resolvedRecipient.setLogoUrl(bank.getLogoUrl());
                showConfirmationName(cachedName);
            } else if (knownUnresolvable(bank)) {
                showInvalidAccountBanner();
            } else {
                showStatus(R.string.verifying_account_name);
                TransferRecipient historyMatch =
                        historyRecipientFor(digits, bank.getName());
                if (historyMatch != null) {
                    trustedRecipient = historyMatch;
                    showConfirmationName(historyMatch.getName());
                    hideStatus();
                } else {
                    resolveNameViaPaystack(digits, bank);
                }
            }
        }
        refreshNextState();
    }



    /**
     * Shows every bank that could own the completed account number, in
     * directory order with no priority, so the user always makes the choice.
     */
    private void showMatchingBanks(final String digits) {
        binding.matchingBanksContainer.removeAllViews();
        binding.matchingBanksContainer.setVisibility(View.GONE);
        listedBankNames.clear();
        resolvedNames.clear();
        failedBankKeys.clear();
        showStatus(R.string.matching_banks);

        // Banks that already received a transfer always work for the account.
        for (TransferRecipient recipient : transferHistory) {
            if (recipient.getAccountNumber().equals(digits)) {
                addMatchingBankRow(digits, bankForProvider(recipient.getProvider()),
                        recipient.getName());
            }
        }

        final String key = walletStore.getPaystackApiKey();
        if (!key.isEmpty()) {
            List<PaystackClient.VerifiedBank> verified =
                    new PaystackClient(context, key).loadVerified(digits);
            if (!verified.isEmpty()) {
                for (PaystackClient.VerifiedBank entry : verified) {
                    BankInstitution bank = new BankInstitution(
                            entry.name, "", entry.code, "");
                    addMatchingBankRow(digits, bank, entry.accountName);
                }
                finishMatchingList();
                return;
            }
        }
        if (key.isEmpty()) {
            withDirectory(directory -> {
                if (!digitsStillCurrent(digits)) {
                    return;
                }
                for (BankInstitution bank
                        : NubanBankResolver.candidateBanks(digits, directory)) {
                    addMatchingBankRow(digits, bank, null);
                }
                finishMatchingList();
            });
            return;
        }

        // Verified mode: only banks that resolve a holder name are listed.
        // Candidates come from Paystack's own (fast) bank list, and every
        // probe is fired at once on the client's thread pool below.
        final PaystackClient client = new PaystackClient(context, key);
        client.listBanks(directory -> {
            if (!digitsStillCurrent(digits)) {
                return;
            }
            List<BankInstitution> targets = new ArrayList<>();
            for (String wallet : WALLET_PROVIDERS) {
                for (BankInstitution bank : directory) {
                    if (bank.getName().toLowerCase(Locale.US).contains(wallet)
                            && !containsBankNamed(targets, bank.getName())) {
                        targets.add(bank);
                        break;
                    }
                }
            }
            int probes = 0;
            for (BankInstitution bank
                    : NubanBankResolver.candidateBanks(digits, directory)) {
                if (probes++ >= MAX_VERIFIED_PROBES) {
                    break;
                }
                if (!containsBankNamed(targets, bank.getName())) {
                    targets.add(bank);
                }
            }
            if (targets.isEmpty()) {
                finishMatchingList();
                return;
            }
            final java.util.concurrent.atomic.AtomicInteger outstanding =
                    new java.util.concurrent.atomic.AtomicInteger(targets.size());
            for (final BankInstitution bank : targets) {
                client.resolveAccount(digits, bank,
                        new PaystackClient.ResolveCallback() {
                            @Override
                            public void onResolved(
                                    String accountName, BankInstitution resolved) {
                                client.persistVerified(digits, resolved, accountName);
                                if (digitsStillCurrent(digits)
                                        && !listedBankNames.contains(
                                                BankNameNormalizer.canonical(
                                                        resolved.getName()))) {
                                    addMatchingBankRow(digits,
                                            bankForProvider(resolved.getName()),
                                            accountName);
                                }
                                if (outstanding.decrementAndGet() == 0) {
                                    finishMatchingList();
                                }
                            }

                            @Override
                            public void onFailed() {
                                // This bank does not own the account: remember
                                // so selecting it later never spins uselessly.
                                failedBankKeys.add(
                                        BankNameNormalizer.canonical(bank.getName()));
                                failedBankKeys.add(
                                        bank.getName().toLowerCase(Locale.US));
                                if (outstanding.decrementAndGet() == 0) {
                                    finishMatchingList();
                                }
                            }
                        });
            }
        });
    }

    private boolean digitsStillCurrent(String digits) {
        return digitsOnly(binding.accountNumberInput.getText()).equals(digits);
    }

    private void finishMatchingList() {
        hideStatus();
        boolean any = !listedBankNames.isEmpty();
        binding.bankExtraDivider.setVisibility(any ? View.VISIBLE : View.GONE);
        binding.matchingBanksContainer.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private void addMatchingBankRow(
            String digits, BankInstitution bank, String resolvedName) {
        listedBankNames.add(BankNameNormalizer.canonical(bank.getName()));
        if (resolvedName != null) {
            resolvedNames.put(BankNameNormalizer.canonical(bank.getName()), resolvedName);
            resolvedNames.put(bank.getName().toLowerCase(Locale.US), resolvedName);
        }
        MatchingBankItemBinding item = MatchingBankItemBinding.inflate(
                inflater, binding.matchingBanksContainer, false);
        item.matchingBankName.setText(bank.getName());
        applyBankLogo(item.matchingBankLogo, bank);
        item.getRoot().setOnClickListener(view ->
                selectMatchedBank(digits, bank));
        binding.matchingBanksContainer.addView(item.getRoot());
    }

    private interface DirectoryCallback {
        void onDirectory(List<BankInstitution> banks);
    }

    private void withDirectory(final DirectoryCallback callback) {
        if (directoryBanks != null) {
            callback.onDirectory(directoryBanks);
            return;
        }
        pendingDirectoryCallbacks.add(callback);
        if (!directoryLoading) {
            directoryLoading = true;
            directoryRepository.load((banks, fromNetwork) -> {
                directoryLoading = false;
                directoryBanks = banks;
                List<DirectoryCallback> callbacks =
                        new ArrayList<>(pendingDirectoryCallbacks);
                pendingDirectoryCallbacks.clear();
                for (DirectoryCallback pending : callbacks) {
                    pending.onDirectory(banks);
                }
            });
        }
    }

    private boolean containsBankNamed(List<BankInstitution> banks, String name) {
        String key = BankNameNormalizer.canonical(name);
        for (BankInstitution bank : banks) {
            if (BankNameNormalizer.canonical(bank.getName()).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private BankInstitution bankForProvider(String provider) {
        if (directoryBanks != null) {
            for (BankInstitution bank : directoryBanks) {
                if (bank.getName().equalsIgnoreCase(provider)) {
                    return bank;
                }
            }
        }
        return new BankInstitution(provider, "", "", "");
    }

    private void applyBankLogo(android.widget.ImageView image, BankInstitution bank) {
        String url = bank.getLogoUrl();
        ImageViewCompat.setImageTintList(image, null);
        if (url != null && !url.isEmpty()) {
            image.setImageResource(R.drawable.ic_bank_building);
            logoLoader.load(url, image);
        } else {
            int fallback = BankLogoResolver.fallbackForProvider(bank.getName());
            image.setImageResource(fallback);
            if (fallback == R.drawable.ic_bank_building) {
                ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(
                        color(android.R.color.white)));
            }
        }
    }

    private void selectMatchedBank(final String digits, final BankInstitution bank) {
        bankSelected = true;
        trustedRecipient = null;
        resolvedRecipient = null;
        hideConfirmation();
        binding.matchingBanksContainer.setVisibility(View.GONE);
        hideInvalidAccountBanner();
        showResolvedBank(bank.getName(), bank.getLogoUrl());
        String cachedName = cachedNameFor(bank);
        if (cachedName != null) {
            resolvedRecipient = new TransferRecipient(
                    cachedName, digits, bank.getName(), "");
            resolvedRecipient.setLogoUrl(bank.getLogoUrl());
            showConfirmationName(cachedName);
            refreshNextState();
            return;
        }
        if (knownUnresolvable(bank)) {
            showInvalidAccountBanner();
            refreshNextState();
            return;
        }
        showStatus(R.string.verifying_account_name);
        TransferRecipient historyMatch = historyRecipientFor(digits, bank.getName());
        if (historyMatch != null) {
            trustedRecipient = historyMatch;
            showConfirmationName(historyMatch.getName());
            hideStatus();
            refreshNextState();
            return;
        }
        resolveNameViaPaystack(digits, bank);
        refreshNextState();
    }



    private TransferRecipient historyRecipientFor(String digits, String bankName) {
        for (TransferRecipient recipient : transferHistory) {
            if (recipient.getAccountNumber().equals(digits)
                    && recipient.getProvider().equalsIgnoreCase(bankName)) {
                return recipient;
            }
        }
        return null;
    }

    private void showConfirmationName(String name) {
        binding.recipientConfirmationItem.confirmationName.setText(name);
        binding.recipientConfirmationItem.getRoot().setVisibility(View.VISIBLE);
    }

    /** Resolves the holder name through Paystack for the bank the user chose. */
    private void resolveNameViaPaystack(final String digits, final BankInstitution bank) {
        String key = walletStore.getPaystackApiKey();
        if (key.isEmpty()) {
            hideStatus();
            return;
        }
        final PaystackClient client = new PaystackClient(context, key);
        client.listBanks(banks -> {
            BankInstitution paystackBank = null;
            String name = bank.getName().toLowerCase(Locale.US);
            for (BankInstitution candidate : banks) {
                String candidateName = candidate.getName().toLowerCase(Locale.US);
                if (candidateName.contains(name) || name.contains(candidateName)) {
                    paystackBank = candidate;
                    break;
                }
            }
            final BankInstitution target = paystackBank != null
                    ? paystackBank
                    : new BankInstitution(bank.getName(), "", bank.getCode(), "");
            client.resolveAccount(digits, target,
                    new PaystackClient.ResolveCallback() {
                        @Override
                        public void onResolved(
                                String accountName, BankInstitution resolved) {
                            resolvedRecipient = new TransferRecipient(
                                    accountName, digits, bank.getName(), "");
                            resolvedRecipient.setLogoUrl(bank.getLogoUrl());
                            showConfirmationName(accountName);
                            hideStatus();
                            hideInvalidAccountBanner();
                            refreshNextState();
                        }

                        @Override
                        public void onFailed() {
                            hideStatus();
                            showInvalidAccountBanner();
                        }
                    });
        });
    }

    private void showInvalidAccountBanner() {
        binding.invalidAccountBanner.setVisibility(View.VISIBLE);
    }

    private void hideInvalidAccountBanner() {
        binding.invalidAccountBanner.setVisibility(View.GONE);
    }

    private String cachedNameFor(BankInstitution bank) {
        String name = resolvedNames.get(BankNameNormalizer.canonical(bank.getName()));
        if (name == null) {
            name = resolvedNames.get(bank.getName().toLowerCase(Locale.US));
        }
        return name;
    }

    private boolean knownUnresolvable(BankInstitution bank) {
        return failedBankKeys.contains(BankNameNormalizer.canonical(bank.getName()))
                || failedBankKeys.contains(bank.getName().toLowerCase(Locale.US));
    }

    private void showStatus(int textRes) {
        binding.bankExtraDivider.setVisibility(View.VISIBLE);
        binding.bankStatusRow.setVisibility(View.VISIBLE);
        binding.bankStatusText.setText(textRes);
    }

    private void hideStatus() {
        binding.bankStatusRow.setVisibility(View.GONE);
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
        // Next only activates once the account name has actually resolved,
        // either from transfer history or from Paystack verification.
        return digitsOnly(binding.accountNumberInput.getText()).length() == 10
                && bankSelected
                && (trustedRecipient != null || resolvedRecipient != null);
    }

    private TransferRecipient effectiveRecipient() {
        if (trustedRecipient != null) {
            return trustedRecipient;
        }
        if (resolvedRecipient != null) {
            return resolvedRecipient;
        }
        return new TransferRecipient(
                "",
                digitsOnly(binding.accountNumberInput.getText()),
                binding.selectedBankText.getText().toString(),
                "");
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
        binding.matchingBanksContainer.setVisibility(View.GONE);
        binding.bankStatusRow.setVisibility(View.GONE);
        binding.bankExtraDivider.setVisibility(View.GONE);
        hideInvalidAccountBanner();
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
