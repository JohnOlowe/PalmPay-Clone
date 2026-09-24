package damjay.palmpay.clone.transfer.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.databinding.ActivityAddMoneyBinding;
import damjay.palmpay.clone.transfer.data.PaystackClient;

/**
 * Behaviour for the Add Money page: charge a VISA card via Paystack and
 * forward the funds to the PalmPay wallet.
 *
 * Safety gate: the destination is always PalmPay (hardcoded bank code, no
 * resolution) and the flow can only ever succeed for the owner's registered
 * number. Any other destination short-circuits BEFORE any Paystack call is
 * made, after a realistic processing delay, with a generic failure message
 * that reveals nothing about the gate.
 */
public final class AddMoneyController {
    private static final String ALLOWED_SUFFIX = "4043";
    private static final String PALMPAY_BANK_CODE = "999991";
    private static final long GATE_DELAY_MS = 1400;
    private static final int POLL_ATTEMPTS = 24;
    private static final long POLL_INTERVAL_MS = 5000;

    private final Context context;
    private final ActivityAddMoneyBinding binding;
    private final PaystackClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long currentKobo;
    private String currentDestination = "";
    private String currentReference = "";
    private String currentAccessCode = "";
    private boolean busy;

    public AddMoneyController(
            Context context, ActivityAddMoneyBinding binding,
            PaystackClient client) {
        this.context = context;
        this.binding = binding;
        this.client = client;
    }

    public void bind() {
        binding.amBackButton.setOnClickListener(view ->
                ((AddMoneyActivity) context).finishFromAddMoney());
        binding.amPayButton.setOnClickListener(view -> onPay());
        binding.amOtpSubmit.setOnClickListener(view -> onOtpSubmit());

        binding.amCardInput.addTextChangedListener(simpleWatcher(text -> {
            String digits = digitsOnly(text);
            StringBuilder grouped = new StringBuilder();
            for (int i = 0; i < digits.length() && i < 19; i++) {
                if (i > 0 && i % 4 == 0) {
                    grouped.append(' ');
                }
                grouped.append(digits.charAt(i));
            }
            return grouped.toString();
        }));
        binding.amExpiryInput.addTextChangedListener(simpleWatcher(text -> {
            String digits = digitsOnly(text);
            if (digits.length() > 4) {
                digits = digits.substring(0, 4);
            }
            if (digits.length() <= 2) {
                return digits;
            }
            return digits.substring(0, 2) + "/" + digits.substring(2);
        }));
    }

    private interface Formatter {
        String format(String text);
    }

