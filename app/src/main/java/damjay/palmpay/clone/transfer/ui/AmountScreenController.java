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
    }

    public void bind() {
        binding.amountRecipientName.setText(recipient.getName());
        binding.amountRecipientAccount.setText(recipient.getAccountNumber());
        binding.amountRecipientProvider.setText(recipient.getProvider());
        WalletStore walletStore = new WalletStore(context);
        binding.amountBalanceText.setText(context.getString(
                R.string.balance_cashbox,
                walletStore.getBalanceDisplay()));

        int logo = BankLogoResolver.fallbackForProvider(recipient.getProvider());
        binding.amountRecipientLogo.setImageResource(logo);
        if (logo == R.drawable.ic_bank_building) {
            ImageViewCompat.setImageTintList(binding.amountRecipientLogo, ColorStateList.valueOf(
                    color(android.R.color.white)));
        } else {
            ImageViewCompat.setImageTintList(binding.amountRecipientLogo, null);
        }

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
        binding.amountKeypadNext.setOnClickListener(view -> showMessage(
                "Transfer amount entered"));

        binding.amountProtectionButton.setOnClickListener(view -> showMessage(
                "Transfer protection selected"));
        binding.amountBackButton.setOnClickListener(view -> closeScreen());
        binding.amountSupportButton.setOnClickListener(view -> showMessage(
                "Transfer support selected"));
        binding.amountHistoryButton.setOnClickListener(view -> showMessage(
                "Transfer history selected"));
        binding.amountInput.setShowSoftInputOnFocus(false);
        binding.amountInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                showMessage("Transfer amount entered");
                return true;
            }
            return false;
        });
        binding.amountInput.requestFocus();
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
        TextView input = binding.amountInput;
        int length = input.length();
        if (length > 0) {
            input.getText().delete(length - 1, length);
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
