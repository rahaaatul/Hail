# gron Skill

Make JSON greppable — flatten JSON into discrete assignments for searching with grep/sed/awk, then ungron back to JSON.

## When to Use

- Searching JSON data with grep when you don't know the exact path
- Finding specific values in deeply nested API responses
- Debugging undocumented JSON APIs
- Extracting fields from JSON without learning jq syntax
- Converting JSON to line-oriented format for sed/awk processing
- Working with JSONL (newline-delimited JSON) logs

## Basic Usage

```bash
gron JSON_FILE          # Flatten JSON
gron URL                # Fetch and flatten remote JSON
echo '{"a":1}' | gron   # Flatten from stdin
gron --ungron            # Reverse: assignments back to JSON
```

## Options

| Flag | Description |
|------|-------------|
| `-u` / `--ungron` | Convert assignments back to JSON |
| `-v` / `--values` | Print just values (no paths) |
| `-c` / `--colorize` | Colorize output (default on tty) |
| `-m` / `--monochrome` | No colors |
| `-s` / `--stream` | Treat each input line as separate JSON object (JSONL) |
| `-k` / `--insecure` | Disable TLS certificate validation |
| `-j` / `--json` | Represent gron data as JSON stream |
| `--no-sort` | Skip output sorting (faster) |

## Understanding Output

Input:
```json
{"name": "John", "age": 30, "items": [1, 2, 3]}
```

Output:
```
json = {};
json.name = "John";
json.age = 30;
json.items = [];
json.items[0] = 1;
json.items[1] = 2;
json.items[2] = 3;
```

Each line shows the complete path to a value. Use `gron --ungron` to reverse.

## Search & Extract Patterns

```bash
# Flatten and grep
gron data.json | grep "key"

# Find nested path
gron data.json | grep "users\[0\]"

# Extract a value
gron data.json | grep "config.setting" | awk '{print $2}'

# Search API response
curl -s https://api.example.com/data | gron | grep "id"

# Multiple patterns
gron data.json | grep -E "id|name|email"

# Filter and reconstruct
gron data.json | grep -E "name|email" | gron --ungron
```

## Advanced: Ungron with Path Rewriting

Use `sed` to strip path prefixes before ungron for cleaner output:

```bash
# Flatten GitHub commits, extract name + message, flatten result
ggh | egrep "(committer.name|commit.message)" \
  | sed -r "s/(commit|committer)\.//g" \
  | gron --ungron

# Include parent URLs
ggh | egrep "(committer.name|commit.message|parents.*html_url)" \
  | sed -r "s/(commit|committer)\.//g" \
  | sed -r "s/\.html_url//" \
  | gron --ungron
```

## JSONL / Stream Mode

```bash
# Parse newline-delimited JSON (e.g., logs)
gron --stream app.log | grep "error"

# Process each line independently
cat events.jsonl | gron --stream --ungron
```

## Direct URL Fetching

```bash
# gron can fetch URLs directly (uses libcurl)
gron "https://api.github.com/repos/tomnomnom/gron/commits?per_page=1" | fgrep "commit.author"
```

## Common Pitfalls

- **Array null-padding**: When you filter array elements, gron preserves indices by inserting `null`. After ungron you get `[null, {...}]`. Use `sed` to strip index prefixes if you want a flat array.
- **Not for production pipelines**: gron is a reconnaissance/debugging tool. Error-prone for scripts — use jq for production data processing.
- **Large files**: Hundreds of MB produce millions of lines. Use `--no-sort` for speed, or switch to jq/`jq --stream`.
- **`--values` only works after gron**: Run `gron file.json | gron --values` to extract all values.

## Aliases (Recommended)

```bash
alias norg="gron --ungron"
alias ungron="gron --ungron"
```

## Integration

```bash
# With curl
curl -s https://api.github.com/users/octocat | gron | grep "name"

# With fzf
gron data.json | fzf

# Search and extract token
gron response.json | grep "token" | sed 's/.*= //;s/;//'

# Reconstruct subset
gron data.json | grep -E "name|email" | gron --ungron > subset.json

# Stream processing
tail -F app.log | gron --stream | grep --line-buffered "userId"
```

## Tips

- gron's primary purpose: find paths in unfamiliar JSON — then switch to jq for the actual extraction
- Use `--no-sort` for large files where ordering doesn't matter
- `--insecure` (`-k`) for self-signed cert APIs
- `gron | grep | gron --ungron` is the canonical edit-transform-restore workflow
