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
    private static final int POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 5000;

    private final Context context;
    private final ActivityAddMoneyBinding binding;
    private final PaystackClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long currentKobo;
    private String currentDestination = "";
    private String currentReference = "";
    private String currentChallenge = "";
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
        if (body == null || !body.optBoolean("status")) {
            fail(messageOf(body));
            return;
        }
        handleStep(body, body.optJSONObject("data"));
    }

    /** Full Paystack charge state machine (pending/otp/pin/phone/3DS/success). */
    private void handleStep(JSONObject body, JSONObject data) {
        if (data == null) {
            fail(messageOf(body));
            return;
        }
        currentReference = data.optString("reference", currentReference);
        String step = data.optString("status", "");
        switch (step) {
            case "success":
                transfer();
                return;
            case "open_url": {
                String url = data.optString("url",
                        data.optString("authorization_url", ""));
                if (url.isEmpty()) {
                    url = body.optString("message", "");
                }
                openBrowser(url);
                pollPending(0);
                return;
            }
            case "send_otp":
                currentChallenge = "otp";
                showChallenge(R.string.am_otp_label);
                return;
            case "send_pin":
                currentChallenge = "pin";
                showChallenge(R.string.am_pin_label);
                return;
            case "send_phone":
                currentChallenge = "phone";
                showChallenge(R.string.am_phone_label);
                return;
            case "pending":
                binding.amOtpRow.setVisibility(View.GONE);
                showStatus(R.string.am_processing);
                handler.postDelayed(() -> pollPending(0), 10_000);
                return;
            case "timeout":
            case "failed":
                fail(data.optString("message", messageOf(body)));
                return;
            default: {
                // Challenged cards may signal paused + authorization_url.
                if (data.optBoolean("paused")) {
                    openBrowser(data.optString("authorization_url", ""));
                    pollPending(0);
                    return;
                }
                pollPending(0);
            }
        }
    }

    /**
     * Opens the bank page in Chrome as its own full-screen task so low-RAM
     * devices never kill it together with this app, then tucks the app to
     * the back so it can be recalled as a pop-up while Chrome stays up.
     */
    private void openBrowser(String url) {
        if (url == null || url.isEmpty() || !url.startsWith("http")) {
            fail("The bank did not provide an authorisation page.");
            return;
        }
        try {
            if (context instanceof AddMoneyActivity) {
                ((AddMoneyActivity) context).enterPopupMode();
            }
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browser);
            binding.amStatusText.setText(R.string.am_browser);
            binding.amStatusRow.setVisibility(View.VISIBLE);
            if (context instanceof AddMoneyActivity) {
                ((AddMoneyActivity) context).moveTaskToBack(true);
            }
        } catch (Exception ignored) {
            fail("Could not open the bank authorisation page.");
        }
    }

    /** Called when the user returns: resume checking the paused charge. */
    public void resumePending() {
        if (busy && currentReference != null && !currentReference.isEmpty()) {
            binding.amStatusText.setText(R.string.am_browser);
            binding.amStatusRow.setVisibility(View.VISIBLE);
            pollPending(0);
        }
    }

    private void showChallenge(int labelRes) {
        hideStatus();
        binding.amOtpLabel.setText(labelRes);
        binding.amOtpInput.setText("");
        binding.amOtpRow.setVisibility(View.VISIBLE);
    }

    private void onOtpSubmit() {
        String value = binding.amOtpInput.getText().toString();
        if (value.length() < 3) {
            return;
        }
        showStatus(R.string.am_processing);
        PaystackClient.BodyCallback next = body -> {
            if (body != null && body.optBoolean("status")) {
                handleStep(body, body.optJSONObject("data"));
            } else {
                fail(messageOf(body));
            }
        };
        if ("pin".equals(currentChallenge)) {
            client.submitPin(currentReference, value, next);
        } else if ("phone".equals(currentChallenge)) {
            client.submitPhone(currentReference, value, next);
        } else {
            client.submitOtp(currentReference, value, next);
        }
    }

    private void pollPending(final int attempt) {
        if (attempt >= POLL_ATTEMPTS) {
            fail("Transaction still pending. Please try again shortly.");
            return;
        }
        handler.postDelayed(() -> client.checkPendingCharge(
                currentReference, body -> {
                    if (body == null) {
                        pollPending(attempt + 1);
                        return;
                    }
                    JSONObject data = body.optJSONObject("data");
                    String step = data != null
                            ? data.optString("status", "") : "";
                    if ("success".equals(step)) {
                        transfer();
                    } else if ("failed".equals(step) || "timeout".equals(step)) {
                        fail(data.optString("message", messageOf(body)));
                    } else if ("open_url".equals(step) && data != null) {
                        String url = data.optString("url",
                                data.optString("authorization_url", ""));
                        if (!url.isEmpty()) {
                            openBrowser(url);
                        }
                        pollPending(attempt + 1);
                    } else {
                        pollPending(attempt + 1);
                    }
                }), POLL_INTERVAL_MS);
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
        if (message.isEmpty()) {
            return "Transaction failed. Please try again.";
        }
        if (message.contains("could not be processed")
                || message.contains("contact merchant")) {
            message += context.getString(R.string.am_card_declined_hint);
        }
        return message;
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
