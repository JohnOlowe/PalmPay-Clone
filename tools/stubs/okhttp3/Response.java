package okhttp3;

public class Response implements AutoCloseable {
    public boolean isSuccessful() {
        return true;
    }

    public ResponseBody body() {
        return null;
    }

    @Override
    public void close() {
    }
}
