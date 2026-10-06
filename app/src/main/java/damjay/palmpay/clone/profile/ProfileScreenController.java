package damjay.palmpay.clone.profile;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.Toast;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.data.NotificationHelper;
import damjay.palmpay.clone.data.WalletStore;
import damjay.palmpay.clone.databinding.ActivityProfileBinding;

/** Coordinates the editable balance form and persists it through WalletStore. */
public final class ProfileScreenController {
    private final Context context;
    private final ActivityProfileBinding binding;
    private final WalletStore walletStore;

    public ProfileScreenController(Context context, ActivityProfileBinding binding) {
        this.context = context;
        this.binding = binding;
        this.walletStore = new WalletStore(context);
    }

    public void bind() {
        binding.profileBalanceInput.setText(stripCurrency(walletStore.getBalanceDisplay()));
        binding.profileNameInput.setText(walletStore.getDisplayName());
        binding.profilePaystackInput.setText(walletStore.getPaystackApiKey());
        binding.profileStripeInput.setText(walletStore.getStripeApiKey());
        binding.profileFlutterwaveInput.setText(
                walletStore.getFlutterwaveApiKey());
        binding.profileFlutterwaveEncInput.setText(
                walletStore.getFlutterwaveEncKey());
        binding.profileEmailInput.setText(
                "customer@email.com".equals(walletStore.getPaystackEmail())
                        ? "" : walletStore.getPaystackEmail());
        binding.profileBackButton.setOnClickListener(view -> close());
        binding.saveBalanceButton.setOnClickListener(view -> saveAll());
        binding.profileSecurityButton.setOnClickListener(
                view -> openSecurityEnrollment());
        binding.profileNotificationsButton.setOnClickListener(
                view -> showNotificationConsent());
        int mode = walletStore.getPinMode();
        binding.profilePinModes.check(mode == 1
                ? R.id.pin_mode_fixed
                : mode == 2 ? R.id.pin_mode_first_fails
                        : R.id.pin_mode_any);
        binding.profilePinInput.setText(walletStore.getPaymentPin());
        binding.profilePinInput.setVisibility(
                mode == 1 ? View.VISIBLE : View.GONE);
        binding.profilePinModes.setOnCheckedChangeListener(
                (group, checkedId) -> binding.profilePinInput.setVisibility(
                        checkedId == R.id.pin_mode_fixed
                                ? View.VISIBLE : View.GONE));
    }

    private static final int NOTIFICATION_PERMISSION_REQUEST = 777;

    /** Register fingerprint / face / screen lock through the system page. */
    private void openSecurityEnrollment() {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 30) {
                intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                intent.putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        androidx.biometric.BiometricManager.Authenticators
                                .BIOMETRIC_WEAK
                                | androidx.biometric.BiometricManager
                                        .Authenticators.DEVICE_CREDENTIAL);
            } else {
                intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            }
            context.startActivity(intent);
        } catch (Exception exception) {
            try {
                context.startActivity(
                        new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Exception fallback) {
                Toast.makeText(context, "Security settings unavailable",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** In-app consent sheet: Allow asks the system, Later slides away. */
    private void showNotificationConsent() {
        final Dialog dialog = new Dialog(context);
        final View sheet = LayoutInflater.from(context).inflate(
                R.layout.dialog_notification_consent, null);
        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.5f);
        }
        sheet.findViewById(R.id.notif_allow).setOnClickListener(view -> {
            dialog.dismiss();
            requestNotificationPermission();
        });
        sheet.findViewById(R.id.notif_later).setOnClickListener(view ->
                sheet.animate()
                        .translationY(Math.max(sheet.getHeight(), 300))
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(dialog::dismiss));
        dialog.show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && context instanceof Activity) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[] {android.Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        } else {
            onNotificationPermissionResult(true);
        }
    }

    public void onRequestPermissionsResult(
            int requestCode, int[] grantResults) {
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        onNotificationPermissionResult(granted);
    }

    private void onNotificationPermissionResult(boolean granted) {
        if (granted) {
            NotificationHelper.postHeadsUp(context,
                    context.getString(R.string.notif_enabled_title),
                    context.getString(R.string.notif_enabled_body));
        } else {
            Toast.makeText(context, "Notifications stay off",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAll() {
        if (!walletStore.saveBalance(binding.profileBalanceInput.getText().toString())) {
            binding.profileBalanceInput.setError(context.getString(R.string.invalid_balance));
            return;
        }
        int pinMode = binding.profilePinModes.getCheckedRadioButtonId()
                == R.id.pin_mode_fixed ? 1
                : binding.profilePinModes.getCheckedRadioButtonId()
                        == R.id.pin_mode_first_fails ? 2 : 0;
        if (pinMode == 1) {
            String pin = binding.profilePinInput.getText().toString();
            if (pin.length() != 4) {
                binding.profilePinInput.setError(
                        context.getString(R.string.pin_invalid));
                return;
            }
            walletStore.savePaymentPin(pin);
        }
        walletStore.savePinMode(pinMode);
        walletStore.saveDisplayName(binding.profileNameInput.getText().toString());
        walletStore.savePaystackApiKey(binding.profilePaystackInput.getText().toString());
        walletStore.savePaystackEmail(binding.profileEmailInput.getText().toString());
        walletStore.saveStripeApiKey(binding.profileStripeInput.getText().toString());
        walletStore.saveFlutterwaveApiKey(
                binding.profileFlutterwaveInput.getText().toString());
        walletStore.saveFlutterwaveEncKey(
                binding.profileFlutterwaveEncInput.getText().toString());
        Toast.makeText(context, R.string.changes_saved, Toast.LENGTH_SHORT).show();
        close();
    }

    private String stripCurrency(String displayValue) {
        return displayValue.replace("₦", "").replace(",", "").trim();
    }

    private void close() {
        if (context instanceof ProfileActivity) {
            ((ProfileActivity) context).finishFromProfile();
        }
    }
}
