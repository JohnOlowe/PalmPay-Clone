package damjay.palmpay.clone.transfer.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import damjay.palmpay.clone.R;
import damjay.palmpay.clone.transfer.model.PalmPayContact;
import damjay.palmpay.clone.transfer.model.TransferShortcut;

/**
 * Demo data for the To PalmPay screen, mirroring the supplied reference.
 * Recent is the full history (latest transfers first, then every contact),
 * while the PalmPay Contacts tab renders the same people without dates,
 * exactly like the official app. Consumed as models so a real contacts API
 * can replace it later without touching the renderer.
 */
public final class PalmPayCatalog {
    private PalmPayCatalog() {
        // No instances.
    }

    public static List<TransferShortcut> shortcuts() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransferShortcut(R.string.schedule_transfer,
                        R.drawable.ic_schedule_transfer),
                new TransferShortcut(R.string.transfer_settings,
                        R.drawable.ic_transfer_settings),
                new TransferShortcut(R.string.withdraw_cash,
                        R.drawable.ic_withdraw_cash)));
    }

    public static List<PalmPayContact> recent() {
        List<PalmPayContact> all = new ArrayList<>();
        all.addAll(recentOnly());
        all.addAll(contacts());
        return Collections.unmodifiableList(all);
    }

    private static List<PalmPayContact> recentOnly() {
        return Arrays.asList(
                new PalmPayContact("OGHENENYERHOVWO  URHOBARA",
                        "8080868957", R.drawable.avatar_purple_woman, false,
                        "Aug 26, 2026"),
                new PalmPayContact("ABIMBOLA BLESSING ADEPOJU",
                        "8030780591", R.drawable.avatar_gen_3, false,
                        "Aug 21, 2026"),
                new PalmPayContact("MUHAMMAD LAWAN BUBA",
                        "8138716828", R.drawable.avatar_gen_4, false,
                        "Aug 19, 2026"),
                new PalmPayContact("FRANKI IN CHINEDU ODILU",
                        "8033145720", R.drawable.avatar_gen_2, false,
                        "Aug 17, 2026"));
    }

    public static List<PalmPayContact> contacts() {
        return Collections.unmodifiableList(Arrays.asList(
                new PalmPayContact("ADEOLA SAMSON OLOWE",
                        "8023875574", R.drawable.avatar_reference, false,
                        "Aug 16, 2026"),
                new PalmPayContact("BABAJIDE JAMES OLUOKUN",
                        "8067237160", R.drawable.avatar_gen_4, false,
                        "Aug 15, 2026"),
                new PalmPayContact("FUNMILAYO - MAKINDE",
                        "8066264578", R.drawable.avatar_purple_woman, false,
                        "Aug 14, 2026"),
                new PalmPayContact("HELEN PRAYER ATENIOLA",
                        "8080720404", R.drawable.avatar_gen_1, false,
                        "Aug 13, 2026"),
                new PalmPayContact("UKACHI MARVELLOUS AMAIKE",
                        "8022892878", R.drawable.avatar_gen_2, false,
                        "Aug 12, 2026"),
                new PalmPayContact("PRECIOUS OLUWADARASIMI AKINDELE",
                        "9133988400", R.drawable.avatar_gen_3, false,
                        "Aug 11, 2026"),
                new PalmPayContact("ABIOLA GRACE AKANO",
                        "8066383696", R.drawable.avatar_purple_woman, false,
                        "Aug 10, 2026"),
                new PalmPayContact("AMOS OGHENAKUGHE OGIAGA",
                        "9550470865", R.drawable.avatar_agent, true,
                        "Aug 9, 2026"),
                new PalmPayContact("BOSEDE ABIGEAL MABAWONKU",
                        "7089565446", R.drawable.avatar_gen_1, false,
                        "Aug 8, 2026")));
    }

    public static List<PalmPayContact> favorites() {
        return Collections.unmodifiableList(contacts().subList(0, 3));
    }
}
