
        this.logoLoader = new BankLogoLoader(context);package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.DialogBankPickerBinding;
import damjay.palmpay.clone.transfer.data.BankDirectoryRepository;
import damjay.palmpay.clone.transfer.data.BankLogoLoader;
import damjay.palmpay.clone.transfer.model.BankInstitution;

/** Searchable bottom sheet for the complete online Nigerian bank directory. */
public final class BankPickerDialog extends BottomSheetDialog {
    private final OnBankSelectedListener listener;
    private final BankDirectoryRepository repository = new BankDirectoryRepository();
    private final BankLogoLoader logoLoader;
    private DialogBankPickerBinding binding;
    private BankListAdapter adapter;

    public interface OnBankSelectedListener {
        void onBankSelected(BankInstitution bank);
    }

    private BankPickerDialog(@NonNull Context context, OnBankSelectedListener listener) {
        super(context);
        this.listener = listener;
        this.logoLoader = new BankLogoLoader(context);
    }

    public static void show(Context context, OnBankSelectedListener listener) {
        BankPickerDialog dialog = new BankPickerDialog(context, listener);
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogBankPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureSheet();

        adapter = new BankListAdapter(getContext(), logoLoader);
        binding.bankList.setAdapter(adapter);
        binding.bankList.setOnItemClickListener((parent, view, position, id) -> {
            listener.onBankSelected(adapter.getBank(position));
            dismiss();
        });
        binding.bankSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                adapter.filter(text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No-op.
            }
        });

        repository.load((banks, fromNetwork) -> {
            if (binding == null) {
                return;
            }
            adapter.replaceBanks(banks);
            binding.bankLoading.setVisibility(View.GONE);
        });
    }

    private void configureSheet() {
        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        getBehavior().setSkipCollapsed(true);
    }

    @Override
    protected void onStop() {
        repository.close();
        logoLoader.close();
        super.onStop();
    }
}
