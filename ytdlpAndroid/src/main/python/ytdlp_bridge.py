"""
ytdlp_bridge.py - Python bridge for yt-dlp per spec sections 7-8, 14-15, 20, 22.
Responsibilities per spec:
- YoutubeDL, extract_info, format selection, download, progress_hook, postprocessor hook
- Exception normalization -> JSON
- JSON string transport (no PyObject leak)
- No Android API touch
- Default browser cookie hook (not Chrome-limited)
"""

import json
import traceback

_runtime = {}
_cancel_flags = set()
_cookies_config = {"from_browser": None, "cookies_file": None}


def version():
    return "python-ok"


def python_version():
    import sys
    return sys.version


def initialize(qjs_path, ffmpeg_dir):
    _runtime["qjs"] = qjs_path
    _runtime["ffmpeg"] = ffmpeg_dir
    return json.dumps({"qjs": qjs_path, "ffmpeg": ffmpeg_dir})


def set_cookies_from_browser(browser_key):
    """Set yt-dlp --cookies-from-browser for default browser hook (Chrome/Firefox/Edge/Brave etc. not Chrome-limited)"""
    _cookies_config["from_browser"] = browser_key
    _cookies_config["cookies_file"] = None
    return json.dumps(_cookies_config)


def set_cookies_file(path):
    """Fallback: set --cookies <file>"""
    _cookies_config["cookies_file"] = path
    _cookies_config["from_browser"] = None
    return json.dumps(_cookies_config)


def get_runtime():
    return json.dumps({**_runtime, **_cookies_config})


def _build_options(download=False, format_id=None, output_type="Video"):
    """
    Base options per spec section 8.
    """
    opts = {
        "quiet": True,
        "noplaylist": True,
        "restrictfilenames": False,
    }
    qjs = _runtime.get("qjs")
    if qjs:
        opts["js_runtimes"] = {"quickjs": {"path": qjs}}
    ffmpeg = _runtime.get("ffmpeg")
    if ffmpeg:
        opts["ffmpeg_location"] = ffmpeg

    # Cookie hook for default browser login state
    if _cookies_config.get("from_browser"):
        # yt-dlp expects ('chrome',) or ('firefox',) etc.
        # For security, we don't pass keyring; let yt-dlp handle.
        opts["cookiesfrombrowser"] = (_cookies_config["from_browser"],)
    elif _cookies_config.get("cookies_file"):
        opts["cookiefile"] = _cookies_config["cookies_file"]

    # Format selection
    if format_id:
        opts["format"] = format_id
    else:
        if output_type == "Audio":
            opts["format"] = "bestaudio/best"
        else:
            opts["format"] = "bestvideo*+bestaudio/best"

    if not download:
        opts["skip_download"] = True

    return opts


def _normalize_error(e):
    """Map Python exceptions to spec error strings per section 20."""
    msg = str(e)
    tname = type(e).__name__
    tb = traceback.format_exc()
    lower = msg.lower()
    if "private video" in lower:
        err = "PrivateVideo"
    elif "login required" in lower or "sign in" in lower:
        err = "LoginRequired"
    elif "video unavailable" in lower:
        err = "VideoUnavailable"
    elif "geo" in lower or "not available in your country" in lower:
        err = "GeoRestricted"
    elif "unsupported url" in lower:
        err = "UnsupportedUrl"
    elif "po token" in lower or "po_token" in lower:
        err = "PoTokenRequired"
    elif "quickjs" in lower or "js runtime" in lower:
        err = "JavaScriptRuntimeError"
    elif "ffmpeg" in lower:
        err = "FfmpegUnavailable"
    else:
        err = "Unknown"
    return {"error": err, "message": msg, "type": tname, "traceback": tb}


def check_ytdlp():
    try:
        import yt_dlp
        return json.dumps({"yt_dlp": yt_dlp.version.__version__})
    except Exception as e:
        return json.dumps(_normalize_error(e))


def check_ytdlp_ejs():
    try:
        import importlib.metadata
        ver = importlib.metadata.version("yt-dlp-ejs")
        return json.dumps({"yt_dlp_ejs": ver})
    except Exception:
        try:
            import yt_dlp_ejs
            return json.dumps({"yt_dlp_ejs": getattr(yt_dlp_ejs, "__version__", "unknown")})
        except Exception as e:
            return json.dumps(_normalize_error(e))


