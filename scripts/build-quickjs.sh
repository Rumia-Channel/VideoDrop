#!/usr/bin/env bash
set -euxo pipefail

# Local QuickJS build for ytdlpAndroid (mirrors CI) — spec 9章
# Research: bellard/quickjs cross via CROSS_PREFIX=host- + CC="clang --target=aarch64-linux-android24"
# Requires Android NDK r27 with clang --target support
# Usage: ./scripts/build-quickjs.sh

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK_VER="${NDK_VERSION:-27.0.12077973}"
API=24
QUICKJS_REF="${QUICKJS_REF:-master}"
TMPDIR="${TMPDIR:-/tmp}/quickjs-build-$$"

# Find NDK
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/ndk/$NDK_VER" ]; then
    ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$NDK_VER"
  elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT/ndk/$NDK_VER" ]; then
    ANDROID_NDK_HOME="$ANDROID_SDK_ROOT/ndk/$NDK_VER"
  fi
fi
TOOLCHAIN="${ANDROID_NDK_HOME:-}/toolchains/llvm/prebuilt/linux-x86_64"
if [ ! -x "$TOOLCHAIN/bin/clang" ]; then
  if ! command -v aarch64-linux-android24-clang >/dev/null 2>&1; then
    echo "NDK clang not found. Install NDK $NDK_VER and set ANDROID_NDK_HOME or PATH:"
    echo "  sdkmanager --install \"ndk;${NDK_VER}\""
    echo "  export ANDROID_NDK_HOME=\$ANDROID_HOME/ndk/${NDK_VER}"
    exit 1
  fi
  TOOLCHAIN="$(dirname "$(command -v aarch64-linux-android24-clang)")/.."
  TOOLCHAIN="$(cd "$TOOLCHAIN/.." && pwd)/toolchains/llvm/prebuilt/linux-x86_64"
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
rm -rf .obj || true

export CC="$TOOLCHAIN/bin/clang --target=aarch64-linux-android${API}"
export AR="$TOOLCHAIN/bin/llvm-ar"
export STRIP="$TOOLCHAIN/bin/llvm-strip"
export RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
echo "TOOLCHAIN=$TOOLCHAIN"
echo "CC=$CC"
make -j"$(nproc)" CROSS_PREFIX=host- CC="$CC" AR="$AR" STRIP="$STRIP" RANLIB="$RANLIB" LIBS="-lm" qjs
file qjs
"$TOOLCHAIN/bin/llvm-readelf" -h qjs | grep -E "Class|Machine|OS/ABI" || true
file qjs | grep -qi "aarch64" || (echo "Not aarch64" && exit 1)

DEST="$ROOT/ytdlpAndroid/src/main/jniLibs/arm64-v8a"
mkdir -p "$DEST"
cp -v qjs "$DEST/libqjsexec.so"
chmod 755 "$DEST/libqjsexec.so"
ls -lh "$DEST"
file "$DEST/libqjsexec.so"
echo "Done: $DEST/libqjsexec.so"
