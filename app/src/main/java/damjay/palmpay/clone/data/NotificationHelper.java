package damjay.palmpay.clone.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import damjay.palmpay.clone.R;

/**
 * Real heads-up notifications (the ones that slide in at the top). On
 * Android 13+ they only show once the POST_NOTIFICATIONS permission the
 * Profile page can request has been granted; until then posting is a no-op.
 */
public final class NotificationHelper {
    private static final String CHANNEL_ID = "palmpay_clone_alerts";
    private static final String CHANNEL_NAME = "Transfer alerts";
    private static int nextId = 1;

    private NotificationHelper() {
        // No instances.
    }

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void postHeadsUp(Context context, String title, String text) {
        if (!canPost(context)) {
            return;
        }
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Receipts and transfer alerts");
            manager.createNotificationChannel(channel);
        }
        android.app.Notification notification = new NotificationCompat.Builder(
                context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .build();
        manager.notify(nextId++, notification);
    }
}
