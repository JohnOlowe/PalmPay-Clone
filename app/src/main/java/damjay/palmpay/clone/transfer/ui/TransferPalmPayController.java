package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityTransferPalmpayBinding;
import damjay.palmpay.clone.databinding.PalmpayContactItemBinding;
import damjay.palmpay.clone.databinding.TransferShortcutItemBinding;
import damjay.palmpay.clone.transfer.data.PalmPayCatalog;
import damjay.palmpay.clone.transfer.model.PalmPayContact;
import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/**
 * Behaviour for the To PalmPay screen: account entry with suggestions,
 * Recent / Favorites / PalmPay Contacts tabs and the verified-recipient
 * hand-off into the amount page. Kept separate from the bank controller so
 * each flow can evolve without touching the other.
 */
public final class TransferPalmPayController {
    private static final String PROVIDER = "PalmPay";

    private final Context context;
    private final ActivityTransferPalmpayBinding binding;
    private final LayoutInflater inflater;
    private final List<PalmPayContact> allContacts = new ArrayList<>();
    private boolean formattingAccount;
    private PalmPayContact selectedContact;
    private damjay.palmpay.clone.transfer.data.PaystackClient paystackClient;
    private int activeTab = TAB_RECENT;

    private static final int TAB_RECENT = 0;
    private static final int TAB_FAVORITES = 1;
    private static final int TAB_CONTACTS = 2;

    public TransferPalmPayController(
            Context context, ActivityTransferPalmpayBinding binding) {
        this.context = context;
        this.binding = binding;
        this.inflater = LayoutInflater.from(context);
    }

    public void bind() {
        paystackClient = new damjay.palmpay.clone.transfer.data.PaystackClient(
                context, new damjay.palmpay.clone.data.WalletStore(context)
                        .getPaystackApiKey());
        allContacts.clear();
        allContacts.addAll(PalmPayCatalog.recent());
        allContacts.addAll(PalmPayCatalog.contacts());

        renderShortcuts(PalmPayCatalog.shortcuts());
        bindTabs();
        renderList();
        bindForm();
        bindToolbar();
    }

    private void bindToolbar() {
        binding.ppBackButton.setOnClickListener(view -> closeScreen());
        binding.ppSupportButton.setOnClickListener(view ->
                showMessage("Transfer support selected"));
        binding.ppHistoryButton.setOnClickListener(view ->
                showMessage("Transfer history selected"));
        binding.ppProtectionButton.setOnClickListener(view ->
                showMessage("Transfer protection selected"));
        binding.ppSearchButton.setOnClickListener(view ->
                showMessage("Search contacts selected"));
        binding.ppViewAll.setOnClickListener(view ->
                showMessage("All contacts selected"));
    }

    private void renderShortcuts(List<TransferShortcut> shortcuts) {
        binding.ppShortcutsContainer.removeAllViews();
        for (TransferShortcut shortcut : shortcuts) {
            TransferShortcutItemBinding item = TransferShortcutItemBinding.inflate(
                    inflater, binding.ppShortcutsContainer, false);
            item.shortcutIcon.setImageResource(shortcut.getIconRes());
            item.shortcutTitle.setText(shortcut.getTitleRes());
            item.getRoot().setContentDescription(
                    context.getString(shortcut.getTitleRes()));
            item.getRoot().setOnClickListener(view -> showMessage(
                    context.getString(shortcut.getTitleRes()) + " selected"));
            binding.ppShortcutsContainer.addView(item.getRoot(),
                    new LinearLayout.LayoutParams(0, dp(87), 1f));
        }
    }

    private void bindTabs() {
        binding.ppRecentTab.setOnClickListener(view -> selectTab(TAB_RECENT));
        binding.ppFavoritesTab.setOnClickListener(view -> selectTab(TAB_FAVORITES));
        binding.ppContactsTab.setOnClickListener(view -> selectTab(TAB_CONTACTS));
    }

    private void selectTab(int tab) {
        activeTab = tab;
        applyTabStyles();
        renderList();
    }

    private void applyTabStyles() {
        binding.ppRecentTabText.setTextColor(color(
                activeTab == TAB_RECENT ? R.color.brand_purple : R.color.ink));
        binding.ppFavoritesTabText.setTextColor(color(
                activeTab == TAB_FAVORITES ? R.color.brand_purple : R.color.ink));
        binding.ppContactsTabText.setTextColor(color(
                activeTab == TAB_CONTACTS ? R.color.brand_purple : R.color.ink));
        binding.ppRecentIndicator.setVisibility(
                activeTab == TAB_RECENT ? View.VISIBLE : View.INVISIBLE);
        binding.ppFavoritesIndicator.setVisibility(
                activeTab == TAB_FAVORITES ? View.VISIBLE : View.INVISIBLE);
        binding.ppContactsIndicator.setVisibility(
                activeTab == TAB_CONTACTS ? View.VISIBLE : View.INVISIBLE);
    }

