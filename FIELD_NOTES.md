# FIELD_NOTES.md — observations from a consuming session

Written by the model session that built `JohnOlowe/PalmPay-Clone` (an AGP,
Java+XML, AndroidX app) and then used this branch to pre-check that app
before GitHub CI. Everything below was measured in the Arena sandbox unless
marked otherwise. The intent: hand the next model everything that cost time
to find out, so it can skip the detours.

## 1. Sandbox facts (the environment this toolchain must survive)

- Network allowlist observed from the shell: `github.com` (git smart-HTTP,
  `codeload.github.com`, `api.github.com`), `pypi.org` +
  `files.pythonhosted.org`. That is ALL.
- Confirmed unreachable (000/EOF): `repo.maven.apache.org`,
  `dl.google.com` (incl. `/dl/android/maven2`), `maven.google.com`,
  `services.gradle.org`, `api.adoptium.net`, `raw.githubusercontent.com`,
  `objects.githubusercontent.com` (GitHub *release assets*),
  `blob.core.windows.net`, and — important for CI debugging —
  `results-receiver.actions.githubusercontent.com` (see §5).
- Consequences: no Gradle wrapper download, no Adoptium/Temurin tarballs, no
  Maven at all. `pip install --user --break-system-packages` works (javalang,
  pillow, reportlab, fonttools all installed fine; `jdk4py` installs but ships
  a JRE only — `bin/java`, NO `javac`, so it cannot compile anything).
- Open fonts ARE obtainable: `git clone --depth 1 --filter=blob:none --sparse
  https://github.com/google/fonts.git` + `git sparse-checkout set ofl/<font>`
  works (blobs come over github.com). Variable TTFs can be instanced to static
  weights with fonttools (`fontTools.varLib.instancer.instantiateVariableFont`,
  NOT `fontTools.instancer` — that module path does not exist).
- The sandbox resets between turns: local git refs snap back to an old base
  (working tree survives), and pip installs vanish. Recovery pattern that
  always worked: copy edited files to /tmp → `git fetch` → `git reset --hard
  origin/<branch>` → copy files back → commit → push. Re-install pip packages
  at the start of a turn instead of trusting the previous one.
