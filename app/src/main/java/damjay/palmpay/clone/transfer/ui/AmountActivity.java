package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.view.WindowManager;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityAmountBinding;
import damjay.palmpay.clone.transfer.model.TransferRecipient;

/** Amount entry page opened from a trusted transfer-history recipient. */
public final class AmountActivity extends AppCompatActivity {
    private static final String EXTRA_RECIPIENT_NAME = "extra_recipient_name";
    private static final String EXTRA_RECIPIENT_ACCOUNT = "extra_recipient_account";
    private static final String EXTRA_RECIPIENT_PROVIDER = "extra_recipient_provider";
    private static final int ENTER_DURATION_MS = 100;

    private ActivityAmountBinding binding;
    private AmountScreenController controller;

    public static Intent createIntent(Context context, TransferRecipient recipient) {
        Intent intent = new Intent(context, AmountActivity.class);
        intent.putExtra(EXTRA_RECIPIENT_NAME, recipient.getName());
        intent.putExtra(EXTRA_RECIPIENT_ACCOUNT, recipient.getAccountNumber());
        intent.putExtra(EXTRA_RECIPIENT_PROVIDER, recipient.getProvider());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        binding = ActivityAmountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        TransferRecipient recipient = new TransferRecipient(
                getIntent().getStringExtra(EXTRA_RECIPIENT_NAME),
                getIntent().getStringExtra(EXTRA_RECIPIENT_ACCOUNT),
                getIntent().getStringExtra(EXTRA_RECIPIENT_PROVIDER),
                "");
        controller = new AmountScreenController(this, binding, recipient);
        controller.bind();
        playEnterAnimation(binding.getRoot());
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
                .setDuration(ENTER_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void showAmountKeyboard() {
        if (binding == null) {
            return;
        }
        binding.amountInput.setShowSoftInputOnFocus(true);
        binding.amountInput.requestFocus();
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        insetsController.show(WindowInsetsCompat.Type.ime());
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(binding.amountInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void finishFromAmount() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && binding != null) {
            binding.amountInput.postDelayed(this::showAmountKeyboard, 250L);
        }
    }

    @Override
    public void onBackPressed() {
        finishFromAmount();
    }

    @Override
    protected void onDestroy() {
        controller = null;
        binding = null;
        super.onDestroy();
    }
}