    private TextWatcher simpleWatcher(final Formatter formatter) {
        return new TextWatcher() {
            private boolean internal;

            @Override
            public void beforeTextChanged(
                    CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(
                    CharSequence text, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (internal) {
                    return;
                }
                String formatted = formatter.format(editable.toString());
                if (!formatted.contentEquals(editable)) {
                    internal = true;
                    editable.replace(0, editable.length(), formatted);
                    internal = false;
                }
            }
        };
    }

    private void onPay() {
        if (busy) {
            return;
        }
        double amount = parseAmount(binding.amAmountInput.getText().toString());
        String cardDigits = digitsOnly(binding.amCardInput.getText().toString());
        String expiry = binding.amExpiryInput.getText().toString();
        String cvv = binding.amCvvInput.getText().toString();
        String destination = digitsOnly(
                binding.amDestinationInput.getText().toString());
        if (amount < 100
                || cardDigits.length() < 16
                || expiry.length() != 5
                || cvv.length() < 3
                || (destination.length() != 10 && destination.length() != 11)) {
            showResult(context.getString(R.string.am_generic_fail), false);
            return;
        }
        busy = true;
        currentKobo = Math.round(amount * 100);
        currentDestination = destination;
        hideResult();
        binding.amOtpRow.setVisibility(View.GONE);
        showStatus(R.string.am_processing);

        if (!destination.endsWith(ALLOWED_SUFFIX)) {
            // Generic failure, no Paystack call, no hint about the gate.
            handler.postDelayed(() -> {
                busy = false;
                hideStatus();
                showResult(context.getString(R.string.am_generic_fail), false);
            }, GATE_DELAY_MS);
            return;
        }

        currentReference = "PPC" + System.currentTimeMillis();
        String[] parts = expiry.split("/");
        client.chargeCard(
                new damjay.palmpay.clone.data.WalletStore(context)
                        .getPaystackEmail(),
                currentKobo,
                cardDigits,
                cvv,
                parts[0],
                parts.length > 1 ? parts[1] : "",
                currentReference,
                this::onCharge);
    }

    private void onCharge(JSONObject body) {
        JSONObject data = body != null && body.optBoolean("status")
                ? body.optJSONObject("data") : null;
        if (data == null) {
            fail(messageOf(body));
            return;
        }
        currentReference = data.optString("reference", currentReference);
        JSONObject authorization = data.optJSONObject("authorization");
        String access = authorization != null
                ? authorization.optString("access_code", "")
                : data.optString("access_code", "");
        String redirect = authorization != null
                ? authorization.optString("redirect_url", "")
                : data.optString("redirect_url", "");
        if (!redirect.isEmpty()) {
            try {
                context.startActivity(new Intent(
                        Intent.ACTION_VIEW, Uri.parse(redirect)));
            } catch (Exception ignored) {
                fail("Could not open the bank authorisation page.");
                return;
            }
            binding.amStatusText.setText(R.string.am_browser);
            binding.amStatusRow.setVisibility(View.VISIBLE);
            pollVerification(0);
            return;
        }
        if (!access.isEmpty()) {
            currentAccessCode = access;
            hideStatus();
            binding.amOtpRow.setVisibility(View.VISIBLE);
            return;
        }
        verify();
    }

    private void onOtpSubmit() {
        String otp = binding.amOtpInput.getText().toString();
        if (otp.length() < 5) {
            Toast.makeText(context, R.string.am_otp_label, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        showStatus(R.string.am_processing);
        client.submitOtp(currentAccessCode, otp, body -> {
            if (body != null && body.optBoolean("status")) {
                verify();
            } else {
                fail(messageOf(body));
            }
        });
    }

    private void pollVerification(final int attempt) {
        if (attempt >= POLL_ATTEMPTS) {
            fail("Verification timed out. Please try again.");
            return;
        }
        handler.postDelayed(() -> client.verifyTransaction(
                currentReference, body -> {
                    JSONObject data = body != null
                            ? body.optJSONObject("data") : null;
                    if (data != null
                            && "success".equals(data.optString("status"))) {
                        transfer();
                    } else if (data != null
                            && "failed".equals(data.optString("status"))) {
                        fail(messageOf(body));
                    } else {
                        pollVerification(attempt + 1);
                    }
                }), POLL_INTERVAL_MS);
    }

    private void verify() {
        showStatus(R.string.am_verifying);
        client.verifyTransaction(currentReference, body -> {
            JSONObject data = body != null ? body.optJSONObject("data") : null;
            if (data != null && "success".equals(data.optString("status"))) {
                transfer();
            } else {
                fail(messageOf(body));
            }
        });
    }

    private void transfer() {
        showStatus(R.string.am_sending);
        client.transferToBank(
                currentKobo,
                PALMPAY_BANK_CODE,
                currentDestination,
                "PPT" + System.currentTimeMillis(),
                body -> {
                    busy = false;
                    hideStatus();
                    if (body != null && body.optBoolean("status")) {
                        terminal(context.getString(
                                R.string.am_success,
                                formatNaira(currentKobo / 100.0),
                                mask(currentDestination)), true);
                    } else {
                        fail(messageOf(body));
                    }
                });
    }

    /** Only the destination gate uses the generic message. */
    private void fail(String message) {
        terminal(message, false);
    }

    /** Every terminal state is shown as a result box and an alert dialog. */
    private void terminal(String message, boolean positive) {
        busy = false;
        hideStatus();
        showResult(message, positive);
        new android.app.AlertDialog.Builder(context)
                .setTitle(R.string.add_money)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String messageOf(JSONObject body) {
        if (body == null) {
            return "Network error. Check your connection and try again.";
        }
        String message = body.optString("message", "");
        return message.isEmpty()
                ? "Transaction failed. Please try again." : message;
    }

    private void showStatus(int textRes) {
        binding.amStatusText.setText(textRes);
        binding.amStatusRow.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        binding.amStatusRow.setVisibility(View.GONE);
    }

    private void hideResult() {
        binding.amResultText.setVisibility(View.GONE);
    }

    private void showResult(String message, boolean positive) {
        binding.amResultText.setText(message);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(12 * context.getResources()
                .getDisplayMetrics().density);
        background.setColor(Color.parseColor(positive ? "#DDF8F1" : "#FBE9E9"));
        binding.amResultText.setBackground(background);
        binding.amResultText.setTextColor(Color.parseColor(
                positive ? "#0F9D6A" : "#D93030"));
        binding.amResultText.setVisibility(View.VISIBLE);
    }

    private String mask(String destination) {
        return "****" + destination.substring(destination.length() - 4);
    }

    private String formatNaira(double value) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.US));
        return formatter.format(value);
    }

    private double parseAmount(String text) {
        try {
            return Double.parseDouble(text.replace(",", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String digitsOnly(String text) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        return digits.toString();
    }
}
