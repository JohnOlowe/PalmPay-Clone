// Compile-only stand-in; the real androidx.biometric is on CI's classpath.
package androidx.biometric;

public class BiometricManager {
    public static final class Authenticators {
        public static final int BIOMETRIC_STRONG = 1 << 1;
        public static final int BIOMETRIC_WEAK = 1 << 2;
        public static final int DEVICE_CREDENTIAL = 1 << 3;
    }
}