    private void renderList() {
        binding.ppListContainer.removeAllViews();
        List<PalmPayContact> rows;
        if (activeTab == TAB_RECENT) {
            rows = PalmPayCatalog.recent();
        } else if (activeTab == TAB_FAVORITES) {
            rows = PalmPayCatalog.favorites();
        } else {
            rows = PalmPayCatalog.contacts();
        }
        for (PalmPayContact contact : rows) {
            addContactRow(contact, activeTab == TAB_RECENT, false, null);
        }
    }

    private void addContactRow(
            PalmPayContact contact, boolean showDate, boolean suggestion,
            String typedDigits) {
        PalmpayContactItemBinding item = PalmpayContactItemBinding.inflate(
                inflater, suggestion
                        ? binding.ppSuggestionsContainer
                        : binding.ppListContainer, false);
        item.contactAvatar.setImageResource(contact.getAvatarRes());
        item.contactName.setText(contact.getName());
        item.contactAgentTag.setVisibility(
                contact.isAgent() ? View.VISIBLE : View.GONE);
        if (suggestion && typedDigits != null) {
            item.contactAccount.setText(progressiveAccount(
                    contact.getAccountNumber(), typedDigits));
        } else {
            item.contactAccount.setText(contact.getAccountNumber());
        }
        if (showDate && contact.getLastTransferDate() != null) {
            item.contactDate.setVisibility(View.VISIBLE);
            item.contactDate.setText(context.getString(
                    R.string.last_transfer_on, contact.getLastTransferDate()));
        } else {
            item.contactDate.setVisibility(View.GONE);
        }
        item.getRoot().setContentDescription(contact.getName());
        if (suggestion) {
            item.getRoot().setOnClickListener(view ->
                    fillFromSuggestion(contact));
        } else {
            item.getRoot().setOnClickListener(view ->
                    openAmountFor(contact));
        }
        (suggestion ? binding.ppSuggestionsContainer : binding.ppListContainer)
                .addView(item.getRoot());
    }

    /** Tapping a history row goes straight to the amount page, like To Bank. */
    private void openAmountFor(PalmPayContact contact) {
        TransferRecipient recipient = new TransferRecipient(
                contact.getName(),
                contact.getAccountNumber(),
                PROVIDER,
                "");
        recipient.setAvatarRes(contact.getAvatarRes());
        ((TransferPalmPayActivity) context).openAmount(recipient);
    }

    /** Tapping a suggestion fills the account and shows the name. */
    private void fillFromSuggestion(PalmPayContact contact) {
        String formatted = formatInput(contact.getAccountNumber());
        formattingAccount = true;
        binding.ppAccountInput.setText(formatted);
        binding.ppAccountInput.setSelection(formatted.length());
        formattingAccount = false;
        binding.ppClearButton.setVisibility(View.VISIBLE);
        binding.ppSuggestionsContainer.setVisibility(View.GONE);
        binding.ppInvalidBanner.setVisibility(View.GONE);
        selectedContact = contact;
        binding.ppConfirmationItem.confirmationName.setText(contact.getName());
        binding.ppConfirmationItem.getRoot().setVisibility(View.VISIBLE);
        refreshNextState();
    }

