#!/usr/bin/env bash
set -euxo pipefail

# Local QuickJS build for ytdlpAndroid (mirrors CI)
# Requires Android NDK r27 with aarch64-linux-android24-clang in PATH
# Usage: ./scripts/build-quickjs.sh

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK_VER="${NDK_VERSION:-27.0.12077973}"
API=24
QUICKJS_REF="${QUICKJS_REF:-master}"
TMPDIR="${TMPDIR:-/tmp}/quickjs-build-$$"

if ! command -v aarch64-linux-android24-clang >/dev/null 2>&1; then
  echo "NDK clang not found. Install NDK $NDK_VER and add to PATH:"
  echo "  sdkmanager --install \"ndk;${NDK_VER}\""
  echo "  export PATH=\$ANDROID_SDK_ROOT/ndk/${NDK_VER}/toolchains/llvm/prebuilt/linux-x86_64/bin:\$PATH"
  exit 1
fi

if [ -d /tmp/quickjs ]; then
  SRC=/tmp/quickjs
else
  SRC="$TMPDIR/quickjs"
  git clone https://github.com/bellard/quickjs.git "$SRC"
fi

cd "$SRC"
if [ "$QUICKJS_REF" != "master" ]; then
  git fetch --depth 1 origin "$QUICKJS_REF" && git checkout FETCH_HEAD
fi

make clean || true
export CC="aarch64-linux-android${API}-clang"
export AR="llvm-ar"
export STRIP="llvm-strip"
make -j"$(nproc)" qjs LDFLAGS="-static"

file qjs
./qjs --help 2>&1 | head -n 20 || true

DEST="$ROOT/ytdlpAndroid/src/main/jniLibs/arm64-v8a"
mkdir -p "$DEST"
cp -v qjs "$DEST/libqjsexec.so"
chmod 755 "$DEST/libqjsexec.so"
ls -lh "$DEST"
file "$DEST/libqjsexec.so"
echo "Done: $DEST/libqjsexec.so"
