package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.BankSectionHeaderBinding;
import damjay.palmpay.clone.databinding.FrequentBankItemBinding;
import damjay.palmpay.clone.databinding.PickerBankItemBinding;
import damjay.palmpay.clone.databinding.ActivityBankPickerBinding;
import damjay.palmpay.clone.transfer.data.BankDirectoryRepository;
import damjay.palmpay.clone.transfer.data.NubanBankResolver;
import damjay.palmpay.clone.transfer.data.BankLogoLoader;
import damjay.palmpay.clone.transfer.model.BankInstitution;

/** Renders the full bank directory and keeps search/filter logic out of the activity. */
public final class BankPickerScreenController {
    private final Context context;
    private final ActivityBankPickerBinding binding;
    private final LayoutInflater inflater;
    private final BankDirectoryRepository repository;
    private final BankLogoLoader logoLoader = new BankLogoLoader();
    private final OnBankPickedListener listener;
    private final String accountDigits;
    private final List<BankInstitution> allBanks = new ArrayList<>();
    private String query = "";
    private boolean destroyed;

    public interface OnBankPickedListener {
        void onBankPicked(BankInstitution bank);
    }

    public BankPickerScreenController(
            Context context,
            ActivityBankPickerBinding binding,
            BankDirectoryRepository repository,
            String accountDigits,
            OnBankPickedListener listener) {
        this.context = context;
        this.binding = binding;
        this.repository = repository;
        this.listener = listener;
        this.accountDigits = accountDigits == null ? "" : accountDigits;
        this.inflater = LayoutInflater.from(context);
    }

