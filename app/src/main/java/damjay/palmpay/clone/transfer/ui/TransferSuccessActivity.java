package damjay.palmpay.clone.transfer.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityTransferSuccessBinding;
import damjay.palmpay.clone.transfer.model.TransferRecipient;

/**
 * The official post-payment page: green tick, the amount, the receipt card,
 * Share / Favorite actions and Complete - with the "Set a Password for
 * Login" sheet sliding in over it, whose Later button slides back down.
 */
public final class TransferSuccessActivity extends AppCompatActivity {
    private static final String EXTRA_AMOUNT = "extra_success_amount";
    private static final String EXTRA_NAME = "extra_success_name";
    private static final String EXTRA_ACCOUNT = "extra_success_account";
    private static final String EXTRA_PROVIDER = "extra_success_provider";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityTransferSuccessBinding binding;
    private Dialog passwordSheet;

    public static void start(Context context, String amount,
                             TransferRecipient recipient) {
        Intent intent = new Intent(context, TransferSuccessActivity.class);
        intent.putExtra(EXTRA_AMOUNT, amount);
        intent.putExtra(EXTRA_NAME, recipient.getName());
        intent.putExtra(EXTRA_ACCOUNT, recipient.getAccountNumber());
        intent.putExtra(EXTRA_PROVIDER, recipient.getProvider());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransferSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        if (name == null) {
            finish();
            return;
        }

        binding.successAmount.setText(
                getString(R.string.naira_sign) + amount);
        binding.successName.setText(name);
        binding.successAccount.setText(getIntent().getStringExtra(EXTRA_ACCOUNT));
        binding.successBank.setText(getIntent().getStringExtra(EXTRA_PROVIDER));

        binding.successViewDetails.setOnClickListener(view ->
                toast(getString(R.string.success_view_details)));
        binding.successShare.setOnClickListener(view ->
                toast(getString(R.string.success_share)));
        binding.successFavorite.setOnClickListener(view ->
                toast(getString(R.string.success_favorite)));
        binding.successComplete.setOnClickListener(view -> finish());

        // The password prompt arrives a beat after the page, like the app.
        handler.postDelayed(this::showPasswordSheet, 600);
    }

    private void showPasswordSheet() {
        if (isFinishing() || passwordSheet != null) {
            return;
        }
        final Dialog dialog = new Dialog(this);
        final View sheet = LayoutInflater.from(this)
                .inflate(R.layout.dialog_password_sheet, null);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.5f);
        }
        sheet.findViewById(R.id.password_set).setOnClickListener(view -> {
            dialog.dismiss();
            toast(getString(R.string.password_set_toast));
        });
        sheet.findViewById(R.id.password_later).setOnClickListener(view ->
                sheet.animate()
                        .translationY(Math.max(sheet.getHeight(), 300))
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(dialog::dismiss));
        passwordSheet = dialog;
        dialog.show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (passwordSheet != null && passwordSheet.isShowing()) {
            passwordSheet.dismiss();
        }
        super.onDestroy();
    }
}
