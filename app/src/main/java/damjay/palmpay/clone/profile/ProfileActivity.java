package damjay.palmpay.clone.profile;

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
import damjay.palmpay.clone.databinding.ActivityProfileBinding;

/** Customisation screen for the persisted demo wallet balance. */
public final class ProfileActivity extends AppCompatActivity {
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, ProfileActivity.class);
        if (!(context instanceof AppCompatActivity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).overridePendingTransition(0, 0);
        }
    }

    private ActivityProfileBinding binding;
    private ProfileScreenController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        controller = new ProfileScreenController(this, binding);
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

    public void finishFromProfile() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        finishFromProfile();
    }

    @Override
    protected void onDestroy() {
        controller = null;
        binding = null;
        super.onDestroy();
    }
}