- `git push` of `.github/workflows/**` is rejected ("refusing to allow a
  GitHub App to create or update workflow ... without `workflows`
  permission"), and `gh workflow run` returns 403. User-supplied workflows
  cannot be modified or dispatched by the session token.

## 2. What worked, with numbers

- `bash toolchain/setup.sh` ≈ 11 s here: JRE (104 MB), ECJ, D8, aapt2,
  apksigner, apktool, android.jar **API 34**, junit/hamcrest, kotlin-stdlib,
  core-lambda-stubs.
- `bash toolchain/androidx.sh` ≈ 31 s: blobless clone of the
  `AuntiSaha/weather_app` Gradle-cache dump, 42 AARs, `androidx.jar` with 4189
  classes + compiled res zips for aapt2 `--extra-packages`-style linking.
- `check.sh` / `test.sh` / `build.sh --verify` all passed on both samples,
  including the apktool round-trip and v2-signature checks. The README quick
  start is accurate.
- ECJ at `--source 17` works for a real AGP codebase (lambdas, method refs,
  strings-in-switch). Above source 8 it puts the *filtered* android.jar on the
  classpath, so `android.*` is checked against API 34 while `java.*` comes
  from the vendored JRE. Practical meaning: it WILL catch wrong Android
  constants/symbols ≤ API 34, but it will NOT stop you from calling a
  desktop-only `java.*` API that Android lacks, and API 35/36 additions are
  invisible to it. CI (compileSdk 36) remains the authority for those.
- A full pre-check of a 45-source, 30-layout AndroidX app (aapt2 link + ECJ)
  runs in ~9 s. Fast enough to run after every edit; that is the whole point.

## 3. Gaps found, and the workarounds that held

1. **The AndroidX harvest does not contain everything an AGP app may need.**
   Missing for PalmPay: `androidx.biometric`, `com.squareup.okhttp3`,
   `com.squareup.okio`, and `org.json:json` (needed to *run* JVM unit tests
   that touch `JSONObject`; android.jar's org.json is stubs that throw).
   `viewpager2`/`recyclerview` ARE in the dump. Workaround that kept ECJ
   honest: compile-only Java stubs for the missing packages, in the same
   package names, method shapes mirroring the real library (see §4 for the
   okhttp/biometric surface list). CI still compiles against the real
   artifacts; stubs exist only for the local gate.
2. **AGP injects `namespace`; bare aapt2 demands `package=` on `<manifest>`.**
   Any AGP project fed to `check.sh` fails aapt2 link with
   "`<manifest>` tag is missing 'package' attribute". Fix in the wrapper:
   `sed 's/<manifest /<manifest package="<applicationId>" /'` into a build dir
   and link the copy. (Suggest doing this inside `aapt2_link` when the
   manifest has no package — see §6.)
3. **Bare aapt2 is STRICTER than AGP's pipeline in at least one case.**
   `values/colors.xml` contained a line `#    <color ...>` (a `#`-prefixed
   comment attempt). AGP/aapt2-in-CI compiled it green for many runs; this
   toolchain's aapt2 hard-fails with "plain text not allowed here". So a
   PRECHECK FAILED does not always mean CI would fail — fix it anyway, but
   don't panic, and don't "fix" CI-config assumptions from it.
4. **Robolectric tests cannot run locally** (they download the android-all
   jar from Maven at test time). Pre-check is a compile gate; JVM-pure tests
   (javax.crypto, java.util.Base64, pure logic) CAN run on the vendored JRE
   with the vendored junit.jar — if you can source a real `org.json` jar
   somewhere (§6).
5. **ViewBinding classes are AGP-generated; ECJ has no AGP.** The pre-check
   must synthesize them (rules in §4). This is the single biggest piece of
   machinery an AGP consumer needs, and the rules have teeth: get the merge
   layout arity wrong and you teach the local gate a lie that CI will punish.

## 4. Recipe: pre-checking an AGP/AndroidX app with this toolchain

Steps that produced a green-then-CI-green loop for PalmPay:

1. `setup.sh`, `androidx.sh` once.
2. `aapt2 compile --dir <app>/src/main/res` + `aapt2 link` with the
   `androidx/res/*.zip` `-R` inputs, `--no-version-vectors`, min-sdk from the
   manifest, and the package-patched manifest copy → `R.java`.
3. Generate ViewBinding stand-ins into one dir, copy `R.java` in, copy stubs
   in, then `ecj_compile <classes> <that dir>` with `JAVA_SRC_LEVEL=17`.
   (PalmPay commits this as `tools/precheck.sh` + `tools/gen_bindings.py` +
   `tools/stubs/` — a working reference implementation of everything here.)

Binding-generation rules that mattered (each one broke CI or the local gate
when wrong):

- Class name: `activity_amount.xml` → `ActivityAmountBinding`; fields are the
  camelCase of `@+id/...` — keep the RAW id string for `findViewById`
  (camel-casing is lossy: `key_00` → `key00` does not invert).
- `<include android:id="@+id/x" layout="@layout/y">` → field type is
  `YBinding`, bound with `YBinding.bind(root.findViewById(...))`, not a View
  cast.
- Merge-rooted layouts generate ONLY `inflate(LayoutInflater, ViewGroup)`
  (two args, always attaches). AGP does not emit the three-arg overload for
  merge roots; generating one teaches your code a method CI will reject with
  "actual and formal argument lists differ in length" — that exact error cost
  a CI run.
- The root's OWN `@+id` becomes a field typed as the root element, and
  `getRoot()` must return the ROOT ELEMENT type, not `View`: real code calls
  root-type methods on it (e.g. `MaterialCardView.setCardBackgroundColor`).
- Tag→type map needs at least: TextView, EditText, ImageView, ImageButton,
  View, LinearLayout, FrameLayout, GridLayout, RadioGroup, RadioButton,
  ScrollView, Space, ViewFlipper, ListView,
  androidx.core.widget.NestedScrollView,
  androidx.constraintlayout.widget.ConstraintLayout,
  com.google.android.material.card.MaterialCardView,
  com.google.android.material.button.MaterialButton; any tag containing a dot
  is its own FQ class.

Compile-only stub surface that was sufficient for PalmPay (mirror these if
you need them): `okhttp3.{OkHttpClient(+Builder), Dispatcher, Request(+Builder),
HttpUrl(+Builder), RequestBody(create(String,MediaType)), FormBody(+Builder),
MediaType(parse/get), Call, Callback, Response(isSuccessful/body/AutoCloseable),
ResponseBody(string)}` and `androidx.biometric.{BiometricPrompt
(+AuthenticationCallback/AuthenticationResult/PromptInfo.Builder),
BiometricManager.Authenticators}`. One public class per file; ECJ is happy.

Negative-test the gate once (plant a known-bad symbol, expect PRECHECK
FAILED, revert). A gate that has never failed is a gate you don't trust.

## 5. CI blindness toolkit (when you still need the log)

- `gh run view --log` / `--log-failed` die with EOF: the redirect target
  (`results-receiver.actions.githubusercontent.com`) is outside the allowlist.
  The job HTML page says "Sign in to view logs" even for public repos.
- `gh api repos/.../check-runs/<job>/annotations` WORKS but usually only
  yields the generic "Process completed with exit code 1" — Gradle/javac
  errors do not surface as annotations.
- Step timings from `gh api .../jobs` are a poor man's classifier: ~60-70 s
  into "Run unit tests" ≈ compile error; near the usual full duration ≈ a
  test actually ran and failed.
- The reliable channel, twice now: ask the user to paste the first
  `error:`/`FAILED` lines from the web UI. Ask early; guessing burns whole
  CI cycles (~2 min each).
- Best of all: make the local gate good enough that the paste is a
  once-per-session event (§4).

## 6. Suggestions for GhostHand itself

- `check.sh`: accept extra source roots (e.g. `--extra-sources DIR`) so
  generated bindings/stubs join the ECJ run without a wrapper; currently only
  the aapt2 `gen` dir is merged in.
- `aapt2_link`: auto-patch a package-less manifest (AGP namespace case) into
  the build dir instead of dying.
- `select_androidx.py`: add `biometric` to the ecosystem list or add a
  generic "extra artifacts" file; consider harvesting a `json-*.jar`
  (org.json) so JVM unit tests of JSON-heavy code can run offline.
- Document the §3.3 asymmetry (pre-check can be stricter than CI) so the next
  consumer doesn't chase a CI ghost after a local aapt2 complaint.
- A `precheck-agp.sh` reference wrapper (manifest patch + binding gen hook)
  would turn §4 from a session artifact into a shipped feature; PalmPay's
  `tools/precheck.sh` is a tested starting point.

## 7. Small recipes worth keeping

- Fonts without Maven: sparse-clone google/fonts (§1), instance variable TTFs
  with fonttools, drop the static TTFs into `res/font/`.
- Pixel forensics on user screenshots: `pip install pillow`, crop+LANCZOS,
  compare candidate fonts by rendering them at the same size side by side.
- `gh api .../actions/runs/<id>/jobs --jq '.jobs[0].steps[]'` for the timing
  classifier (§5).
- When the user's device screenshots and CI disagree about what shipped,
  check whether their APK predates the fix commit before re-debugging — line
  numbers in a pasted error are a build fingerprint.

— end of notes; adjust or fold into RECIPE.md as you see fit.
