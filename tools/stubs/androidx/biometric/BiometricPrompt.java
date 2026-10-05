// Compile-only stand-in; the real androidx.biometric is on CI's classpath.
package androidx.biometric;

import androidx.fragment.app.FragmentActivity;

public class BiometricPrompt {
    public BiometricPrompt(FragmentActivity activity,
                           AuthenticationCallback callback) {
    }

    public void authenticate(PromptInfo info) {
    }

    public static class AuthenticationResult {
    }

    public static class AuthenticationCallback {
        public void onAuthenticationSucceeded(AuthenticationResult result) {
        }

        public void onAuthenticationError(int errorCode, CharSequence err) {
        }

        public void onAuthenticationFailed() {
        }
    }

    public static class PromptInfo {
        public static class Builder {
            public Builder setTitle(CharSequence title) {
                return this;
            }

            public Builder setSubtitle(CharSequence subtitle) {
                return this;
            }

            public Builder setAllowedAuthenticators(int authenticators) {
                return this;
            }

            public Builder setDeviceCredentialAllowed(boolean allowed) {
                return this;
            }

            public PromptInfo build() {
                return new PromptInfo();
            }
        }
    }
}
