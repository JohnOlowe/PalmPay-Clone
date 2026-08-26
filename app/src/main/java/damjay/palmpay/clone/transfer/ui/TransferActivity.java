package damjay.palmpay.clone.transfer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityTransferBinding;
import damjay.palmpay.clone.transfer.data.LocalTransferRepository;
import damjay.palmpay.clone.transfer.model.BankInstitution;
import damjay.palmpay.clone.transfer.model.TransferRecipient;

/** Transfer-to-bank screen, kept as a separate activity for the requested page transition. */
public final class TransferActivity extends AppCompatActivity {
    private static final int REQUEST_BANK = 410;

    private ActivityTransferBinding binding;
    private TransferScreenController controller;

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, TransferActivity.class);
        if (!(context instanceof AppCompatActivity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).overridePendingTransition(0, 0);
        }
    }

    public void openBankPicker() {
        openBankPicker("");
    }

    public void openBankPicker(String accountDigits) {
        startActivityForResult(
                BankPickerActivity.createIntent(this, accountDigits), REQUEST_BANK);
        overridePendingTransition(0, 0);
    }

    public void openAmount(TransferRecipient recipient) {
        startActivity(AmountActivity.createIntent(this, recipient));
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BANK && resultCode == Activity.RESULT_OK && data != null
                && controller != null) {
            String name = data.getStringExtra(BankPickerActivity.EXTRA_BANK_NAME);
            String code = data.getStringExtra(BankPickerActivity.EXTRA_BANK_CODE);
            String logo = data.getStringExtra(BankPickerActivity.EXTRA_BANK_LOGO);
            if (name != null) {
                controller.selectBank(new BankInstitution(
                        name, "", code == null ? "" : code, logo == null ? "" : logo));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        binding = ActivityTransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        controller = new TransferScreenController(
                this,
                binding,
                new LocalTransferRepository());
        controller.bind();
        playEnterAnimation(binding.getRoot());
    }

    private void configureWindow() {
        // The transfer toolbar sits immediately below the status bar, as in the reference.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        boolean darkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        getWindow().setStatusBarColor(ContextCompat.getColor(
                this, R.color.transfer_toolbar_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(
                this, R.color.transfer_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(!darkMode);
        insetsController.setAppearanceLightNavigationBars(!darkMode);
    }

    private void playEnterAnimation(@NonNull View root) {
        root.setPivotX(root.getResources().getDisplayMetrics().widthPixels / 2f);
        root.setPivotY(root.getResources().getDisplayMetrics().heightPixels / 2f);
        root.setScaleX(0.90f);
        root.setScaleY(0.90f);
        root.setAlpha(0.78f);
        root.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(100L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void finishFromTransfer() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        finishFromTransfer();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.onDestroy();
        }
        controller = null;
        binding = null;
        super.onDestroy();
    }
}
