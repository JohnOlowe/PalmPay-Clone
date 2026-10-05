package okhttp3;

public class HttpUrl {
    public static HttpUrl get(String url) {
        return new HttpUrl();
    }

    public Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        public Builder addQueryParameter(String name, String value) {
            return this;
        }

        public HttpUrl build() {
            return new HttpUrl();
        }
    }
}
