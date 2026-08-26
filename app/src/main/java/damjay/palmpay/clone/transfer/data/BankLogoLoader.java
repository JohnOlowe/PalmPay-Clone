package damjay.palmpay.clone.transfer.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.core.widget.ImageViewCompat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small asynchronous bitmap loader with an in-memory cache for bank logos. */
public final class BankLogoLoader {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.util.LruCache<String, Bitmap> cache =
            new android.util.LruCache<String, Bitmap>(4 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return Math.max(1, bitmap.getByteCount() / 1024);
                }
            };

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

        executor.execute(() -> {
            Bitmap bitmap = download(url);
            if (bitmap == null) {
                return;
            }
            cache.put(url, bitmap);
            mainHandler.post(() -> display(imageView, url, bitmap));
        });
    }

    public void close() {
        executor.shutdownNow();
        cache.evictAll();
    }

    private void display(ImageView imageView, String url, Bitmap bitmap) {
        if (url.equals(imageView.getTag())) {
            ImageViewCompat.setImageTintList(imageView, null);
            imageView.setImageBitmap(bitmap);
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
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
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