def extract_info(url):
    """
    Extract metadata without download.
    Returns JSON string: sanitized info on success, error JSON on failure.
    """
    try:
        from yt_dlp import YoutubeDL
        if not url or not url.strip().startswith(("http://", "https://")):
            return json.dumps({"error": "InvalidUrl", "message": "Invalid URL: " + str(url)})
        opts = _build_options(download=False)
        with YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
            sanitized = ydl.sanitize_info(info)
            return json.dumps(sanitized)
    except Exception as e:
        return json.dumps(_normalize_error(e))


def cancel_download(download_id):
    _cancel_flags.add(download_id)
    return json.dumps({"cancelled": download_id})


def is_cancelled(download_id):
    return download_id in _cancel_flags


def clear_cancel(download_id):
    _cancel_flags.discard(download_id)


def download(url, output_template, download_id, callback, format_id=None, output_type="Video"):
    """
    Download with progress_hook.
    output_template: e.g. /tmp/<id>/.../%(title)s.%(ext)s
    callback: Kotlin object with method on_progress(json_str) and should_cancel()
    Returns JSON string with final uri or error.
    """
    try:
        from yt_dlp import YoutubeDL
        if not url or not url.strip().startswith(("http://", "https://")):
            err = {"error": "InvalidUrl", "message": "Invalid URL"}
            callback.on_progress(json.dumps({"_type": "error", "id": download_id, "error": err}))
            return json.dumps(err)

        def progress_hook(data):
            try:
                should_cancel = False
                try:
                    should_cancel = bool(callback.should_cancel())
                except Exception:
                    pass
                if should_cancel or download_id in _cancel_flags:
                    raise InterruptedError("Cancelled")
                payload = {
                    "_type": "progress",
                    "id": download_id,
                    "status": data.get("status"),
                    "downloaded_bytes": data.get("downloaded_bytes"),
                    "total_bytes": data.get("total_bytes") or data.get("total_bytes_estimate"),
                    "speed": data.get("speed"),
                    "eta": data.get("eta"),
                    "percent": None,
                    "filename": data.get("filename"),
                }
                try:
                    if payload["downloaded_bytes"] and payload["total_bytes"]:
                        payload["percent"] = (payload["downloaded_bytes"] / payload["total_bytes"]) * 100.0
                except Exception:
                    pass
                callback.on_progress(json.dumps(payload))
            except InterruptedError:
                raise
            except Exception as inner:
                try:
                    callback.on_progress(json.dumps({"_type": "hook_error", "error": str(inner)}))
                except Exception:
                    pass

        opts = _build_options(download=True, format_id=format_id, output_type=output_type)
        opts["outtmpl"] = output_template
        opts["progress_hooks"] = [progress_hook]
        def post_hook(data):
            try:
                callback.on_progress(json.dumps({"_type": "postprocessing", "id": download_id, "status": data.get("status"), "postprocessor": data.get("postprocessor")}))
            except Exception:
                pass
        opts["postprocessor_hooks"] = [post_hook]
        _cancel_flags.discard(download_id)
        with YoutubeDL(opts) as ydl:
            qjs = _runtime.get("qjs")
            if qjs:
                import os
                if not os.path.exists(qjs):
                    err = {"error": "QuickJsUnavailable", "message": "QuickJS not found at " + qjs}
                    callback.on_progress(json.dumps({"_type": "error", "id": download_id, "error": err}))
                    return json.dumps(err)
            callback.on_progress(json.dumps({"_type": "started", "id": download_id}))
            ydl.download([url])
            callback.on_progress(json.dumps({"_type": "completed", "id": download_id, "template": output_template}))
            return json.dumps({"status": "Completed", "id": download_id, "template": output_template})
    except InterruptedError:
        try:
            callback.on_progress(json.dumps({"_type": "cancelled", "id": download_id}))
        except Exception:
            pass
        return json.dumps({"status": "Cancelled", "id": download_id})
    except Exception as e:
        err = _normalize_error(e)
        try:
            callback.on_progress(json.dumps({"_type": "error", "id": download_id, "error": err}))
        except Exception:
            pass
        return json.dumps(err)
