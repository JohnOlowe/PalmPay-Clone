package okhttp3;

public class Request {
    public static class Builder {
        public Builder url(String url) {
            return this;
        }

        public Builder url(HttpUrl url) {
            return this;
        }

        public Builder header(String name, String value) {
            return this;
        }

        public Builder post(RequestBody body) {
            return this;
        }

        public Builder get() {
            return this;
        }

        public Request build() {
            return new Request();
        }
    }
}
