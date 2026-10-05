# FIELD_NOTES.md — generic field notes for consuming the GhostHand toolchain

App-agnostic observations, recipes and workarounds gathered while using the
GhostHand branch (self-contained Android toolchain: vendored JRE, ECJ, D8/R8,
aapt2, apksigner, android.jar, JUnit, no Gradle/Maven/SDK required). Written
for whatever app the next session is building. Adjust or fold into RECIPE.md.

## 1. The sandbox you are in

- Reachable from the shell: `github.com` (git smart-HTTP, `codeload`,
  `api.github.com`) and PyPI (`pypi.org`, `files.pythonhosted.org`). Nothing
  else. Confirmed dead: `repo.maven.apache.org`, `dl.google.com` (including
  the Android Maven repo), `maven.google.com`, `services.gradle.org`,
  `api.adoptium.net`, `raw.githubusercontent.com`,
  `objects.githubusercontent.com` (GitHub *release assets*), and
  `results-receiver.actions.githubusercontent.com` (CI logs — see §7).
- Consequences: no Gradle wrapper downloads, no JDK tarballs from Adoptium,
  no Maven at all. `pip install --user --break-system-packages` works
  (javalang, pillow, reportlab, fonttools all fine). `pip install jdk4py`
  gives you a **JRE only — there is no javac in it**; it can run things, not
  compile them. The vendored GhostHand JRE + ECJ is the real compile path.
- The sandbox resets between turns: git refs snap back to an old base while
  the working tree survives, and pip packages vanish. Recovery pattern: copy
  edited files to /tmp → `git fetch` → `git reset --hard origin/<branch>` →
  copy back → commit → push. Re-install pip packages instead of trusting the
  previous turn.
- Token permissions vary by session/repo (some sessions cannot push to a
  sibling repo, cannot edit `.github/workflows`, cannot `gh workflow run`).
  Probe once early (`git push` a trivial commit, `gh api` a write endpoint)
  instead of assuming.

## 2. Quick start and measured timings

```bash
bash toolchain/setup.sh [--api N]     # ~11 s; default API 34
bash toolchain/androidx.sh            # ~31 s; harvests AndroidX, no Maven
bash toolchain/check.sh  PROJ [--source 17] [--min-api 26] [--no-dex]
bash toolchain/test.sh   PROJ         # JUnit 4 on the vendored JRE
bash toolchain/build.sh  PROJ --verify
```

- `check.sh` on a several-dozen-source AndroidX app (aapt2 link + ECJ) is
  ~9 s. Run it after every edit; that is the whole point of the toolchain.
- `setup.sh --api N` swaps the platform: `android.jar` comes from
  `Sable/android-platforms` on GitHub (`android-<N>/android.jar`, blobless
  checkout), and `filter_android_jar.py` regenerates
  `android-classpath.jar` from it. So **different android.jar versions are
  one flag away**; pick the API your app compiles against so new-API symbols
  are actually checked. Note ECJ only ever sees one level — symbols added
  above it are invisible, and with `--source` > 8 `java.*` comes from the
  JRE, so desktop-only Java APIs are NOT flagged. CI with a real SDK remains
  the authority for both.

## 3. AndroidX without Maven: how it works and how to extend it

- `androidx.sh` blobless-clones a GitHub repo that commits a resolved Gradle
  cache as plain files (default `AuntiSaha/weather_app`: ~69 AARs / 10 jars
  including appcompat, material, core(+ktx), fragment, lifecycle,
  recyclerview, viewpager2, constraintlayout, cardview, coordinatorlayout),
  selects the highest version of an explicit WANT list
  (`toolchain/select_androidx.py`), extracts classes into one `androidx.jar`
  and aapt2-compiles each library's res into `res/*.zip` for linking.
- **To add an artifact that is in the dump**: append its base name to
  `WANT_AARS`/`WANT_JARS` and re-run `androidx.sh`.
- **To add an artifact that is NOT in the dump** (biometric, okhttp, okio,
  org.json were missing): the generic technique is to find *another* GitHub
  repo that happens to commit the file you need (Gradle-cache dumps, vendored
  libs dirs) and harvest just that blob:
  ```bash
  git clone --depth 1 --filter=blob:none --no-checkout <repo> /tmp/dump
  git -C /tmp/dump ls-tree -r --name-only HEAD | grep -i <artifact>
  git -C /tmp/dump checkout HEAD -- <path/to/file.aar>
  ```
  `gh api search/code` (filename queries) is the way to locate candidates.
  If no dump has it, fall back to compile-only stubs (§4).
- `kotlin-stdlib` is not optional next to AndroidX: activity/fragment/
  lifecycle are Kotlin-compiled and reference `kotlin.jvm.internal.*` on
  ordinary paths. `androidx_setup` already wires it in; keep it on BOTH the
  ECJ classpath and the D8 program input.

## 4. Classes you cannot harvest: compile-only stubs

- Write minimal Java sources in the SAME package, mirroring only the surface
  your code touches (one public class per file; nested classes for Builders).
  They exist purely so ECJ type-checks; the real library stays on the real CI
  classpath, so stub drift only ever costs you locally if you guess a
  signature wrong (ECJ will tell you).
