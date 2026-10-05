#!/usr/bin/env bash
# precheck.sh -- compile PalmPay's Java + XML locally with the GhostHand
# toolchain (sibling clone, see https://github.com/JohnOlowe/GhostHand branch
# arena/01a0d3a8-ghosthand) BEFORE pushing, so GitHub CI only ever sees code
# that already type-checks. Catches the same class of errors CI does:
# missing symbols, wrong constants, unknown view ids, broken resources.
#
#   GHOSTHAND=/path/to/GhostHand bash tools/precheck.sh
#
# Uses only vendored tools (ECJ, aapt2, android.jar API 34, the harvested
# AndroidX jars); nothing is pushed and no binary enters git.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$HERE")"
GH="${GHOSTHAND:-$ROOT/../GhostHand}"
[ -d "$GH/toolchain" ] || { echo "GhostHand not found at $GH (set GHOSTHAND=...)"; exit 1; }

[ -x "$GH/toolchain/vendor/jre/bin/java" ] || bash "$GH/toolchain/setup.sh"
[ -s "$GH/toolchain/vendor/androidx/androidx.jar" ] || bash "$GH/toolchain/androidx.sh"

# shellcheck source=/dev/null
. "$GH/toolchain/lib.sh"
load_env
export GH_ANDROIDX=on
androidx_setup

PROJ="$ROOT/app"
MIN_API=26
JAVA_SRC_LEVEL=17
detect_layout "$PROJ"

BUILD="$HERE/precheck/build"
rm -rf "$BUILD"
mkdir -p "$BUILD"

# AGP supplies the package from gradle `namespace`; bare aapt2 wants it in
# the manifest, so link against a patched copy (real manifest untouched).
MANIFEST="$BUILD/AndroidManifest.xml"
sed 's/<manifest /<manifest package="damjay.palmpay.clone" /' \
    "$PROJ/src/main/AndroidManifest.xml" > "$MANIFEST"

msg "1/3 aapt2: compile resources + link manifest (R.java)"
aapt2_compile_res "$BUILD/res.zip"
RES_ZIP="$BUILD/res.zip"
aapt2_link "$BUILD/base.apk" "$BUILD/gen" --min-sdk-version "$MIN_API"

msg "2/3 generating view bindings + okhttp/biometric compile stubs"
COMB="$BUILD/allgen"
mkdir -p "$COMB"
cp -r "$BUILD/gen/." "$COMB/"
python3 "$HERE/gen_bindings.py" "$RES_DIR/layout" "$COMB"
cp -r "$HERE/stubs/." "$COMB/"

msg "3/3 ECJ: compile app sources against android.jar + androidx (source 17)"
if ecj_compile "$BUILD/classes" "$COMB"; then
    ok "$(find "$BUILD/classes" -name '*.class' | wc -l | tr -d ' ') class files"
    echo
    printf '\033[1;32mPRECHECK PASSED\033[0m  safe to push; CI should stay green\n'
else
    echo
    printf '\033[1;31mPRECHECK FAILED\033[0m  fix the errors above before pushing\n'
    exit 1
fi
