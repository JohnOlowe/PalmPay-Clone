package damjay.palmpay.clone;

import android.graphics.Color;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import damjay.palmpay.clone.databinding.ActivityMainBinding;
import damjay.palmpay.clone.ui.HomeScreenController;

/** Entry point for the PalmPay-inspired home screen. */
public final class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private HomeScreenController homeScreenController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarInsets(binding.getRoot());

        homeScreenController = new HomeScreenController(this, binding);
        homeScreenController.bind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeScreenController != null) {
            homeScreenController.refreshBalance();
        }
    }

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean darkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        insetsController.setAppearanceLightStatusBars(!darkMode);
        insetsController.setAppearanceLightNavigationBars(!darkMode);
    }

    private void applySystemBarInsets(@NonNull View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // The content is edge-to-edge so the background reaches the status
            // and navigation bars, while interactive controls stay clear of them.
            binding.homeScroll.setPadding(
                    binding.homeScroll.getPaddingLeft(),
                    systemBars.top,
                    binding.homeScroll.getPaddingRight(),
                    binding.homeScroll.getPaddingBottom());
            // The bottom bar has an explicit 64dp height matching the
            // reference. Its background is allowed to extend behind the
            // transparent system navigation area instead of increasing its
            // measured height.
            binding.bottomBar.setPadding(0, 0, 0, 0);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onDestroy() {
        if (homeScreenController != null) {
            homeScreenController.release();
        }
        homeScreenController = null;
        binding = null;
        super.onDestroy();
    }
}
