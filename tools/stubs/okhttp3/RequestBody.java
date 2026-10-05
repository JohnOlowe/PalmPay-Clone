package okhttp3;

public class RequestBody {
    public static RequestBody create(String content, MediaType contentType) {
        return new RequestBody();
    }
}
