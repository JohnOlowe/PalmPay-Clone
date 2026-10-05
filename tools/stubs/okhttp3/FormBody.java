package okhttp3;

public class FormBody extends RequestBody {
    public static class Builder {
        public Builder add(String name, String value) {
            return this;
        }

        public FormBody build() {
            return new FormBody();
        }
    }
}