- A ready-made sufficient surface for okhttp 4.x, if you need it:
  `OkHttpClient(+Builder: connectTimeout/readTimeout(long,TimeUnit),
  dispatcher, build; newCall)`, `Dispatcher(setMaxRequests,
  setMaxRequestsPerHost)`, `Request(+Builder: url(String|HttpUrl), header,
  post, get, build)`, `HttpUrl(get, newBuilder; Builder.addQueryParameter)`,
  `RequestBody(create(String,MediaType))`, `FormBody(+Builder.add)`,
  `MediaType(parse,get)`, `Call(enqueue)`, `Callback(onFailure(Call,
  IOException), onResponse(Call, Response))`, `Response(isSuccessful, body,
  implements AutoCloseable)`, `ResponseBody(string)`. Same idea for
  `androidx.biometric` (`BiometricPrompt` + `AuthenticationCallback` +
  `AuthenticationResult` + `PromptInfo.Builder`, `BiometricManager.
  Authenticators` int constants) if the harvest lacks it.
- **org.json gap**: android.jar's `org.json` is runtime-stubs that throw.
  JVM unit tests that touch `JSONObject` therefore cannot run on the vendored
  JRE unless you harvest a real `json-*.jar` (a dump repo, or any project
  vendoring it). Compile-only, ECJ is happy with the android.jar versions.

## 5. Compiler/linker gotchas (each one cost real time)

- ECJ `--source` ≤ 8 needs `core-lambda-stubs.jar` on the bootclasspath for
  lambdas (setup.sh builds it); > 8 switches to classpath mode automatically.
- aapt2: keep `--no-version-vectors` for pre-21 min-sdk, else vectors get
  emptied into `drawable-v21/` and inflate crashes on old devices.
- D8 `--min-api` and apksigner v1/v2 behaviour derive from the manifest's
  `minSdkVersion`; let `detect_layout` read it rather than overriding.
- Bare aapt2 can be STRICTER than a full CI pipeline: a values file with a
  stray `#`-prefixed line ("plain text not allowed here") failed here while
  an AGP-based CI tolerated it for many runs. A local FAILED is worth fixing
  but is not proof CI would fail.
- ECJ catches real API mistakes that parsers miss (e.g. Android's
  `Settings.EXTRA_BIOMETRIC_AUTHENTICATORS` does not exist — the constant is
  `..._AUTHENTICATORS_ALLOWED`; a 9-second pre-check beats a 2-minute CI
  round trip). **Negative-test your gate once**: plant a known-bad symbol,
  expect FAILED, revert. A gate that has never failed is untrusted.
- If the project relies on AGP-generated classes (ViewBinding, BuildConfig):
  generate stand-ins before ECJ. Rules with teeth: keep the RAW `@+id` string
  for `findViewById` (camel-casing is lossy, `key_00` → `key00`);
  `<include android:id>` fields are typed as the included layout's binding
  and bound via `XBinding.bind(...)`; merge-rooted layouts expose ONLY the
  two-arg `inflate(inflater, parent)` (AGP emits no third arg — generating
  one teaches your code a method CI rejects); the root's own id is a field,
  and `getRoot()` must return the root ELEMENT type (code calls
  `MaterialCardView` methods on it). Tag→type map needs at least the common
  widgets plus `NestedScrollView`, `ConstraintLayout`, `MaterialCardView`,
  `MaterialButton`, `ListView`; dotted tags are their own FQ class. Also:
  AGP injects the package from gradle `namespace`, but bare aapt2 link
  requires `package=` on `<manifest>` — link a sed-patched copy, keep the
  real manifest untouched.

## 6. CI interaction from this sandbox

- `gh run view --log/--log-failed` dies with EOF (the redirect host is
  outside the allowlist); the job page says "Sign in to view logs" even for
  public repos. `gh api repos/.../check-runs/<job>/annotations` WORKS but
  usually only carries the generic "Process completed with exit code 1".
- Poor man's classifier: `gh api .../actions/runs/<id>/jobs` step timings —
  a failure ~60-70 s into the test/compile step ≈ compile error; near the
  usual full duration ≈ a test ran and failed.
- If you own the workflow (permission permitting), make failures readable:
  `set -o pipefail; ./gradlew ... | tee step.log`, then an `if: failure()`
  step that greps `error:|FAILED|Exception|Caused by` and prints each line
  prefixed with `::error::` — workflow-command output becomes check-run
  ANNOTATIONS, which the annotations endpoint above returns. Also upload the
  raw logs as an artifact for humans.
- Otherwise the reliable channel is asking the user to paste the first
  `error:` lines from the web UI. Ask early; each guess costs a CI cycle.

## 7. Small recipes worth keeping

- Open fonts without Maven: `git clone --depth 1 --filter=blob:none --sparse
  https://github.com/google/fonts.git` + `sparse-checkout set ofl/<name>`;
  instance variable TTFs to static weights with
  `fontTools.varLib.instancer.instantiateVariableFont` (NOT
  `fontTools.instancer` — that module path does not exist).
- Screenshot forensics: `pip install pillow`; crop + LANCZOS upscale; render
  candidate fonts at the same size side by side instead of eyeballing.
- `gh api .../jobs --jq '.jobs[0].steps[]'` for the timing classifier.
- When a user's device screenshots disagree with CI state, treat pasted line
  numbers as a build fingerprint — they may be looking at an older APK.

— end; adjust or fold into RECIPE.md as you see fit.
