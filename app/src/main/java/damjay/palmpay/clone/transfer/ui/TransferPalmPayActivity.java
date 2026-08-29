package damjay.palmpay.clone.transfer.ui;

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
import damjay.palmpay.clone.databinding.ActivityTransferPalmpayBinding;
import damjay.palmpay.clone.transfer.model.TransferRecipient;

/** Transfer-to-PalmPay screen, mirroring the official reference. */
public final class TransferPalmPayActivity extends AppCompatActivity {
    private ActivityTransferPalmpayBinding binding;
    private TransferPalmPayController controller;

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, TransferPalmPayActivity.class);
        if (!(context instanceof AppCompatActivity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).overridePendingTransition(0, 0);
        }
    }

    public void openAmount(TransferRecipient recipient) {
        startActivity(AmountActivity.createIntent(this, recipient));
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        binding = ActivityTransferPalmpayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        controller = new TransferPalmPayController(this, binding);
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
        controller = null;
        binding = null;
        super.onDestroy();
    }
}
