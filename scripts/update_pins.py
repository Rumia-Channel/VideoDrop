#!/usr/bin/env python3
"""
Sync config/upstream-versions.json -> generated pins.

Updates:
- ytdlpAndroid/requirements-android.txt
- ytdlpAndroid/build.gradle.kts (pip installs)
- (Future) QuickJS/FFmpeg build configs if they become generated

Idempotent: same upstream -> no file change -> empty git diff per spec 18.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "config" / "upstream-versions.json"
REQ = ROOT / "ytdlpAndroid" / "requirements-android.txt"
GRADLE = ROOT / "ytdlpAndroid" / "build.gradle.kts"

def load_config() -> dict:
    data = json.loads(CONFIG.read_text(encoding="utf-8"))
    return {
        "yt-dlp": str(data["yt-dlp"]["version"]).strip(),
        "yt-dlp-ejs": str(data["yt-dlp-ejs"]["version"]).strip(),
        "quickjs": str(data["quickjs"]["version"]).strip(),
        "ffmpeg": str(data["ffmpeg"]["version"]).strip(),
    }

def update_requirements(versions: dict) -> bool:
    # Validate
    if not re.match(r"^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$", versions["yt-dlp"]):
        raise ValueError(f"Invalid yt-dlp version: {versions['yt-dlp']}")
    if not re.match(r"^[0-9]+\.[0-9]+\.[0-9]+$", versions["yt-dlp-ejs"]):
        raise ValueError(f"Invalid ejs version: {versions['yt-dlp-ejs']}")

    content = f"""# Pinned yt-dlp versions for reproducible Android builds per spec section 5
# Single source: config/upstream-versions.json — do not edit manually, use scripts/update_pins.py
yt-dlp=={versions["yt-dlp"]}
yt-dlp-ejs=={versions["yt-dlp-ejs"]}
"""
    old = REQ.read_text(encoding="utf-8") if REQ.exists() else ""
    if old == content:
        print(f"No change {REQ}")
        return False
    REQ.write_text(content, encoding="utf-8")
    print(f"Updated {REQ}")
    return True

def update_gradle(versions: dict) -> bool:
    text = GRADLE.read_text(encoding="utf-8")
    # Replace pip installs
    # Pattern: install("yt-dlp==...") and install("yt-dlp-ejs==...")
    new_text = re.sub(
        r'install\("yt-dlp==[^"]+"\)',
        f'install("yt-dlp=={versions["yt-dlp"]}")',
        text
    )
    new_text = re.sub(
        r'install\("yt-dlp-ejs==[^"]+"\)',
        f'install("yt-dlp-ejs=={versions["yt-dlp-ejs"]}")',
        new_text
    )
    # Also handle case where versions were unpinned (install("yt-dlp")) -> pin them
    # If not found, try to replace install("yt-dlp") without version
    if 'install("yt-dlp==%s")' % versions["yt-dlp"] not in new_text:
        # Fallback: replace install("yt-dlp") -> pinned
        new_text = re.sub(
            r'install\("yt-dlp"\)',
            f'install("yt-dlp=={versions["yt-dlp"]}")',
            new_text
        )
    if 'install("yt-dlp-ejs==%s")' % versions["yt-dlp-ejs"] not in new_text:
        new_text = re.sub(
            r'install\("yt-dlp-ejs"\)',
            f'install("yt-dlp-ejs=={versions["yt-dlp-ejs"]}")',
            new_text
        )

    # Ensure comment says generated
    if "// Generated from config/upstream-versions.json" not in new_text:
        new_text = new_text.replace(
            "// Generated from config/upstream-versions.json — do not edit manually",
            "// Generated from config/upstream-versions.json — do not edit manually"
        )
        # If not present, ensure the pip block has comment
        if "Generated from config/upstream-versions.json" not in new_text:
            new_text = new_text.replace(
                "pip {",
                "pip {\n            // Generated from config/upstream-versions.json — do not edit manually"
            )

    if new_text == text:
        print(f"No change {GRADLE}")
        return False
    GRADLE.write_text(new_text, encoding="utf-8")
    print(f"Updated {GRADLE}")
    return True

def main() -> int:
    versions = load_config()
    print(f"Config versions: {versions}")

    changed_req = update_requirements(versions)
    changed_gradle = update_gradle(versions)

    # QuickJS/FFmpeg are not hardcoded in Gradle currently; they are built via CI from LATEST.json / tag
    # But we could generate a file like config/generated-quickjs-version.txt if needed
    # For now, just ensure no-change yields no diff
    changed = changed_req or changed_gradle
    if not changed:
        print("No pin changes — empty diff per spec 18")
    return 0

if __name__ == "__main__":
    sys.exit(main())
