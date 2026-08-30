# Native executables per spec sections 9-10

MVP supports **arm64-v8a only**.

Place standalone PIE executables here, named as `.so` so Android extracts them as native libraries:

```
ytdlpAndroid/src/main/jniLibs/arm64-v8a/
  libqjsexec.so      # QuickJS qjs executable (PIE, not shared lib)
  libffmpegexec.so   # FFmpeg executable (PIE)
  libffprobeexec.so  # FFprobe executable (PIE)
```

## Why lib*.so name?

APK/AAB extracts `jniLibs/*.so` to `applicationInfo.nativeLibraryDir` automatically.
Spec section 9-10 requires using `context.applicationInfo.nativeLibraryDir` + `libqjsexec.so` etc.
Never copy to writable data directory and execute from there.

Gradle packaging:

```kotlin
android {
    packaging {
        jniLibs.useLegacyPackaging = true // ensures extraction to filesystem
    }
}
```

## QuickJS build

QuickJS must be >= current yt-dlp recommended performance (yt-dlp wiki QuickJS section).
Build via Android NDK (arm64-v8a, API 24+):

```bash
# Example cross-compile (adapt to latest QuickJS)
# https://github.com/bellard/quickjs
make CC="aarch64-linux-android24-clang" \
     LDFLAGS="-static" \
     qjs
# Verify
file qjs # -> ELF 64-bit LSB pie executable, ARM aarch64
# Rename and place
cp qjs ytdlpAndroid/src/main/jniLibs/arm64-v8a/libqjsexec.so
```

Sanity per spec:

```kotlin
ProcessBuilder(qjsPath, "--version")
```

Expected: exit 0 with version string. If fails, engine returns `RuntimeUnavailable.QuickJs` and blocks downloads.

## FFmpeg / FFprobe build

Build via NDK with minimal codecs for MVP per spec section 10:

- MP4, WebM, AAC, Opus, H.264, VP9, AV1 demuxer/muxer
- No re-encode; only `video stream + audio stream -> container merge`

Use https://github.com/FFmpeg/FFmpeg with NDK:

```bash
./configure \
  --target-os=android --arch=aarch64 --cpu=armv8-a \
  --cross-prefix=aarch64-linux-android- --cc=aarch64-linux-android24-clang \
  --disable-everything \
  --enable-demuxer=mp4,matroska,webm \
  --enable-muxer=mp4,matroska,webm,mp3 \
  --enable-decoder=h264,vp9,av1,aac,opus \
  --enable-encoder=aac \
  --enable-protocol=file,http,https,tls \
  --enable-small --disable-doc
make -j$(nproc)
# Rename
cp ffmpeg ytdlpAndroid/src/main/jniLibs/arm64-v8a/libffmpegexec.so
cp ffprobe ytdlpAndroid/src/main/jniLibs/arm64-v8a/libffprobeexec.so
```

Sanity:

```kotlin
ProcessBuilder(ffmpegPath, "-version")
ProcessBuilder(ffprobePath, "-version")
```

yt-dlp receives directory via `ffmpeg_location` option (nativeLibraryDir).

## ABI notes

- MVP only `arm64-v8a`. Add `x86_64` when emulator support needed per spec section 4.
- `abiFilters += "arm64-v8a"` in both ytdlpAndroid and composeApp/androidApp.

## Verification

`AndroidYtDlpEngine.runtimeStatus()` checks:

```
Python, yt-dlp, yt-dlp-ejs, QuickJS, FFmpeg, FFprobe, native ABI
```

UI Settings -> Runtime Information displays versions.

If QuickJS/FFmpeg missing, UI returns `QuickJsUnavailable` / `FfmpegUnavailable` and download is blocked before starting.

## TODO

- Add CI job to build QuickJS/FFmpeg via NDK and verify PIE + arm64.
- Check APK extracts: `unzip -l app.apk | grep libqjsexec`
- Test on real arm64 device (not emulator) per spec section 27.

