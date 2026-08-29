package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.data.WalletStore;
import damjay.palmpay.clone.databinding.ActivityAmountBinding;
import damjay.palmpay.clone.transfer.data.BankLogoLoader;
import damjay.palmpay.clone.transfer.data.BankLogoResolver;
import damjay.palmpay.clone.transfer.model.TransferRecipient;

/** Binds the trusted recipient, amount controls, and in-app numeric keypad. */
public final class AmountScreenController {
    private final Context context;
    private final ActivityAmountBinding binding;
    private final TransferRecipient recipient;

    public AmountScreenController(
            Context context,
            ActivityAmountBinding binding,
            TransferRecipient recipient) {
        this.context = context;
        this.binding = binding;
        this.recipient = recipient;
        this.logoLoader = new BankLogoLoader(context);
    }

    private final BankLogoLoader logoLoader;
    private boolean formattingAmount;

    public void bind() {
        binding.amountRecipientName.setText(recipient.getName());
        binding.amountRecipientAccount.setText(recipient.getAccountNumber());
        binding.amountRecipientProvider.setText(recipient.getProvider());
        WalletStore walletStore = new WalletStore(context);
        binding.amountBalanceText.setText(context.getString(
                R.string.balance_cashbox,
                walletStore.getBalanceDisplay()));

        if (recipient.getAvatarRes() != 0) {
            ImageViewCompat.setImageTintList(binding.amountRecipientLogo, null);
            binding.amountRecipientLogo.setImageResource(recipient.getAvatarRes());
            applyPalmPayVariant();
        } else {
            bindProviderLogo();
        }
        bindControls();
    }

    /** The official PalmPay amount page: person header, no provider, no
     *  toolbar extras, protection row above the card. */
    private void applyPalmPayVariant() {
        binding.amountTitle.setText(R.string.transfer_to_palmpay);
        binding.amountSupportButton.setVisibility(View.GONE);
        binding.amountHistoryButton.setVisibility(View.GONE);
        binding.amountRecipientProvider.setVisibility(View.GONE);
        android.view.ViewGroup card =
                (android.view.ViewGroup) binding.amountProtectionButton.getParent();
        android.view.ViewGroup outer =
                (android.view.ViewGroup) card.getParent();
        card.removeView(binding.amountProtectionButton);
        outer.addView(binding.amountProtectionButton,
                outer.indexOfChild(card));
    }