    public void bind() {
        binding.bankPickerBackButton.setOnClickListener(view -> close());
        binding.bankPickerSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                query = text.toString();
                renderDirectory();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No-op.
            }
        });

        allBanks.addAll(repository.fallbackBanks());
        renderDirectory();
        repository.load((banks, fromNetwork) -> {
            if (destroyed) {
                return;
            }
            allBanks.clear();
            allBanks.addAll(banks);
            renderDirectory();
            binding.bankDirectoryLoading.setVisibility(View.GONE);
        });
    }

    public void onDestroy() {
        destroyed = true;
        repository.close();
        logoLoader.close();
    }

    private void renderDirectory() {
        List<BankInstitution> filtered = filteredBanks();
        boolean searching = !query.trim().isEmpty();
        binding.frequentlyUsedTitle.setVisibility(searching ? View.GONE : View.VISIBLE);
        binding.frequentBanksGrid.setVisibility(searching ? View.GONE : View.VISIBLE);
        renderFrequentBanks();
        renderAllBanks(filtered);
    }

    private void renderFrequentBanks() {
        binding.frequentBanksGrid.removeAllViews();
        if (!query.trim().isEmpty()) {
            return;
        }
        List<BankInstitution> frequent = frequentBanks();
        for (BankInstitution bank : frequent) {
            FrequentBankItemBinding item = FrequentBankItemBinding.inflate(
                    inflater, binding.frequentBanksGrid, false);
            item.frequentBankName.setText(bank.getName());
            prepareLogo(item.frequentBankLogo, bank, true);
            item.getRoot().setContentDescription(bank.getName());
            item.getRoot().setOnClickListener(view -> listener.onBankPicked(bank));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            params.height = dp(87);
            params.setGravity(Gravity.FILL_HORIZONTAL);
            binding.frequentBanksGrid.addView(item.getRoot(), params);
        }
    }

    private void renderAllBanks(List<BankInstitution> banks) {
        binding.allBanksContainer.removeAllViews();
        char currentSection = 0;
        for (BankInstitution bank : banks) {
            char section = sectionFor(bank.getName());
            if (section != currentSection) {
                currentSection = section;
                BankSectionHeaderBinding header = BankSectionHeaderBinding.inflate(
                        inflater, binding.allBanksContainer, false);
                header.bankSectionTitle.setText(String.valueOf(section));
                binding.allBanksContainer.addView(header.getRoot(), new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
            }

            PickerBankItemBinding item = PickerBankItemBinding.inflate(
                    inflater, binding.allBanksContainer, false);
            item.pickerBankName.setText(bank.getName());
            prepareLogo(item.pickerBankLogo, bank, false);
            item.getRoot().setContentDescription(bank.getName());
            item.getRoot().setOnClickListener(view -> listener.onBankPicked(bank));
            binding.allBanksContainer.addView(item.getRoot(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        }
    }

    private List<BankInstitution> filteredBanks() {
        String normalized = query.trim().toLowerCase(Locale.US);
        List<BankInstitution> result = new ArrayList<>();
        for (BankInstitution bank : allBanks) {
            if (normalized.isEmpty()
                    || bank.getName().toLowerCase(Locale.US).contains(normalized)
                    || bank.getCode().toLowerCase(Locale.US).contains(normalized)) {
                result.add(bank);
            }
        }
        Collections.sort(result, Comparator.comparing(
                bank -> bank.getName().toLowerCase(Locale.US)));
        return result;
    }

    private List<BankInstitution> frequentBanks() {
        if (accountDigits.length() == 10) {
            List<BankInstitution> verified =
                    NubanBankResolver.candidateBanks(accountDigits, allBanks);
            if (!verified.isEmpty()) {
                return verified;
            }
        }
        String[] wanted = {
                "palmpay", "access bank", "first bank", "moniepoint", "opay", "united bank for africa"
        };
        List<BankInstitution> result = new ArrayList<>();
        for (String search : wanted) {
            BankInstitution found = null;
            for (BankInstitution bank : allBanks) {
                if (bank.getName().toLowerCase(Locale.US).contains(search)) {
                    found = bank;
                    break;
                }
            }
            if (found == null) {
                found = fallbackFrequent(search);
            }
            result.add(found);
        }
        return result;
    }

    private BankInstitution fallbackFrequent(String search) {
        if (search.equals("palmpay")) {
            return new BankInstitution("PalmPay", "palmpay", "999991", "");
        }
        if (search.equals("access bank")) {
            return new BankInstitution("Access Bank", "access-bank", "044", "");
        }
        if (search.equals("first bank")) {
            return new BankInstitution("First Bank Of Nigeria", "first-bank-of-nigeria", "011", "");
        }
        if (search.equals("moniepoint")) {
            return new BankInstitution("Moniepoint", "moniepoint", "090405", "");
        }
        if (search.equals("opay")) {
            return new BankInstitution("OPay", "opay", "999992", "");
        }
        return new BankInstitution("UNITED BANK FOR AFRICA", "united-bank-for-africa", "033", "");
    }

    private char sectionFor(String name) {
        if (name == null || name.trim().isEmpty()) {
            return '#';
        }
        char first = Character.toUpperCase(name.trim().charAt(0));
        return Character.isLetter(first) ? first : '#';
    }

    private void prepareLogo(android.widget.ImageView image, BankInstitution bank, boolean showFrame) {
        image.setTag(null);
        int fallback = fallbackLogo(bank);
        image.setBackgroundResource(showFrame || fallback == R.drawable.ic_bank_building
                ? R.drawable.bg_recipient_circle
                : android.R.color.transparent);
        image.setImageResource(fallback);
        if (fallback == R.drawable.ic_bank_building) {
            ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(
                    color(android.R.color.white)));
        } else {
            ImageViewCompat.setImageTintList(image, null);
        }
        logoLoader.load(bank.getLogoUrl(), image);
    }

    @DrawableRes
    private int fallbackLogo(BankInstitution bank) {
        String value = bank.getName().toLowerCase(Locale.US);
        if (value.contains("palmpay")) {
            return R.drawable.bank_logo_palmpay;
        }
        if (value.contains("78 finance")) {
            return R.drawable.bank_logo_78;
        }
        if (value.equals("9 psb")) {
            return R.drawable.bank_logo_9psb;
        }
        if (value.contains("9japay")) {
            return R.drawable.bank_logo_9japay;
        }
        if (value.contains("aaa finance")) {
            return R.drawable.bank_logo_aaa;
        }
        if (value.contains("access bank")) {
            return R.drawable.bank_logo_access;
        }
        if (value.contains("first bank")) {
            return R.drawable.bank_logo_firstbank;
        }
        if (value.contains("united bank for africa")) {
            return R.drawable.bank_logo_uba;
        }
        if (value.contains("opay")) {
            return R.drawable.bank_logo_opay;
        }
        if (value.contains("moniepoint")) {
            return R.drawable.bank_logo_monie;
        }
        if (value.contains("smartcash") || value.contains("9payment")) {
            return R.drawable.recipient_smartcash;
        }
        return R.drawable.ic_bank_building;
    }

    private void close() {
        if (context instanceof BankPickerActivity) {
            ((BankPickerActivity) context).finishFromBankPicker();
        }
    }

    @ColorInt
    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
