package damjay.palmpay.clone;

import android.graphics.Color;
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

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);
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
            binding.bottomNavigation.setPadding(
                    binding.bottomNavigation.getPaddingLeft(),
                    binding.bottomNavigation.getPaddingTop(),
                    binding.bottomNavigation.getPaddingRight(),
                    systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onDestroy() {
        homeScreenController = null;
        binding = null;
        super.onDestroy();
    }
}
