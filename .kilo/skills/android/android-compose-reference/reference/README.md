# Compose Documentation Reference

Mirrors `https://developer.android.com/develop/ui/compose/*.md.txt` pages.

## Source

- Index: https://developer.android.com/develop/ui/compose/documentation.md.txt
- `context7` mirror: https://context7.com/websites/developer_android_develop_ui_compose/llms.txt (saved at `../developer_android_compose_llms.txt`)

## Layout

Files mirror the URL path under `develop/ui/compose/`, e.g.
`designsystems/material3.md.txt` ↔ `/develop/ui/compose/designsystems/material3.md.txt`.

## Pages that have no `.md.txt` endpoint (404)

These pages exist in the documentation index but Google's server does not
expose a `*.md.txt` version:

- `semantics.md.txt` — page redirected to `/develop/ui/compose/accessibility/semantics`
- `testing-cheatsheet.md.txt` — page lives under `/develop/ui/compose/testing/`

To grab them, fetch the HTML and run a markdown extractor, or read them
directly in a browser.

## Regenerate

```bash
cd .kilo/reference/Compose
curl -fsSL https://developer.android.com/develop/ui/compose/documentation.md.txt -o documentation.md.txt
grep -oE 'https://developer\.android\.com/develop/ui/compose/[^)]+' documentation.md.txt \
  | sed 's|https://developer.android.com/develop/ui/compose/||;s|/$||' | sort -u \
  | awk '{print "https://developer.android.com/develop/ui/compose/" $0 ".md.txt"}' > urls.txt
mkdir -p animation designsystems layouts migrate touch-input/user-interactions
while IFS= read -r url; do
  rel=$(echo "$url" | sed 's|https://developer.android.com/develop/ui/compose/||')
  curl --retry 3 --retry-delay 1 -fsSL "$url" -o "$rel" || echo "FAILED: $url"
done < urls.txt
```