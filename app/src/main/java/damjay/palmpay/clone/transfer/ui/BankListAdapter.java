package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.BankItemBinding;
import damjay.palmpay.clone.transfer.data.BankLogoLoader;
import damjay.palmpay.clone.transfer.model.BankInstitution;

/** List adapter that renders every bank returned by the remote directory. */
public final class BankListAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final BankLogoLoader logoLoader;
    private final List<BankInstitution> allBanks = new ArrayList<>();
    private final List<BankInstitution> visibleBanks = new ArrayList<>();

    public BankListAdapter(Context context, BankLogoLoader logoLoader) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.logoLoader = logoLoader;
    }

    public void replaceBanks(List<BankInstitution> banks) {
        allBanks.clear();
        allBanks.addAll(banks);
        visibleBanks.clear();
        visibleBanks.addAll(banks);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.US);
        visibleBanks.clear();
        for (BankInstitution bank : allBanks) {
            if (normalized.isEmpty()
                    || bank.getName().toLowerCase(Locale.US).contains(normalized)
                    || bank.getCode().toLowerCase(Locale.US).contains(normalized)) {
                visibleBanks.add(bank);
            }
        }
        notifyDataSetChanged();
    }

    public BankInstitution getBank(int position) {
        return visibleBanks.get(position);
    }

    @Override
    public int getCount() {
        return visibleBanks.size();
    }

    @Override
    public BankInstitution getItem(int position) {
        return visibleBanks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View recycled, ViewGroup parent) {
        BankItemBinding binding;
        if (recycled == null) {
            binding = BankItemBinding.inflate(inflater, parent, false);
            binding.getRoot().setTag(binding);
        } else {
            binding = (BankItemBinding) recycled.getTag();
        }

        BankInstitution bank = getItem(position);
        binding.bankName.setText(bank.getName());
        binding.bankCode.setText(context.getString(R.string.bank_code_format, bank.getCode()));
        binding.bankLogo.setTag(null);
        binding.bankLogo.setImageResource(R.drawable.ic_bank_building);
        ImageViewCompat.setImageTintList(binding.bankLogo, ColorStateList.valueOf(
                ContextCompat.getColor(context, android.R.color.white)));
        logoLoader.load(bank.getLogoUrl(), binding.bankLogo);
        return binding.getRoot();
    }
}
