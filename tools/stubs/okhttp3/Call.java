package okhttp3;

public interface Call {
    void enqueue(Callback callback);
}
