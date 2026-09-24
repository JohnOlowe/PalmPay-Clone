package damjay.palmpay.clone.profile;

import android.content.Context;

import android.widget.Toast;

import damjay.palmpay.clone.R;
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
    }

    private void saveAll() {
        if (!walletStore.saveBalance(binding.profileBalanceInput.getText().toString())) {
            binding.profileBalanceInput.setError(context.getString(R.string.invalid_balance));
            return;
        }
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
