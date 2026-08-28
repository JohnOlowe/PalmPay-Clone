package damjay.palmpay.clone.transfer.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous bank-logo loader with an in-memory and an on-device disk
 * cache. Every downloaded logo is normalised exactly once — centre-cropped
 * to a square, scaled to 96px and clipped to a circle — and the processed
 * bitmap is persisted, so repeat displays never re-download or re-process.
 */
public final class BankLogoLoader {
    private static final int LOGO_SIZE = 96;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache =
            new LruCache<String, Bitmap>(4 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return Math.max(1, bitmap.getByteCount() / 1024);
                }
            };
    private final Context context;

    public BankLogoLoader() {
        this(null);
    }

    public BankLogoLoader(Context context) {
        this.context = context;
    }

    public void load(String url, ImageView imageView) {
        if (url == null || url.isEmpty()) {
            return;
        }
        imageView.setTag(url);
        Bitmap cached = cache.get(url);
        if (cached != null) {
            display(imageView, url, cached);
            return;
        }
        final Bitmap fromDisk = readDisk(url);
        if (fromDisk != null) {
            cache.put(url, fromDisk);
            display(imageView, url, fromDisk);
            return;
        }
        executor.execute(() -> {
            Bitmap bitmap = download(url);
            if (bitmap == null) {
                return;
            }
            final Bitmap processed = roundLogo(bitmap);
            cache.put(url, processed);
            writeDisk(url, processed);
            mainHandler.post(() -> display(imageView, url, processed));
        });
    }

    public void close() {
        executor.shutdownNow();
        cache.evictAll();
    }

    private void display(ImageView imageView, String url, Bitmap bitmap) {
        if (url.equals(imageView.getTag())) {
            androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);
            imageView.setImageBitmap(bitmap);
        }
    }

    /** Centre-crops to a square, scales and clips to a transparent circle. */
    static Bitmap roundLogo(Bitmap source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        Bitmap square = source;
        if (source.getWidth() != side || source.getHeight() != side) {
            square = Bitmap.createBitmap(source,
                    (source.getWidth() - side) / 2,
                    (source.getHeight() - side) / 2,
                    side, side);
        }
        Bitmap out = Bitmap.createBitmap(
                LOGO_SIZE, LOGO_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Path circle = new Path();
        circle.addCircle(LOGO_SIZE / 2f, LOGO_SIZE / 2f, LOGO_SIZE / 2f,
                Path.Direction.CCW);
        canvas.clipPath(circle);
        canvas.drawBitmap(square, null,
                new Rect(0, 0, LOGO_SIZE, LOGO_SIZE), null);
        return out;
    }

    private File diskFile(String url) {
        if (context == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(url.getBytes("UTF-8"));
            StringBuilder name = new StringBuilder();
            for (byte b : hash) {
                name.append(Character.forDigit((b >> 4) & 0xF, 16));
                name.append(Character.forDigit(b & 0xF, 16));
            }
            File dir = new File(context.getApplicationContext()
                    .getCacheDir(), "bank_logos");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            return new File(dir, name + ".png");
        } catch (Exception exception) {
            return null;
        }
    }

    private Bitmap readDisk(String url) {
        File file = diskFile(url);
        if (file == null || !file.exists()) {
            return null;
        }
        try (InputStream stream = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(stream);
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeDisk(String url, Bitmap bitmap) {
        File file = diskFile(url);
        if (file == null) {
            return;
        }
        try (FileOutputStream stream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (Exception ignored) {
            // A failed cache write simply re-processes on the next load.
        }
    }

    private Bitmap download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(10_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "image/*");
            if (connection.getResponseCode() < 200
                    || connection.getResponseCode() >= 300) {
                return null;
            }
            try (InputStream stream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(stream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
