package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityBankPickerBinding;
import damjay.palmpay.clone.transfer.data.BankDirectoryRepository;
import damjay.palmpay.clone.transfer.model.BankInstitution;

/** Full-screen searchable bank directory matching the supplied Select bank reference. */
public final class BankPickerActivity extends AppCompatActivity {
    public static final String EXTRA_BANK_NAME = "extra_bank_name";
    public static final String EXTRA_BANK_CODE = "extra_bank_code";
    public static final String EXTRA_BANK_LOGO = "extra_bank_logo";

    private ActivityBankPickerBinding binding;
    private BankPickerScreenController controller;

    public static Intent createIntent(Context context) {
        return new Intent(context, BankPickerActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        binding = ActivityBankPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        controller = new BankPickerScreenController(
                this,
                binding,
                new BankDirectoryRepository(),
                this::returnSelectedBank);
        controller.bind();
    }

    private void configureWindow() {
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

    private void returnSelectedBank(BankInstitution bank) {
        Intent result = new Intent();
        result.putExtra(EXTRA_BANK_NAME, bank.getName());
        result.putExtra(EXTRA_BANK_CODE, bank.getCode());
        result.putExtra(EXTRA_BANK_LOGO, bank.getLogoUrl());
        setResult(RESULT_OK, result);
        finishFromBankPicker();
    }

    public void finishFromBankPicker() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        finishFromBankPicker();
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
