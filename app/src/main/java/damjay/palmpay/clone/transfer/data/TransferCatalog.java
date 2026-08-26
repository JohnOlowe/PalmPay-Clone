package damjay.palmpay.clone.transfer.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.transfer.model.TransferRecipient;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/**
 * Local transfer data used while the bank directory/API is being integrated.
 * The controller consumes models, so replacing this catalogue with a remote
 * repository will not require rewriting the XML renderer.
 */
public final class TransferCatalog {
    private TransferCatalog() {
        // No instances.
    }

    public static List<TransferShortcut> shortcuts() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransferShortcut(R.string.schedule_transfer, R.drawable.ic_schedule_transfer),
                new TransferShortcut(R.string.success_rate, R.drawable.ic_success_rate),
                new TransferShortcut(R.string.transfer_settings, R.drawable.ic_transfer_settings),
                new TransferShortcut(R.string.withdraw_cash, R.drawable.ic_withdraw_cash)
        ));
    }

    public static List<TransferRecipient> recentRecipients() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransferRecipient(
                        "JOHN OLUWADAMILARE OLOWE", "9112413798", "OPay", "Aug 25, 2026"),
                new TransferRecipient(
                        "JOHN OLOWE", "9112413798", "SmartCash PSB", "Aug 25, 2026"),
                new TransferRecipient(
                        "ROSELINE YETUNDE AWOLEKE", "7082608683", "OPay", "Aug 24, 2026"),
                new TransferRecipient(
                        "OLANIX SUPER MARKET - VARIETIES - ...", "9072698792", "Moniepoint", "Aug 24, 2026"),
                new TransferRecipient(
                        "POS Transfer- OLORUNNIYI OLAJUMOKE", "5004768632", "Moniepoint", "Aug 23, 2026"),
                new TransferRecipient(
                        "LAGBAJA CREATIONS VENTURES", "5460471307", "Moniepoint", "Aug 20, 2026"),
                new TransferRecipient(
                        "YALARAK INTEGRATED SERVICES", "5980510647", "Moniepoint", "Aug 20, 2026"),
                new TransferRecipient(
                        "HELEN PRAYER ATENIOLA", "8080720404", "OPay", "Aug 19, 2026"),
                new TransferRecipient(
                        "Veronica Chinyere Ayoola", "8145162196", "OPay", "Aug 18, 2026"),
                new TransferRecipient(
                        "TOMIWA EMMANUEL BAMIKALE", "7010906507", "OPay", "Aug 15, 2026"),
                new TransferRecipient(
                        "AISHAT OPEYEMI LATEEF", "8128891481", "Moniepoint", "Aug 14, 2026")
        ));
    }
}
