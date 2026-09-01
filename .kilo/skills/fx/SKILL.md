# fx Skill

Terminal JSON viewer and processor — interactive TUI exploration plus JavaScript-based scripting for JSON, YAML, and TOML.

## When to Use

- Exploring unfamiliar JSON structures interactively
- Processing JSON with JavaScript expressions instead of jq DSL
- Debugging API responses with visual navigation
- Transforming, filtering, and aggregating JSON data
- Working with YAML/TOML configs
- Streaming newline-delimited JSON in real-time

## Two Modes

**Interactive TUI** (`fx file.json` or `cat data.json | fx`): Navigate, search, expand/collapse, yank values.
**Scripting** (`fx file.json '.expr'`): Apply JavaScript expressions, output to stdout.

## Interactive Mode Keys

| Key | Action |
|-----|--------|
| `j` / `k` or `↑` / `↓` | Navigate down/up |
| `Enter` | Expand/collapse node |
| `Space` | Expand/collapse children |
| `q` / `Ctrl+c` / `Esc` | Quit |
| `/` | Search (regex, `n`/`N` for next/prev, `/pattern/i` for case-sensitive) |
| `?` | Help |
| `.` | Enter "dig" mode — fuzzy-search path |
| `@` | Goto symbol (fuzzy search keys) |
| `[` / `]` | Jump back/forward in history |
| `p` / `P` | Preview / print value to stdout |
| `y` | Yank key, value, or path |
| `f` | Filter |
| `s` | Sort |
| `r` | Reverse |
| `e` / `E` | Expand / collapse all |
| `dd` | Delete node |
| `i` | Edit-in-place |
| `F` | Follow selected process |
| `Ctrl+g` | Go to ref |
| `s` | Toggle array/object size display |
| `%` | Toggle memory display mode |

Use `FX_NO_MOUSE=1` to disable mouse event redirection (useful for text selection).

## JavaScript Expressions

Use `x` or `this` to reference input. Dot shorthand (`.expr`) auto-wraps to `x => x.expr`.

```bash
# Dot shorthand
fx data.json '.name'

# Explicit arrow function
fx data.json 'x => x.users.map(u => u.name)'

# Using `this`
fx data.json '.name' '`Hello, ${this}!`'
```

### Common Patterns

```bash
# Extract field
fx data.json '.name'

# Nested field
fx data.json '.user.email'

# Array access
fx data.json '.items[0]'

# Filter array
fx data.json '.items.filter(x => x.price > 100)'

# Map
fx data.json '.items.map(x => x.name)'

# Sort
fx data.json '.items.sort((a,b) => a.name.localeCompare(b.name))'

# Reduce / sum
fx data.json '.items.reduce((sum, i) => sum + i.price, 0)'

# Count
fx data.json '.items | length'

# Group by
fx data.json '.items.reduce((acc, i) => {
  acc[i.category] = (acc[i.category] || 0) + 1;
  return acc;
}, {})'

# Flatten
fx data.json '.users.flatMap(u => u.orders)'

# Pretty print
fx data.json 'JSON.stringify(x, null, 2)'

# Conditional
fx data.json 'x.status === "ok" ? "pass" : "fail"'
```

## CLI Flags

| Flag | Description |
|------|-------------|
| `--slurp` / `-s` | Read all inputs into array |
| `--raw` | Treat input as raw text lines |
| `--yaml` | Parse input as YAML |
| `--toml` | Parse input as TOML |
| `--strict` | Strict JSON parsing (no comments/trailing commas) |
| `--no-inline` | Disable inline formatting of small objects/arrays |
| `--help` | Usage info |
| `--version` | Version |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FX_COLLAPSED=1` | Start in collapsed view |
| `FX_LINE_NUMBERS=1` | Show line numbers |
| `FX_NO_MOUSE=1` | Disable mouse events |

## Custom Functions

Create `~/.fxrc.js` for reusable functions:

```javascript
// ~/.fxrc.js
function present(x) {
  return x != null && x !== "";
}

function confidential(x) {
  return x.replace(/\b[A-Z]{2,}\d{4,}\b/g, "[REDACTED]");
}
```

Use them: `fx data.json '.filter(present).map(confidential)'`

## Examples

```bash
# Interactive exploration
fx data.json

# From curl
curl -s https://api.github.com/users/octocat | fx

# Extract nested value
fx data.json '.user.email'

# Filter array
fx data.json '.items.filter(x => x.active)'

# Transform and save
fx data.json 'x => x.users.map(u => ({name: u.name, email: u.email}))' > users.json

# Save with built-in save function
fx data.json 'x.name = x.name.toUpperCase(), x' save

# Slurp multiple JSON objects
cat data/*.json | fx --slurp '.reduce((a,b) => a + b)'

# Process YAML
fx config.yaml --yaml '.servers'

# Process TOML
fx config.toml --toml '.package.dependencies'

# HTTP headers with curl
curl -i https://api.example.com | fx

# Stream JSONL
tail -F events.jsonl | fx --slurp '.[-1]'

# Filter shortcut (v39+)
fx data.json '?.price > 100'
```

## Common Pitfalls

- fx uses Goja (ECMAScript 5.1) — no modern JS: no optional chaining, nullish coalescing, async/await, or imports
- Cannot access Node.js APIs or external libraries
- For very large files (GB+), fx loads into memory — use `jq --stream` instead
- TUI is unusable over high-latency SSH — use scripting mode
- `--slurp` requires Node.js or Deno runtime (not the built-in Goja engine in some versions)
- `--strict` rejects JSON with comments or trailing commas

## fx vs jq

| Task | jq | fx |
|------|-----|-----|
| Extract field | `jq '.name'` | `fx '.name'` |
| Filter array | `jq '.[] \| select(.active)'` | `fx '.filter(x => x.active)'` |
| Map | `jq 'map(.name)'` | `fx '.map(x => x.name)'` |
| Interactive | No | Yes |
| Large files | Streaming (`--stream`) | Memory-bound |
| Complex transforms | Powerful DSL | JavaScript familiarity |
| Learning curve | Steep | Lower for JS devs |

## Integration

```bash
# With curl
curl -s https://api.example.com/data | fx

# Pipe to bat for syntax highlighting
fx data.json 'JSON.stringify(x, null, 2)' | bat -l json

# With fzf
fx data.json | fzf

# With gron
fx data.json '.items' | gron

# Save extracted data
fx data.json '.items' > items.json
```

## Tips

- Use interactive mode to explore unknown JSON structures first
- Dot shorthand (`.field`) is equivalent to `x => x.field`
- `@` goto-symbol jumps to matching keys across the document
- `FX_COLLAPSED=1` speeds up opening huge files
- Use `save` function for in-place editing: `fx file.json 'x.key = "new", x' save`
