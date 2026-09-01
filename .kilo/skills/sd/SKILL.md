# sd Skill

Intuitive find-and-replace CLI — simpler alternative to `sed` for text replacement. Uses Rust regex syntax with JavaScript/Python-style capture groups.

## When to Use

- Replacing text patterns in files
- Simple string substitutions
- Replacing `sed -i 's/old/new/g'`
- When you want readable, simple syntax
- Multi-file replacements with globs
- Multi-line pattern matching
- When you need literal string mode (no regex escaping)

## Basic Usage

```bash
sd 'old' 'new' FILE              # Replace in file (in-place)
sd 'old' 'new' FILE1 FILE2       # Replace in multiple files
echo "text" | sd 'old' 'new'     # Replace in stdin
sd -p 'old' 'new' FILE           # Preview without modifying
```

## Common Patterns

### Basic Replacement

```bash
# Simple string replacement (in-place by default)
sd 'foo' 'bar' file.txt

# Regex replacement with capture groups
sd '(\w+)' '[$1]' file.txt

# Replace all occurrences (default behavior)
sd 'foo' 'bar' file.txt

# Replace first occurrence only
sd -n 1 'foo' 'bar' file.txt

# Replace limited occurrences
sd -n 5 'foo' 'bar' file.txt
```

### Regex Features

```bash
# Capture groups (indexed)
sd '(\d+)-(\d+)' '$2-$1' file.txt

# Named captures
sd '(?P<year>\d+)' 'Year: $year' file.txt

# Disambiguate named captures with curly braces
sd '(?P<dollars>\d+)\.(?P<cents>\d+)' '${dollars}_dollars' file.txt

# Multiline mode (dot matches newlines)
sd -m 'BEGIN.*?END' 'REPLACED' file.txt

# Case insensitive
sd -i 'foo' 'bar' file.txt

# Whole-word match
sd -w 'foo' 'bar' file.txt

# Combine flags
sd -f 'iw' 'foo' 'bar' file.txt    # case-insensitive + whole-word
```

### Fixed-String Mode (Literal)

```bash
# No regex interpretation — safe for special characters
sd -F '((([])))' '' file.txt
sd -F '1.0.0' '1.1.0' package.json
sd -F 'https://old.com' 'https://new.com' file.txt
```

### Across Mode (Multi-Line)

```bash
# Process entire input as one string (allows matching \n)
sd -A '\n' ',' file.txt            # Replace newlines with commas
sd -A 'foo\nbar' 'replaced' file.txt  # Match across lines

# Short form
sd -s 'start.*end' 'replacement' file.txt
```

### Preview Mode

```bash
# Preview changes without modifying
sd --preview 'foo' 'bar' file.txt

# Preview with color diff
sd --preview 'foo' 'bar' file.txt | delta
```

### Pipe Usage

```bash
# Replace in piped input
cat file.txt | sd 'foo' 'bar'

# Chain replacements
cat file.txt | sd 'foo' 'bar' | sd 'baz' 'qux'

# Replace in command output
git status | sd 'modified:' '→ '

# Read from stdin, write to stdout
sd 'window.fetch' 'fetch' < http.js > http_new.js
```

## Flags Reference

| Flag | Description |
|------|-------------|
| `-p` / `--preview` | Preview changes without modifying |
| `-F` / `--fixed-strings` | Treat patterns as literal strings |
| `-n N` / `--max-replacements N` | Limit replacements per file (0 = unlimited) |
| `-f FLAGS` / `--flags FLAGS` | Regex flags: `i` (case-insensitive), `w` (whole-word), `m` (multiline), `s` (dot-matches-newline) |
| `-A` / `--across` | Process entire input (allows `\n` matching) |
| `-s` / `--string-mode` | Alias for `--across` |
| `--` | Signal end of flags (for patterns starting with `-`) |

## Examples

```bash
# Replace function name across project
sd 'old_function' 'new_function' src/*.rs

# Remove whitespace at end of lines
sd '\s+$' '' file.txt

# Add prefix to lines
sd '^' '// ' file.txt

# Remove prefix
sd '^// ' '' file.txt

# Swap two words
sd '(\w+) (\w+)' '$2 $1' file.txt

# Replace quotes
sd '"' "'" file.txt

# Replace newlines with spaces (across mode)
sd -A '\n' ' ' file.txt

# Extract and reformat
sd '.*(\d{4}).*' '$1' file.txt

# Preview before replacing
sd --preview 'TODO' 'DONE' src/*.rs

# CommonJS to ES modules
sd "const (\w+) = require\('([^']+)\'" "import $1 from '$2'" src/**/*.js

# API version bump
sd '/api/v1/' '/api/v2/' src/**/*.ts

# Remove console.log statements
sd 'console\.log\([^)]*\);?\s*\n?' '' src/**/*.ts

# Rename with word boundaries (avoid partial matches)
sd '\bgetUser\b' 'fetchUser' src/**/*.ts

# Use with fd for project-wide replacement
fd -e ts -x sd 'oldApi' 'newApi' {}

# Use with rg to find and replace
rg -l 'oldPattern' src/ | xargs -I{} sd 'oldPattern' 'newPattern' {}
```

## Comparison with sed

| Task | sed | sd |
|------|-----|-----|
| Simple replace | `sed -i 's/old/new/g'` | `sd 'old' 'new'` |
| Capture groups | `sed -i 's/\(a\)/\1\1/g'` | `sd '(a)' '$1$1'` |
| Preview | `sed 's/old/new/g'` (no -i) | `sd --preview 'old' 'new'` |
| Pipe | `echo x \| sed 's/a/b/'` | `echo x \| sd 'a' 'b'` |
| Literal strings | `sed 's/\*\./_/g'` | `sd -F '*.' '_'` |
| Multi-line | `sed ':a;N;$!ba;s/\n/,/g'` | `sd -A '\n' ','` |
| No slash escaping | `sed 's\|/a\|/b\|'` | `sd '/a' '/b'` |

## Pitfalls and Gotchas

- **In-place by default**: sd modifies files directly. Use `--preview` first or have git as backup.
- **Arguments starting with `-`**: sd interprets them as flags. Use `--` delimiter:
  ```bash
  sd -- '-foo' 'bar' file.txt
  sd 'foo' -- '-bar' file.txt
  ```
- **Literal `$` in replacement**: Use `$$` to insert a literal dollar sign:
  ```bash
  echo "foo" | sd 'foo' '$$bar'   # Output: $bar
  ```
- **Line-by-line vs across mode**: Default is line-by-line (low memory, streaming). Use `-A` for multi-line patterns.
- **Named capture ambiguity**: Use `${var}` instead of `$var` when followed by identifier characters.
- **No backup files**: Unlike `sed -i.bak`, sd doesn't create backups. Use git.
- **Regex flavor**: Uses Rust's `regex` crate (similar to PCRE). Not POSIX-compatible.

## Tips

- sd uses Rust regex syntax (similar to PCRE, JavaScript, Python)
- No need to escape `/` in patterns
- Preview mode helps verify before replacing
- Works with stdin for piping workflows
- Capture groups use `$1`, `$2` (not `\1`, `\2`)
- Use `-F` when replacing version strings, URLs, or paths with special chars
- Combine with `fd -e ts -x sd 'old' 'new' {}` for type-safe project-wide replacements
- Combine with `rg -l` to find files first, then apply sd
- Always quote patterns to prevent shell interpretation
- Use `\b` for word boundaries to avoid partial matches
