package damjay.palmpay.clone.transfer.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.widget.ImageViewCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean formattingAmount;
    private Dialog paymentSheet;
    private Dialog loadingOverlay;

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
                        ColorStateList.valueOf(color(R.color.transfer_hint)));
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
                showPaymentSheet();
            }
        });
        binding.amountClear.setOnClickListener(view ->
                binding.amountInput.setText(""));

        binding.amountProtectionButton.setOnClickListener(view -> showMessage(
                "Transfer protection selected"));
        binding.amountBackButton.setOnClickListener(view -> closeScreen());
        binding.amountInput.setShowSoftInputOnFocus(false);
        // The digits render at display size now, so the range hint keeps a
        // readable size of its own.
        android.text.SpannableString hint = new android.text.SpannableString(
                context.getString(R.string.amount_hint));
        hint.setSpan(new android.text.style.AbsoluteSizeSpan(16, true),
                0, hint.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.amountInput.setHint(hint);
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
                showPaymentSheet();
                return true;
            }
            return false;
        });
        binding.amountInput.requestFocus();
    }

    private static final double MIN_AMOUNT = 10.0;
    private static final double MAX_AMOUNT = 200000.0;
    private static final double STAMP_DUTY_THRESHOLD = 10000.0;

    private static final String[] PLACE_NAMES = {
            "Ones", "Tens", "Hundreds", "Thousands", "Tens of Thousands",
            "Hundreds of Thousands", "Millions", "Tens of Millions",
            "Hundreds of Millions", "Billions"
    };

    private void refreshAmountState() {
        String text = binding.amountInput.getText().toString();
        boolean hasText = !text.isEmpty();
        updatePlaceTooltip(text);
        double value = parseAmount(text);
        boolean inRange = hasText && value >= MIN_AMOUNT && value <= MAX_AMOUNT;
        binding.amountClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
        binding.amountUnderline.setVisibility(hasText ? View.VISIBLE : View.GONE);
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

    /** The official chips always land in the field with ".00" appended. */
    private void bindQuickAmount(TextView chip, int amountRes) {
        chip.setOnClickListener(view -> binding.amountInput.setText(
                context.getString(amountRes) + ".00"));
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

    /** Shows the official place-value bubble above the entered amount. */
    private void updatePlaceTooltip(String text) {
        String intPart = text.replace(",", "");
        int dot = intPart.indexOf('.');
        if (dot >= 0) {
            intPart = intPart.substring(0, dot);
        }
        int length = intPart.length();
        if (length == 0) {
            // Only the bubble hides; its slot keeps its height so the
            // digits never shift.
            binding.amountTooltipWrap.setVisibility(View.INVISIBLE);
            return;
        }
        int index = Math.min(length, PLACE_NAMES.length) - 1;
        binding.amountPlaceTooltip.setText(PLACE_NAMES[index]);
        binding.amountTooltipWrap.setVisibility(View.VISIBLE);
    }

    /**
     * The official confirmation: a bottom sheet over a dimmed screen with
     * the purple total, the detail card and the CashBox payment method.
     */
    private void showPaymentSheet() {
        if (paymentSheet != null && paymentSheet.isShowing()) {
            return;
        }
        double value = parseAmount(binding.amountInput.getText().toString());
        String decimal = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.US)).format(value);

        Dialog dialog = new Dialog(context);
        View sheet = LayoutInflater.from(context)
                .inflate(R.layout.dialog_payment_sheet, null);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.5f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        TextView amount = sheet.findViewById(R.id.sheet_amount);
        amount.setText(context.getString(R.string.naira_sign) + decimal);
        TextView rowAmount = sheet.findViewById(R.id.sheet_row_amount);
        rowAmount.setText(context.getString(R.string.naira_sign) + decimal);
        TextView feeStrike = sheet.findViewById(R.id.sheet_fee_strike);
        feeStrike.setText(context.getString(R.string.naira_sign) + "10.00");
        feeStrike.setPaintFlags(
                feeStrike.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        TextView rowAccount = sheet.findViewById(R.id.sheet_row_account);
        rowAccount.setText(recipient.getAccountNumber());
        TextView rowName = sheet.findViewById(R.id.sheet_row_name);
        rowName.setText(recipient.getName());
        TextView rowBank = sheet.findViewById(R.id.sheet_row_bank);
        rowBank.setText(recipient.getProvider());
        TextView cashboxBalance =
                sheet.findViewById(R.id.sheet_cashbox_balance);
        String balance = new WalletStore(context).getBalanceDisplay();
        cashboxBalance.setText("(" + balance + ")");
        cashboxBalance.setPaintFlags(
                cashboxBalance.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        sheet.findViewById(R.id.sheet_close)
                .setOnClickListener(view -> dialog.dismiss());
        sheet.findViewById(R.id.sheet_confirm)
                .setOnClickListener(view -> confirmToPay(decimal));

        paymentSheet = dialog;
        dialog.show();
    }

    /** Confirm to Pay raises the app's own Touch ID sheet, like the original. */
    private void confirmToPay(final String decimal) {
        if (loadingOverlay != null && loadingOverlay.isShowing()) {
            return;
        }
        showTouchIdSheet(decimal);
    }

    private Dialog touchIdSheet;
    private Dialog pinSheet;
    private Dialog incompleteDialog;
    private final StringBuilder pinCode = new StringBuilder();
    private boolean pinVisible;

    private void showTouchIdSheet(final String decimal) {
        if (touchIdSheet != null && touchIdSheet.isShowing()) {
            return;
        }
        final Dialog dialog = new Dialog(context);
        View sheet = LayoutInflater.from(context)
                .inflate(R.layout.dialog_touchid_sheet, null);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.5f);
        }
        TextView amount = sheet.findViewById(R.id.touchid_amount);
        amount.setText(context.getString(R.string.naira_sign) + decimal);
        sheet.findViewById(R.id.touchid_close)
                .setOnClickListener(view -> showIncompleteDialog(decimal));
        sheet.findViewById(R.id.touchid_pay)
                .setOnClickListener(view -> startBiometric(decimal));
        sheet.findViewById(R.id.touchid_verify_pin)
                .setOnClickListener(view -> {
                    dialog.dismiss();
                    showPinSheet(decimal);
                });
        touchIdSheet = dialog;
        dialog.show();
    }

    /** The leave-confirmation card shown over the Touch ID sheet. */
    private void showIncompleteDialog(final String decimal) {
        if (incompleteDialog != null && incompleteDialog.isShowing()) {
            return;
        }
        final Dialog dialog = new Dialog(context);
        View sheet = LayoutInflater.from(context)
                .inflate(R.layout.dialog_payment_incomplete, null);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout((int) (context.getResources()
                    .getDisplayMetrics().widthPixels * 0.86f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setDimAmount(0.35f);
        }
        sheet.findViewById(R.id.incomplete_close)
                .setOnClickListener(view -> dialog.dismiss());
        sheet.findViewById(R.id.incomplete_continue)
                .setOnClickListener(view -> dialog.dismiss());
        sheet.findViewById(R.id.incomplete_leave)
                .setOnClickListener(view -> {
                    dialog.dismiss();
                    dismissAuthSheets();
                    if (paymentSheet != null && paymentSheet.isShowing()) {
                        paymentSheet.dismiss();
                    }
                });
        incompleteDialog = dialog;
        dialog.show();
    }

    /** System fingerprint prompt: original title, no subtitle, "Use PIN". */
    private void startBiometric(final String decimal) {
        if (!(context instanceof FragmentActivity)) {
            showPinSheet(decimal);
            return;
        }
        androidx.biometric.BiometricManager manager =
                androidx.biometric.BiometricManager.from(context);
        if (manager.canAuthenticate(androidx.biometric.BiometricManager
                .Authenticators.BIOMETRIC_WEAK)
                != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            showPinSheet(decimal);
            return;
        }
        FragmentActivity activity = (FragmentActivity) context;
        BiometricPrompt prompt = new BiometricPrompt(activity,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        activity.runOnUiThread(() -> {
                            dismissAuthSheets();
                            proceedToLoading(decimal);
                        });
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                            CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            activity.runOnUiThread(() -> {
                                if (touchIdSheet != null) {
                                    touchIdSheet.dismiss();
                                }
                                showPinSheet(decimal);
                            });
                        }
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(context.getString(R.string.touchid_title))
                .setNegativeButtonText(
                        context.getString(R.string.touchid_negative))
                .setAllowedAuthenticators(androidx.biometric.BiometricManager
                        .Authenticators.BIOMETRIC_WEAK)
                .build();
        prompt.authenticate(info);
    }

    /** The app's own PIN entry sheet with the secure-input keypad. */
    private void showPinSheet(final String decimal) {
        if (pinSheet != null && pinSheet.isShowing()) {
            return;
        }
        pinCode.setLength(0);
        pinVisible = false;
        final Dialog dialog = new Dialog(context);
        final View sheet = LayoutInflater.from(context)
                .inflate(R.layout.dialog_pin_sheet, null);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.5f);
        }
        final TextView[] boxes = new TextView[] {
                sheet.findViewById(R.id.pin_box_1),
                sheet.findViewById(R.id.pin_box_2),
                sheet.findViewById(R.id.pin_box_3),
                sheet.findViewById(R.id.pin_box_4)};
        final ImageView eye = sheet.findViewById(R.id.pin_eye);
        eye.setOnClickListener(view -> {
            pinVisible = !pinVisible;
            eye.setImageResource(pinVisible
                    ? R.drawable.ic_eye_visible : R.drawable.ic_eye_off);
            renderPin(boxes);
        });
        View.OnClickListener digit = view -> {
            if (pinCode.length() < 4) {
                pinCode.append(((TextView) view).getText());
                renderPin(boxes);
                if (pinCode.length() == 4) {
                    handler.postDelayed(() -> {
                        dialog.dismiss();
                        dismissAuthSheets();
                        proceedToLoading(decimal);
                    }, 220);
                }
            }
        };
        for (int id : new int[] {R.id.pin_key_0, R.id.pin_key_1,
                R.id.pin_key_2, R.id.pin_key_3, R.id.pin_key_4,
                R.id.pin_key_5, R.id.pin_key_6, R.id.pin_key_7,
                R.id.pin_key_8, R.id.pin_key_9}) {
            sheet.findViewById(id).setOnClickListener(digit);
        }
        sheet.findViewById(R.id.pin_key_back).setOnClickListener(view -> {
            if (pinCode.length() > 0) {
                pinCode.deleteCharAt(pinCode.length() - 1);
                renderPin(boxes);
            }
        });
        sheet.findViewById(R.id.pin_close)
                .setOnClickListener(view -> dialog.dismiss());
        sheet.findViewById(R.id.pin_forgot)
                .setOnClickListener(view -> showMessage(
                        context.getString(R.string.pin_forgot_toast)));
        sheet.findViewById(R.id.pin_touchid)
                .setOnClickListener(view -> {
                    dialog.dismiss();
                    startBiometric(decimal);
                });
        pinSheet = dialog;
        dialog.show();
    }

    private void renderPin(TextView[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            boolean filled = i < pinCode.length();
            boxes[i].setBackgroundResource(filled
                    ? R.drawable.bg_pin_box_filled : R.drawable.bg_pin_box);
            boxes[i].setText(filled
                    ? (pinVisible ? String.valueOf(pinCode.charAt(i)) : "\u25CF")
                    : "");
        }
    }

    private void dismissAuthSheets() {
        if (touchIdSheet != null && touchIdSheet.isShowing()) {
            touchIdSheet.dismiss();
        }
        if (pinSheet != null && pinSheet.isShowing()) {
            pinSheet.dismiss();
        }
        if (incompleteDialog != null && incompleteDialog.isShowing()) {
            incompleteDialog.dismiss();
        }
    }

    /** The dimmed screen with the fluid PalmPay logo card. */
    private void proceedToLoading(String decimal) {
        if (loadingOverlay != null && loadingOverlay.isShowing()) {
            return;
        }
        dismissAuthSheets();
        Dialog loading = new Dialog(context,
                android.R.style.Theme_Translucent_NoTitleBar);
        loading.setContentView(LayoutInflater.from(context)
                .inflate(R.layout.dialog_loading_logo, null));
        Window window = loading.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setDimAmount(0f);
        }
        loading.setCancelable(false);
        loadingOverlay = loading;
        loading.show();

        // One fill + hold + drain cycle, then settle the transfer.
        handler.postDelayed(() -> {
            if (loadingOverlay != null && loadingOverlay.isShowing()) {
                loadingOverlay.dismiss();
            }
            if (paymentSheet != null && paymentSheet.isShowing()) {
                paymentSheet.dismiss();
            }
            damjay.palmpay.clone.data.NotificationHelper.postHeadsUp(context,
                    context.getString(R.string.notif_transfer_success_title),
                    context.getString(R.string.notif_transfer_success_body,
                            decimal, recipient.getName()));
            TransferSuccessActivity.start(context, decimal, recipient);
            closeScreen();
        }, 2600);
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