    private void bindProviderLogo() {
        String logoUrl = recipient.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            ImageViewCompat.setImageTintList(binding.amountRecipientLogo, null);
            logoLoader.load(logoUrl, binding.amountRecipientLogo);
        } else {
            int logo = BankLogoResolver.fallbackForProvider(recipient.getProvider());
            binding.amountRecipientLogo.setImageResource(logo);
            if (logo == R.drawable.ic_bank_building) {
                ImageViewCompat.setImageTintList(binding.amountRecipientLogo,
                        ColorStateList.valueOf(color(android.R.color.white)));
            } else {
                ImageViewCompat.setImageTintList(binding.amountRecipientLogo, null);
            }
        }
    }

    private void bindControls() {
        bindQuickAmount(binding.chip500, R.string.amount_500);
        bindQuickAmount(binding.chip1000, R.string.amount_1000);
        bindQuickAmount(binding.chip2000, R.string.amount_2000);
        bindQuickAmount(binding.chip5000, R.string.amount_5000);
        bindQuickAmount(binding.chip9999, R.string.amount_9999);
        bindQuickAmount(binding.chip10000, R.string.amount_10000);
        bindKey(binding.key1, "1");
        bindKey(binding.key2, "2");
        bindKey(binding.key3, "3");
        bindKey(binding.key4, "4");
        bindKey(binding.key5, "5");
        bindKey(binding.key6, "6");
        bindKey(binding.key7, "7");
        bindKey(binding.key8, "8");
        bindKey(binding.key9, "9");
        bindKey(binding.key00, "00");
        bindKey(binding.key0, "0");
        bindKey(binding.keyDot, ".");
        binding.keyBackspace.setOnClickListener(view -> deleteLastAmountCharacter());
        binding.amountKeypadNext.setOnClickListener(view -> {
            if (binding.amountKeypadNext.isEnabled()) {
                showMessage("Transfer amount entered");
            }
        });
        binding.amountClear.setOnClickListener(view ->
                binding.amountInput.setText(""));

        binding.amountProtectionButton.setOnClickListener(view -> showMessage(
                "Transfer protection selected"));
        binding.amountBackButton.setOnClickListener(view -> closeScreen());
        binding.amountInput.setShowSoftInputOnFocus(false);
        binding.amountInput.setOnFocusChangeListener((view, focused) -> {
            if (focused) {
                binding.amountKeypad.setVisibility(View.VISIBLE);
                hideSystemKeyboard();
            }
        });
        binding.noteInput.setOnFocusChangeListener((view, focused) -> {
            binding.amountKeypad.setVisibility(focused ? View.GONE : View.VISIBLE);
            if (!focused) {
                hideSystemKeyboard();
            }
        });
        binding.amountInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(
                    CharSequence text, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                if (formattingAmount) {
                    return;
                }
                String formatted = formatAmount(editable.toString());
                if (!formatted.contentEquals(editable)) {
                    formattingAmount = true;
                    binding.amountInput.setText(formatted);
                    binding.amountInput.setSelection(formatted.length());
                    formattingAmount = false;
                }
                refreshAmountState();
            }
        });
        refreshAmountState();
        binding.amountInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    && binding.amountKeypadNext.isEnabled()) {
                showMessage("Transfer amount entered");
                return true;
            }
            return false;
        });
        binding.amountInput.requestFocus();
    }

    private static final double MIN_AMOUNT = 10.0;
    private static final double MAX_AMOUNT = 200000.0;
    private static final double STAMP_DUTY_THRESHOLD = 10000.0;

    private void refreshAmountState() {
        String text = binding.amountInput.getText().toString();
        boolean hasText = !text.isEmpty();
        double value = parseAmount(text);
        boolean inRange = hasText && value >= MIN_AMOUNT && value <= MAX_AMOUNT;
        binding.amountClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
        binding.amountError.setVisibility(
                hasText && !inRange ? View.VISIBLE : View.GONE);
        binding.stampNoticeCard.setVisibility(
                hasText && value >= STAMP_DUTY_THRESHOLD ? View.VISIBLE : View.GONE);
        binding.amountKeypadNext.setEnabled(inRange);
        binding.amountKeypadNext.setBackgroundResource(inRange
                ? R.drawable.bg_keypad_next_enabled
                : R.drawable.bg_keypad_next);
    }

    private double parseAmount(String text) {
        try {
            return Double.parseDouble(text.replace(",", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String formatAmount(String raw) {
        String cleaned = raw.replace(",", "");
        String intPart = cleaned;
        String decPart = null;
        int dot = cleaned.indexOf('.');
        if (dot >= 0) {
            intPart = cleaned.substring(0, dot);
            decPart = cleaned.substring(dot + 1);
        }
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < intPart.length(); i++) {
            grouped.append(intPart.charAt(i));
            int remaining = intPart.length() - 1 - i;
            if (remaining > 0 && remaining % 3 == 0) {
                grouped.append(',');
            }
        }
        if (decPart != null) {
            grouped.append('.').append(decPart);
        }
        return grouped.toString();
    }

    private void bindQuickAmount(TextView chip, int amountRes) {
        chip.setOnClickListener(view -> binding.amountInput.setText(
                context.getString(amountRes).replace(",", "")));
    }

    private void bindKey(TextView key, String value) {
        key.setOnClickListener(view -> {
            String current = binding.amountInput.getText().toString();
            if (".".equals(value) && current.contains(".")) {
                return;
            }
            binding.amountInput.append(value);
        });
    }

    private void deleteLastAmountCharacter() {
        int length = binding.amountInput.length();
        if (length > 0) {
            binding.amountInput.getText().delete(length - 1, length);
        }
    }

    private void hideSystemKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.amountInput.getWindowToken(), 0);
        }
    }

    private void closeScreen() {
        if (context instanceof AmountActivity) {
            ((AmountActivity) context).finishFromAmount();
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @ColorInt
    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }
}