    private void bindForm() {
        binding.ppClearButton.setOnClickListener(view -> {
            binding.ppAccountInput.setText("");
            binding.ppAccountInput.requestFocus();
        });
        binding.ppNextButton.setOnClickListener(view -> {
            if (selectedContact == null) {
                return;
            }
            TransferRecipient recipient = new TransferRecipient(
                    selectedContact.getName(),
                    selectedContact.getAccountNumber(),
                    PROVIDER,
                    "");
            recipient.setAvatarRes(selectedContact.getAvatarRes());
            ((TransferPalmPayActivity) context).openAmount(recipient);
        });
        binding.ppAccountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(
                    CharSequence text, int start, int before, int count) {
                // Formatting happens in afterTextChanged.
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (formattingAccount) {
                    return;
                }
                String digits = digitsOnly(editable);
                String formatted = formatInput(digits);
                if (!formatted.contentEquals(editable)) {
                    formattingAccount = true;
                    binding.ppAccountInput.setText(formatted);
                    binding.ppAccountInput.setSelection(formatted.length());
                    formattingAccount = false;
                }
                binding.ppClearButton.setVisibility(
                        digits.isEmpty() ? View.GONE : View.VISIBLE);
                updateMatches(digits);
            }
        });
        refreshNextState();
    }

    private void updateMatches(String digits) {
        selectedContact = null;
        binding.ppConfirmationItem.getRoot().setVisibility(View.GONE);
        binding.ppStatusRow.setVisibility(View.GONE);
        binding.ppSuggestionsContainer.removeAllViews();
        if (digits.isEmpty()) {
            binding.ppSuggestionsContainer.setVisibility(View.GONE);
            binding.ppInvalidBanner.setVisibility(View.GONE);
            refreshNextState();
            return;
        }
        for (PalmPayContact contact : allContacts) {
            if (contact.getAccountNumber().startsWith(digits)) {
                addContactRow(contact, false, true, digits);
            }
        }
        boolean any = binding.ppSuggestionsContainer.getChildCount() > 0;
        if (digits.length() >= 10) {
            // Complete number: name must come from a real lookup.
            binding.ppSuggestionsContainer.setVisibility(View.GONE);
            PalmPayContact exact = null;
            for (PalmPayContact contact : allContacts) {
                if (contact.getAccountNumber().equals(digits)) {
                    exact = contact;
                    break;
                }
            }
            if (exact != null) {
                fillFromSuggestion(exact);
            } else {
                resolveNameViaPaystack(digits);
            }
        } else {
            binding.ppSuggestionsContainer.setVisibility(any ? View.VISIBLE : View.GONE);
            binding.ppInvalidBanner.setVisibility(View.GONE);
        }
        refreshNextState();
    }

    /** Single fast resolve against PalmPay itself. */
    private void resolveNameViaPaystack(final String digits) {
        binding.ppInvalidBanner.setVisibility(View.GONE);
        binding.ppStatusRow.setVisibility(View.VISIBLE);
        if (paystackClient == null || !paystackClient.isConfigured()) {
            binding.ppStatusRow.setVisibility(View.GONE);
            binding.ppInvalidBanner.setVisibility(View.VISIBLE);
            return;
        }
        paystackClient.listBanks(banks -> {
            damjay.palmpay.clone.transfer.model.BankInstitution palmPay = null;
            for (damjay.palmpay.clone.transfer.model.BankInstitution bank : banks) {
                if (bank.getName().toLowerCase(java.util.Locale.US)
                        .contains("palmpay")) {
                    palmPay = bank;
                    break;
                }
            }
            if (palmPay == null) {
                binding.ppStatusRow.setVisibility(View.GONE);
                binding.ppInvalidBanner.setVisibility(View.VISIBLE);
                return;
            }
            paystackClient.resolveAccount(digits, palmPay,
                    new damjay.palmpay.clone.transfer.data.PaystackClient
                            .ResolveCallback() {
                        @Override
                        public void onResolved(
                                String accountName,
                                damjay.palmpay.clone.transfer.model.BankInstitution bank) {
                            binding.ppStatusRow.setVisibility(View.GONE);
                            if (!digitsOnly(binding.ppAccountInput.getText())
                                    .equals(digits)) {
                                return;
                            }
                            selectedContact = new PalmPayContact(
                                    accountName, digits,
                                    R.drawable.avatar_reference, false, null);
                            binding.ppConfirmationItem.confirmationName
                                    .setText(accountName);
                            binding.ppConfirmationItem.getRoot()
                                    .setVisibility(View.VISIBLE);
                            binding.ppInvalidBanner.setVisibility(View.GONE);
                            refreshNextState();
                        }

                        @Override
                        public void onFailed() {
                            binding.ppStatusRow.setVisibility(View.GONE);
                            if (!digitsOnly(binding.ppAccountInput.getText())
                                    .equals(digits)) {
                                return;
                            }
                            binding.ppInvalidBanner.setVisibility(View.VISIBLE);
                            refreshNextState();
                        }
                    });
        });
    }

    private void refreshNextState() {
        boolean ready = selectedContact != null;
        binding.ppNextButton.setBackgroundResource(ready
                ? R.drawable.bg_transfer_next_enabled
                : R.drawable.bg_transfer_next);
        binding.ppNextButton.setTextColor(color(ready
                ? R.color.transfer_next_text_enabled
                : R.color.transfer_next_text_disabled));
    }

    private SpannableString progressiveAccount(String account, String typed) {
        SpannableString span = new SpannableString(account);
        int highlighted = Math.min(typed.length(), account.length());
        if (highlighted > 0) {
            span.setSpan(new ForegroundColorSpan(color(R.color.brand_purple)),
                    0, highlighted, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (highlighted < account.length()) {
            span.setSpan(new ForegroundColorSpan(color(R.color.ink_secondary)),
                    highlighted, account.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return span;
    }

    private String digitsOnly(CharSequence text) {
        StringBuilder digits = new StringBuilder();
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.isDigit(c)) {
                    digits.append(c);
                }
            }
        }
        return digits.toString();
    }

    /** Phone numbers (11 digits, leading 0) group 4-3-4; accounts 3-3-4. */
    private String formatInput(String digits) {
        StringBuilder formatted = new StringBuilder(digits.length() + 2);
        boolean phone = digits.length() > 10 && digits.startsWith("0");
        for (int i = 0; i < digits.length(); i++) {
            if (phone ? (i == 4 || i == 7) : (i == 3 || i == 6)) {
                formatted.append(' ');
            }
            formatted.append(digits.charAt(i));
        }
        return formatted.toString();
    }

    /** Paystack/PalmPay accounts carry no leading 0: 08080868957 -> 8080868957. */
    private String probeDigitsFor(String raw) {
        if (raw.length() == 11 && raw.startsWith("0")) {
            return raw.substring(1);
        }
        return raw;
    }

    private void closeScreen() {
        if (context instanceof TransferPalmPayActivity) {
            ((TransferPalmPayActivity) context).finishFromTransfer();
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
        return Math.round(value
                * context.getResources().getDisplayMetrics().density);
    }
}
