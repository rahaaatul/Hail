#!/usr/bin/env python3
"""Upload a built APK to Telegram.

The upload is the job; the caption is metadata on it. Every dynamic value in
the caption passes through html.escape at the point of interpolation. Release
and pre-release captions read the Highlights subsection of the tagged version
out of CHANGELOG.md. Channel routing lives in .github/telegram.json so it
changes in a reviewable diff.
"""

import html
import json
import mimetypes
import os
import pathlib
import re
import sys
import urllib.request
import uuid

API = "https://api.telegram.org"
API_TIMEOUT = 60
SIZE_LIMIT = 50 * 1024 * 1024   # Telegram bot API sendDocument ceiling
CAPTION_LIMIT = 1024            # Telegram bot API caption ceiling
REPO = "rahaaatul/Hail"

# .github/scripts/upload.py -> three levels up is the repository root.
ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
CHANGELOG = ROOT / "CHANGELOG.md"
TOPICS = ROOT / ".github" / "telegram.json"

HEADING = re.compile(r"^#{1,6}\s+(.*?)\s*#*\s*$")
BULLET = re.compile(r"^\s*[*+-]\s+(.*)$")
TAGS = re.compile(r"<[^>]+>")


def required(name: str) -> str:
    value = os.environ.get(name, "")
    if not value:
        sys.exit(f"::error::Missing required environment variable: {name}")
    return value


def visible(text: str) -> int:
    """Telegram counts a caption after entity parsing, so markup is free."""
    return len(TAGS.sub("", text))


def topic_for(channel: str) -> int:
    topics = json.loads(TOPICS.read_text(encoding="utf-8")).get("topics", {})
    if channel not in topics:
        sys.exit(f"::error::Unknown channel '{channel}'. Configured: {sorted(topics)}")
    return int(topics[channel])


def pr_caption(number: str, title: str) -> str:
    return (
        f"<p><strong>PR #{html.escape(number)}</strong></p>\n"
        f"<blockquote>\n<p>{html.escape(title)}</p>\n</blockquote>"
    )


def debug_caption(sha: str, subject: str) -> str:
    return (
        f'<p><strong><a href="https://github.com/{REPO}/commit/{html.escape(sha)}">'
        f"Build {html.escape(sha[:7])}</a></strong></p>\n"
        f"<blockquote>\n<p>{html.escape(subject)}</p>\n</blockquote>"
    )


def changelog_link(tag: str) -> str:
    url = f"https://github.com/{REPO}/releases/tag/{html.escape(tag)}"
    return f'<blockquote>\n<a href="{url}">See full changelog</a>\n</blockquote>'


def release_section(version: str) -> list[str] | None:
    """Lines belonging to one release, excluding its `## [x.y.z] - date` marker.

    The marker only locates the section; it is never rendered.
    """
    if not CHANGELOG.is_file():
        return None
    lines = CHANGELOG.read_text(encoding="utf-8").splitlines()
    head = re.compile(rf"^##\s+\[?{re.escape(version)}\]?(\s|$)")
    start = next((i for i, line in enumerate(lines) if head.match(line)), None)
    if start is None:
        return None
    end = next(
        (j for j in range(start + 1, len(lines)) if lines[j].startswith("## ")),
        len(lines),
    )
    return lines[start + 1:end]


def highlights(section: list[str]) -> list[str]:
    """Bullet text under `### Highlights`, and nothing else."""
    out: list[str] = []
    inside = False
    for raw in section:
        line = raw.rstrip()
        heading = HEADING.match(line)
        if heading:
            inside = heading.group(1).strip().lower() == "highlights"
            continue
        if inside:
            bullet = BULLET.match(line)
            if bullet:
                out.append(bullet.group(1))
    return out


def release_caption(tag: str) -> str:
    head = "<b>📚 Changelogs</b>\n\n"
    version = tag[1:] if tag.startswith("v") else tag
    section = release_section(version)
    items = highlights(section) if section else []

    if items:
        bullets = "\n".join(f"‣ {html.escape(text)}" for text in items)
        caption = f"{head}<b>Highlights</b>\n<blockquote>\n{bullets}\n</blockquote>"
        # Highlights sections are short today. If a future one outgrows the
        # caption limit, degrade to the link rather than fail the upload.
        if visible(caption) <= CAPTION_LIMIT:
            return caption

    return head + changelog_link(tag)


def send_document(token: str, group: str, topic: int, path: str, caption: str) -> None:
    boundary = uuid.uuid4().hex

    def field(name: str, value: str) -> bytes:
        return (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n"
        ).encode()

    # The filename lands in a header, so it is sanitised rather than trusted.
    safe_name = "".join(
        c for c in os.path.basename(path) if c.isalnum() or c in "-._"
    )
    ctype = mimetypes.guess_type(path)[0] or "application/octet-stream"

    with open(path, "rb") as handle:
        payload = field("chat_id", group)
        payload += field("message_thread_id", str(topic))
        payload += field("caption", caption)
        # Every caption above is HTML. Without this field Telegram applies no
        # parse mode and posts the markup literally, so subscribers would read
        # "<b>📚 Changelogs</b>" instead of formatted text.
        payload += field("parse_mode", "HTML")
        payload += (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="document"; filename="{safe_name}"\r\n'
            f"Content-Type: {ctype}\r\n\r\n"
        ).encode() + handle.read() + f"\r\n--{boundary}--\r\n".encode()

    request = urllib.request.Request(
        f"{API}/bot{token}/sendDocument",
        data=payload,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    with urllib.request.urlopen(request, timeout=API_TIMEOUT) as response:
        response.read()


def main() -> None:
    path = required("APK_PATH")
    channel = required("TG_CHANNEL")
    # Checked before anything else touches the file. release.yml runs this step
    # with `if: always()`, so a failed build or rename lands here, and an
    # unguarded stat() would bury the real cause under a traceback.
    if not os.path.isfile(path):
        sys.exit(f"::error::APK not found: {path}")
    size = pathlib.Path(path).stat().st_size

    if size > SIZE_LIMIT:
        # The artifact is already on the GitHub draft, so an oversized upload
        # costs the notification, not the release. Warn and exit 0.
        print(
            f"::warning::APK is {size / 1024 / 1024:.1f} MB, over Telegram's "
            f"50 MB sendDocument limit. Skipping the upload - the APK is still "
            f"available from the GitHub release. To fit it, build per ABI: "
            f"./gradlew :app:assemblePr -Pabi=arm64-v8a"
        )
        return

    token = os.environ.get("TG_TOKEN", "")
    if not token:
        # A `pull_request` run from a fork is given no repository secrets, so an
        # unset token is a normal external contribution, not a fault. The
        # upload.sh this replaces treated it as a dry run and exited 0; do the
        # same, so the log reads as a notice rather than an error about a secret
        # the workflow was never allowed to see.
        print(
            "::warning::TG_TOKEN is unset; skipping the Telegram upload. Pull "
            "requests from forks do not receive repository secrets. The APK is "
            "still available from this run's artifact."
        )
        return

    if channel == "pr":
        caption = pr_caption(
            os.environ.get("PR_NUMBER", "?"), os.environ.get("PR_TITLE", "")
        )
    elif channel == "debug":
        caption = debug_caption(
            os.environ.get("GITHUB_SHA", ""), os.environ.get("COMMIT_SUBJECT", "")
        )
    else:
        caption = release_caption(os.environ.get("RELEASE_TAG", ""))

    send_document(
        required("TG_TOKEN"),
        required("TG_GROUP"),
        topic_for(channel),
        path,
        caption,
    )
    print("::notice::APK sent to Telegram")


if __name__ == "__main__":
    main()
