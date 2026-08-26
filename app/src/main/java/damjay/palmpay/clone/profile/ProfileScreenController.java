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
        binding.profileBackButton.setOnClickListener(view -> close());
        binding.saveBalanceButton.setOnClickListener(view -> saveBalance());
    }

    private void saveBalance() {
        if (!walletStore.saveBalance(binding.profileBalanceInput.getText().toString())) {
            binding.profileBalanceInput.setError(context.getString(R.string.invalid_balance));
            return;
        }
        Toast.makeText(context, R.string.balance_saved, Toast.LENGTH_SHORT).show();
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
