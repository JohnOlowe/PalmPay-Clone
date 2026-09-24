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
import damjay.palmpay.clone.databinding.ActivityAddMoneyBinding;
import damjay.palmpay.clone.data.WalletStore;
import damjay.palmpay.clone.transfer.data.PaystackClient;

/**
 * Add Money: charges a VISA card through Paystack and sends the funds to
 * the PalmPay wallet. The destination is always PalmPay; for safety the
 * flow only ever succeeds for the owner's registered number - any other
 * destination fails with a generic message and no hint as to why.
 */
public final class AddMoneyActivity extends AppCompatActivity {
    private ActivityAddMoneyBinding binding;
    private AddMoneyController controller;

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, AddMoneyActivity.class);
        if (!(context instanceof AppCompatActivity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        binding = ActivityAddMoneyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        controller = new AddMoneyController(
                this,
                binding,
                new PaystackClient(this,
                        new WalletStore(this).getPaystackApiKey()));
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

    public void finishFromAddMoney() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        finishFromAddMoney();
    }

    @Override
    protected void onDestroy() {
        controller = null;
        binding = null;
        super.onDestroy();
    }
}
