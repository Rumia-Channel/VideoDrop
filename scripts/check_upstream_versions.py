#!/usr/bin/env python3
"""
Upstream version checker per spec sections 4-8, 20.

Fetches latest stable releases for:
- yt-dlp (yt-dlp/yt-dlp)
- yt-dlp-ejs (yt-dlp/ejs)
- QuickJS (bellard.org LATEST.json)
- FFmpeg (FFmpeg/FFmpeg tags nX.Y.Z)

Usage:
  python scripts/check_upstream_versions.py
  python scripts/check_upstream_versions.py --json-output /tmp/plan.json

Exit codes:
  0 = success (changed or not, see JSON)
  1 = error
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, Any

# Validation regex per spec 20
RE_YT_DLP = re.compile(r"^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$")
RE_YT_DLP_EJS = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+$")
RE_QUICKJS = re.compile(r"^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
RE_FFMPEG = re.compile(r"^[0-9]+\.[0-9]+(\.[0-9]+)?$")

CONFIG_DEFAULT = Path(__file__).resolve().parent.parent / "config" / "upstream-versions.json"


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


def http_get_json(url: str, headers: Dict[str, str] | None = None) -> Any:
    req = urllib.request.Request(url, headers=headers or {})
    # GitHub API token to avoid rate limit
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("UPSTREAM_BOT_TOKEN") or os.environ.get("GH_TOKEN")
    if token and "api.github.com" in url:
        req.add_header("Authorization", f"Bearer {token}")
    req.add_header("Accept", "application/vnd.github.v3+json")
    req.add_header("User-Agent", "VideoDrop-upstream-checker/1.0")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = resp.read().decode("utf-8")
            return json.loads(data)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="ignore")[:2000]
        raise RuntimeError(f"HTTP {e.code} for {url}: {body}") from e


def fetch_yt_dlp() -> str:
    data = http_get_json("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
    tag = data.get("tag_name", "").strip()
    # draft/prerelease already excluded by /releases/latest, but double check
    if data.get("draft") or data.get("prerelease"):
        raise RuntimeError(f"yt-dlp latest is draft/prerelease: {tag}")
    # Some tags have 'v' prefix? yt-dlp uses bare date like 2026.08.19
    tag = tag.lstrip("v")
    if not RE_YT_DLP.match(tag):
        raise ValueError(f"yt-dlp tag invalid per spec: {tag!r}")
    return tag


def fetch_yt_dlp_ejs() -> str:
    data = http_get_json("https://api.github.com/repos/yt-dlp/ejs/releases/latest")
    tag = data.get("tag_name", "").strip()
    if data.get("draft") or data.get("prerelease"):
        raise RuntimeError(f"ejs latest is draft/prerelease: {tag}")
    tag = tag.lstrip("v")
    if not RE_YT_DLP_EJS.match(tag):
        raise ValueError(f"yt-dlp-ejs tag invalid per spec: {tag!r}")
    return tag


def fetch_quickjs() -> str:
    data = http_get_json("https://bellard.org/quickjs/binary_releases/LATEST.json")
    ver = str(data.get("version", "")).strip()
    if not RE_QUICKJS.match(ver):
        raise ValueError(f"QuickJS version invalid per spec: {ver!r}")
    return ver


def fetch_ffmpeg() -> str:
    # Use git ls-remote to get tags, then filter per spec
    # Spec: only ^n[0-9]+\.[0-9]+(\.[0-9]+)?$ after stripping n
    cmd = ["git", "ls-remote", "--tags", "https://github.com/FFmpeg/FFmpeg.git"]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
    if result.returncode != 0:
        raise RuntimeError(f"git ls-remote failed: {result.stderr[:2000]}")
    tags = []
    for line in result.stdout.splitlines():
        # format: <sha>\trefs/tags/<tag>^{} or .../refs/tags/<tag>
        parts = line.strip().split()
        if len(parts) < 2:
            continue
        ref = parts[1]
        # Remove ^{} suffix for dereferenced tags
        ref = ref.removesuffix("^{}")
        if not ref.startswith("refs/tags/"):
            continue
        tag = ref[len("refs/tags/"):]
        # Only nX.Y or nX.Y.Z, no rc/beta/alpha/dev/snapshot
        # Spec regex: ^n[0-9]+\.[0-9]+(\.[0-9]+)?$
        if not re.match(r"^n[0-9]+\.[0-9]+(\.[0-9]+)?$", tag):
            continue
        # Exclude anything with extra suffix already filtered, but also exclude n with dash
        # We have already filtered to only nX.Y pattern, so we can strip n
        ver = tag[1:]  # remove n
        if not RE_FFMPEG.match(ver):
            continue
        tags.append(ver)

    if not tags:
        raise RuntimeError("No FFmpeg stable tags found")

    # Version-aware sort: split by . and compare ints
    def version_key(v: str):
        return tuple(int(x) for x in v.split("."))

    tags_sorted = sorted(tags, key=version_key)
    latest = tags_sorted[-1]
    return latest


def load_current(path: Path) -> Dict[str, str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    # Normalize: expect { "yt-dlp": {"version": "x"}, ... }
    out = {}
    for key in ["yt-dlp", "yt-dlp-ejs", "quickjs", "ffmpeg"]:
        try:
            out[key] = str(data[key]["version"]).strip()
        except KeyError as e:
            raise KeyError(f"Missing {key}.version in {path}") from e
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description="Check upstream versions")
    parser.add_argument("--config", default=str(CONFIG_DEFAULT), help="Path to upstream-versions.json")
    parser.add_argument("--json-output", default=None, help="Write plan JSON to file")
    parser.add_argument("--check-only", action="store_true", help="Exit 0 if changed, 1 if not? Not used")
    args = parser.parse_args()

    config_path = Path(args.config)

    try:
        current = load_current(config_path)
    except Exception as e:
        eprint(f"Failed to load current config {config_path}: {e}")
        return 1

    # Fetch upstream
    errors = []
    upstream: Dict[str, str] = {}

    for name, fetcher in [
        ("yt-dlp", fetch_yt_dlp),
        ("yt-dlp-ejs", fetch_yt_dlp_ejs),
        ("quickjs", fetch_quickjs),
        ("ffmpeg", fetch_ffmpeg),
    ]:
        try:
            upstream[name] = fetcher()
            print(f"{name}: {current[name]} -> {upstream[name]}", file=sys.stderr)
        except Exception as e:
            errors.append(f"{name}: {e}")
            eprint(f"Failed to fetch {name}: {e}")

    if errors:
        eprint("One or more fetch errors:")
        for er in errors:
            eprint(f"  {er}")
        # If any fetch fails, we treat as no update to avoid partial false positives
        # But we still want to report
        # For spec, verification should fail and not merge
        # We'll return error status but still produce plan with changed=false
        # To be safe, exit 1 if all failed? Let's exit 0 with changed=false if partial?
        # Spec says checker should be deterministic; if fetch fails, don't produce PR
        # We'll produce changed=false and log
        plan = {"changed": False, "updates": {}, "errors": errors, "current": current, "upstream": upstream}
        print(json.dumps(plan, indent=2, ensure_ascii=False))
        if args.json_output:
            Path(args.json_output).write_text(json.dumps(plan, indent=2, ensure_ascii=False), encoding="utf-8")
        return 1 if len(errors) == 4 else 0

    # Compare
    updates: Dict[str, Dict[str, str]] = {}
    for key in current:
        old = current[key]
        new = upstream.get(key)
        if new is None:
            continue
        if old != new:
            updates[key] = {"old": old, "new": new}

    changed = len(updates) > 0

    plan = {
        "changed": changed,
        "updates": updates,
        "current": current,
        "upstream": upstream,
    }

    # Output
    print(json.dumps(plan, indent=2, ensure_ascii=False))
    if args.json_output:
        Path(args.json_output).write_text(json.dumps(plan, indent=2, ensure_ascii=False), encoding="utf-8")

    return 0


if __name__ == "__main__":
    sys.exit(main())
