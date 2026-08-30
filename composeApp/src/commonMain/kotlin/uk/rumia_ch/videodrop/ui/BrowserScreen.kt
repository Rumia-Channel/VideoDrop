package uk.rumia_ch.videodrop.ui

import androidx.compose.runtime.Composable

/**
 * In-app browser per Clipbox spec: クリップボックスの「ブラウザ」相当
 * - サーチ/クリップ/ブラウザ選択を統合
 * - アドレスバー + 戻る/進む/更新 + ブックマーク/履歴 stubs
 * - WebView + FAB「＋」で yt-dlp 解析へ
 * Platform actual in androidMain (WebView) and jvmMain (placeholder)
 */
@Composable
expect fun BrowserScreen(
    onClipRequest: (url: String) -> Unit
)
