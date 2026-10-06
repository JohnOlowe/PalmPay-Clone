// Compile-only stand-in; the real androidx.biometric is on CI's classpath.
package androidx.biometric;

import android.content.Context;

public class BiometricManager {
    public static final int BIOMETRIC_SUCCESS = 0;
    public static final int BIOMETRIC_ERROR_NO_HARDWARE = 1;
    public static final int BIOMETRIC_ERROR_NONE_ENROLLED = 3;

    public static BiometricManager from(Context context) {
        return new BiometricManager();
    }

    public int canAuthenticate(int authenticators) {
        return BIOMETRIC_SUCCESS;
    }

    public static final class Authenticators {
        public static final int BIOMETRIC_STRONG = 1 << 1;
        public static final int BIOMETRIC_WEAK = 1 << 2;
        public static final int DEVICE_CREDENTIAL = 1 << 3;
    }
}
