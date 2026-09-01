# ripgrep (rg) Skill

ripgrep (`rg`) is a line-oriented search tool that recursively searches directories for regex patterns. Written in Rust, it combines the usability of The Silver Searcher with the raw speed of grep, and ships with sensible defaults: it respects `.gitignore`, skips hidden files, and skips binary files out of the box.

## When to Use

- Searching code or text for patterns across a project
- Finding function definitions, imports, TODOs, secrets, etc.
- Replacing `grep -r`, `grep -ri`, `ag`, or `ack`
- When you need fast, colorized results with file paths and line numbers
- Interactive fuzzy search when combined with `fzf`
- Extracting specific matches (`-o`) for piping to other tools
- Searching compressed files (`-z`) or files with non-UTF-8 encodings (`-E`)
- Searching through preprocessed content like PDFs (`--pre`)

## Basic Usage

```bash
rg PATTERN              # Search current directory recursively
rg PATTERN PATH         # Search specific path
rg 'fn main' src/       # Search in src/ directory
rg -e -foo              # Search for pattern starting with a dash
rg -- foo               # Alternative: use -- to separate flags from pattern
```

## Search Options

### Case & Matching

| Flag | Description |
|------|-------------|
| `-i` / `--ignore-case` | Search case-insensitively |
| `-s` / `--case-sensitive` | Search case-sensitively (default) |
| `-S` / `--smart-case` | Case-insensitive if pattern is all-lowercase, else case-sensitive |
| `-F` / `--fixed-strings` | Treat pattern as a literal string, not regex (much faster) |
| `-w` / `--word-regexp` | Match whole words only (surrounds pattern with `\b`) |
| `-x` / `--line-regexp` | Match the entire line (surrounds pattern with `^...$`) |
| `-v` / `--invert-match` | Show non-matching lines |
| `-o` / `--only-matching` | Print only the matched portion of each line |
| `-m N` / `--max-count=N` | Stop after N matches per file |

### Context Lines

| Flag | Description |
|------|-------------|
| `-A N` / `--after-context=N` | Show N lines after each match |
| `-B N` / `--before-context=N` | Show N lines before each match |
| `-C N` / `--context=N` | Show N lines before and after (equivalent to `-A N -B N`) |
| `--context-separator=SEP` | String separating non-contiguous context (default: `--`) |

### Regex Engines

| Flag | Description |
|------|-------------|
| `-P` / `--pcre2` | Use the PCRE2 regex engine (supports lookaround and backreferences) |
| `--engine=ENGINE` | Explicitly choose engine: `default`, `pcre2`, or `auto` |
| `--auto-hybrid-regex` | Use PCRE2 only when pattern requires it (alias for `--engine=auto`) |
| `--no-unicode` | Disable Unicode mode for ASCII-only patterns (faster with `\w`, `\s`, `\d`) |
| `--no-pcre2-unicode` | Disable Unicode in PCRE2 mode (faster but less correct) |

> **Note:** PCRE2 is an optional build feature. Check availability with `rg --pcre2-version`. The default finite-automaton engine is faster but does not support lookaround or backreferences. Use `--engine=auto` to let ripgrep pick the right engine per-pattern.

### Multiline Search

| Flag | Description |
|------|-------------|
| `-U` / `--multiline` | Enable multiline matching (`.` does not match `\n` by default) |
| `--multiline-dotall` | Make `.` match newlines in multiline mode (like `(?s)` in PCRE2) |

> **Warning:** Multiline mode reads entire files into memory and disables memory mapping. This can be slow or OOM on large files.

## File Filtering

### File Types

| Flag | Description |
|------|-------------|
| `-t TYPE` / `--type=TYPE` | Search only files of given type (e.g., `rust`, `python`, `js`) |
| `-T TYPE` / `--type-not=TYPE` | Exclude files of given type |
| `--type-list` | List all available file types and their globs |
| `--type-add=SPEC` | Define a custom file type: `'name:*.{ext1,ext2}'` |
| `--type-add=SPEC` | Include other types: `'name:include:cpp,py,md'` |
| `--type-clear=TYPE` | Clear all globs for a type |

### Glob Patterns

| Flag | Description |
|------|-------------|
| `-g GLOB` / `--glob=GLOB` | Include files matching glob; use `!` prefix to exclude |
| `--iglob=GLOB` | Same as `-g` but case-insensitive |
| `--glob-case-insensitive` | Make all glob patterns case-insensitive |
| `--max-depth=N` / `-d N` | Limit directory traversal to N levels |
| `--max-filesize=SIZE` | Skip files larger than SIZE (e.g., `50K`, `80M`) |
| `--one-file-system` | Don't cross filesystem boundaries (like `find -xdev`) |
| `-L` / `--follow` | Follow symbolic links during directory traversal |

> **Gotcha:** `-g` always overrides other ignore logic. When matching directories, use a trailing `**` — e.g., `-g 'foo/**'`, not `-g 'foo'`.

## Hidden/Ignored Files

| Flag | Description |
|------|-------------|
| `-.` / `--hidden` | Search hidden files and directories (dotfiles) |
| `-u` / `--unrestricted` | Disable ignore filtering (level 1: `--no-ignore`) |
| `-uu` | Also search hidden files (`--no-ignore --hidden`) |
| `-uuu` | Also search binary files (`--no-ignore --hidden --binary`) |
| `--no-ignore` | Don't respect `.gitignore`, `.ignore`, `.rgignore` |
| `--no-ignore-vcs` | Don't respect `.gitignore` only |
| `--no-ignore-dot` | Don't respect `.ignore` / `.rgignore` |
| `--no-ignore-parent` | Don't read ignore files from parent directories |
| `--no-ignore-global` | Don't respect global gitignore |
| `--no-ignore-exclude` | Don't respect `.git/info/exclude` |
| `--ignore-file=PATH` | Use a custom ignore file (gitignore format) |
| `--ignore-file-case-insensitive` | Process ignore files case-insensitively |
| `--no-require-git` | Respect `.gitignore` even outside a git repo |
| `--binary` | Search binary files (stops at NUL after a match) |
| `-a` / `--text` | Search binary files as if they were text (may emit terminal escape codes) |
| `--no-ignore-messages` | Suppress warnings from malformed ignore files |

> **Gotcha:** By default, `rg` respects `.gitignore` and skips hidden files. This is the #1 reason people think `rg` "missed" results — use `-u` or `--no-ignore` to check.

## Output Control

| Flag | Description |
|------|-------------|
| `-n` / `--line-number` | Show line numbers (1-based; on by default for tty output) |
| `-N` / `--no-line-number` | Suppress line numbers |
| `--column` | Show column numbers (1-based; implies `--line-number`) |
| `-H` / `--with-filename` | Always print file path with matches (default for multi-file) |
| `-I` / `--no-filename` | Never print file path (default for single file / stdin) |
| `--heading` | Print file path above match clusters (default for tty) |
| `--no-heading` | Disable heading; always prefix with filename |
| `-c` / `--count` | Print number of matching *lines* per file |
| `--count-matches` | Print number of individual *matches* per file |
| `--include-zero` | With `-c`, print count for files with zero matches |
| `--files` | List files that would be searched, then exit |
| `-0` / `--null` | Separate file paths with NUL (for `xargs -0`) |
| `-p` / `--pretty` | Shorthand for `--color=always --heading --line-number` |
| `--vimgrep` | Print every match on its own line (for editors; can be verbose) |
| `--passthru` / `--passthrough` | Print both matching and non-matching lines |
| `-q` / `--quiet` | Silent output; use exit code only (0 = found, 1 = not found) |
| `--color=WHEN` | `always`, `auto` (default), `never`, or `ansi` |
| `--colors=SPEC` | Custom colors: `match:fg:red`, `line:bg:yellow`, etc. |
| `--path-separator=SEP` | Override path separator (default: `/` on Unix) |
| `--trim` | Strip leading whitespace from each output line |
| `--null-data` | Use NUL as line terminator (for binary/searching NUL-delimited data) |
| `-M N` / `--max-columns=N` | Truncate lines longer than N columns (0 = unlimited) |
| `--max-columns-preview` | Show preview of truncated lines |

## Replacements (Output Only)

| Flag | Description |
|------|-------------|
| `-r TEXT` / `--replace=TEXT` | Replace matches with text in output (does NOT modify files) |

> **Important:** ripgrep **never modifies files**. The `-r` flag only transforms output on stdout. Use capture groups with `$1`, `$2`, or `${name}` syntax.

```bash
# Simple replacement
rg 'old_name' -r 'new_name'

# Using capture groups
rg '(\w+): (\d+)' -r '$1 -> $2'

# Named capture groups
rg '(?P<name>\w+): (?P<value>\d+)' -r '${name} = ${value}'
```

## File Encoding

| Flag | Description |
|------|-------------|
| `-E ENC` / `--encoding=ENC` | Specify file encoding (e.g., `utf-16`, `latin-1`, `gbk`, `euc-jp`, `shift_jis`) |

> ripgrep automatically detects UTF-16 via BOM. For other encodings, use `-E`. Without BOM, files are assumed UTF-8.

## Compressed Files

| Flag | Description |
|------|-------------|
| `-z` / `--search-zip` | Search compressed files (gzip, bzip2, xz, lzma, lz4, brotli, zstd) |

> Requires corresponding decompression binaries (`gzip`, `bzip2`, `xz`, `lz4`, `brotli`, `zstd`) to be installed. Does not search archive formats like `*.tar.gz`.

## Preprocessor

| Flag | Description |
|------|-------------|
| `--pre=COMMAND` | Run command on each file before searching (receives file path as arg) |
| `--pre-glob=GLOB` | Only apply preprocessor to files matching glob |

> Useful for searching PDFs, decrypting files, or any content that needs transformation. The command receives the file path and should output text to stdout.

```bash
# Search PDFs using pdftotext
echo '#!/bin/bash
pdftotext "$1" -' > /usr/local/bin/rg-pdf
chmod +x /usr/local/bin/rg-pdf
rg --pre rg-pdf 'pattern' --glob '*.pdf'
```

## Performance

| Flag | Description |
|------|-------------|
| `-j N` / `--threads=N` | Number of threads (0 = auto; default uses CPU core count) |
| `--mmap` | Force memory-mapped I/O (default for single-file searches) |
| `--no-mmap` | Disable memory mapping (useful for files that may be truncated) |
| `--sort=SORTBY` | Sort results: `path`, `modified`, `accessed`, `created`, `none` |
| `--sortr=SORTBY` | Reverse sort order |
| `--stats` | Print search statistics (files searched, bytes, matches, time) |
| `--no-progress` | Suppress progress messages (useful for large searches) |
| `--dfa-size-limit=SIZE` | Increase DFA cache for large pattern files (e.g., `1G`) |

> **Performance tips:**
> - Use `-F` (fixed strings) instead of regex when the pattern is literal — it's significantly faster.
> - `rg` automatically respects `.gitignore` — let it rather than disabling filters.
> - `--sort` disables parallelism — avoid it if you don't need sorted output.
> - `--mmap` is faster for large single files but disabled on macOS by default.
> - Use `--no-unicode` with `\w`, `\s`, `\d` when you only need ASCII matching.
> - For large pattern files (`-f`), increase `--dfa-size-limit` if searches are slow.
> - Use `-j1` to reduce memory usage (disables output buffering per file).

## Pattern Files

| Flag | Description |
|------|-------------|
| `-f FILE` / `--file=FILE` | Read patterns from file (one per line) |

> When using pattern files, ripgrep reports a match if **any** pattern matches. For large pattern files, consider `--dfa-size-limit` to improve performance.

## Common Pitfalls and Gotchas

1. **Missing results due to `.gitignore`:** By default, `rg` skips files in `.gitignore`. If you're not finding something, try `rg -u 'pattern'` or `rg --no-ignore 'pattern'`.

2. **Binary files are skipped:** `rg` silently skips files it detects as binary (containing NUL bytes). Use `rg -a` to force-text or `-uuu` / `--binary` to handle them.

3. **PCRE2 may not be compiled in:** If `rg -P` says "PCRE2 is not available", your build doesn't include it. Use `--engine=auto` to fall back gracefully, or install a version with PCRE2 support.

4. **Multiline mode reads whole files:** `rg -U` reads entire files into memory and disables memory mapping. This can be slow or OOM on large files.

5. **`-r` / `--replace` does NOT modify files:** The `-r` flag only transforms output on stdout. To modify files in place, pipe through `xargs` with `sed`, or use a tool like `sd`:
   ```bash
   rg 'old_name' -l | xargs sed -i 's/old_name/new_name/g'
   ```

6. **Hidden files not searched with `-u` alone:** `-u` only disables ignore rules. To also search hidden files, use `-uu` (or `--no-ignore --hidden`).

7. **`--sort` kills parallelism:** Sorting results forces single-threaded execution, which can slow down large searches significantly.

8. **Patterns starting with `-`:** Use `rg -e PATTERN` or `rg -- PATTERN` to search for patterns that look like flags.

9. **`.` does not match newlines:** Even in multiline mode, `.` won't match `\n` unless you use `--multiline-dotall` or `(?s)` in PCRE2.

10. **Memory usage with long lines:** Files with very long lines (common in minified JS) can cause high memory usage. Use `--max-columns` to limit this.

11. **Cygwin path translation:** On Cygwin, patterns starting with `/` may be mangled. Use `rg //foo` or `MSYS_NO_PATHCONV=1 rg /foo`.

## Examples

```bash
# Find all TODO comments
rg 'TODO|FIXME|HACK'

# Search only Rust files for a function
rg 'fn parse' -trust

# Find all imports of a crate
rg '^use serde' -trust

# Search excluding tests directory
rg 'MyStruct' -g '!tests/**'

# Case-insensitive search for a word
rg -w -i 'config'

# Show context around matches
rg -C 3 'panic!(' -trust

# Count matches per file (lines with matches)
rg -c 'import' -tpy

# Count individual matches (not lines)
rg --count-matches 'import' -tpy

# Replace text in output (does NOT modify files)
rg 'old_function' -r 'new_function' -trust

# Replace with capture groups in output
rg '(\w+): (\d+)' -r '{1} -> {2}'

# Search hidden files too
rg --hidden 'secret'

# Search binary files as text
rg -a 'needle' binary_blob.dat

# List all files that would be searched
rg --files -trust

# Show match counts including zero-match files
rg -c 'pattern' --include-zero

# Extract just the matched text
rg -o '[A-Za-z0-9._-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}' | sort -u

# Fixed-string search (faster, no regex)
rg -F '(.*)' --glob '*.log'

# PCRE2 regex with lookbehind
rg -P '(?<=\bfunction\s)\w+'

# Show column numbers
rg --column 'fn '

# Search compressed files
rg -z 'pattern' --glob '*.gz'

# Search specific depth
rg 'TODO' --max-depth 2

# Statistics
rg 'error' --stats

# Null-separated output for xargs
rg -l 'pattern' -0 | xargs -0 wc -l

# Pretty output for piping to less
rg -p 'pattern' | less -R

# Quiet mode for scripts (exit code only)
if rg -q 'pattern' file.txt; then echo "found"; fi

# Multiline search (e.g., multi-line log entries)
rg -U 'ERROR.*\n.*Exception' --multiline-dotall

# Search UTF-16 files
rg -E utf-16 'pattern'

# Search with multiple patterns (OR)
rg -f patterns.txt

# Auto-select regex engine based on pattern
rg --engine=auto '(?<=prefix)pattern'

# Truncate long lines in output
rg --max-columns 200 --max-columns-preview 'pattern'

# Search with debug output (shows why files are skipped)
rg --debug 'pattern'

# No config file (ignore RIPGREP_CONFIG_PATH)
rg --no-config 'pattern'
```

## File Types (Common)

```bash
rg --type-list          # List all types
-trust                  # Rust  (-t rust)
-tpy / -t python        # Python
-tjs                   # JavaScript
-ts                   # TypeScript
-tgo                   # Go
-tjava                 # Java
-tc / -t cpp           # C / C++
-thtml / -t css        # HTML / CSS
-tjson / -t yaml       # JSON / YAML
-ttoml / -t xml        # TOML / XML
-tmd                   # Markdown
-tsh / -t bash         # Shell scripts
-tsql                  # SQL
```

## Integration with Other Tools

```bash
# Open matches in editor
rg -l 'pattern' | xargs -o vim

# Search within fd results
fd -e rs -X rg 'pattern'

# Interactive ripgrep with fzf
rg --line-number --no-heading --color=always '' | \
  fzf --ansi --delimiter : --preview 'bat --style=numbers --color=always --highlight-line {2} {1}' \
      --preview-window '+{2}/2'

# Interactive ripgrep switching between rg and fzf modes
RG_PREFIX="rg --column --line-number --no-heading --color=always --smart-case "
fzf --ansi --disabled --bind "start:reload:$RG_PREFIX {q}" \
    --bind "change:reload:$RG_PREFIX {q} || true" \
    --bind 'ctrl-t:transform:[[ ! $FZF_PROMPT =~ ripgrep ]] &&
            echo "change-prompt(ripgrep> )+reload($RG_PREFIX {q} || true)" ||
            echo "change-prompt(fzf> )+enable-search"' \
    --delimiter : --preview 'bat --style=numbers --color=always --highlight-line {2} {1}' \
    --preview-window '+{2}/2' \
    --bind 'enter:become(vim {1} +{2})'

# Pipe to bat for syntax highlighting
rg -C 5 'pattern' --color=always | bat

# Generate tags for ctags
rg --tags

# Search with git log
git log -p | rg 'pattern'

# Find files containing multiple patterns (AND)
rg 'pattern1' | rg 'pattern2'

# Search and replace in files (using sd)
rg 'old_pattern' -l | xargs sd 'old_pattern' 'new_pattern'

# Fuzzy search with preview using fzf and bat
rg --line-number --no-heading --color=always --smart-case '' | \
  fzf --ansi --delimiter : \
      --preview 'bat --style=numbers --color=always --highlight-line {2} {1}' \
      --preview-window '+{2}/2'
```

## Configuration

Create `~/.config/ripgrep/ripgreprc` (or set `RIPGREP_CONFIG_PATH`) for default options:

```bash
# Don't let ripgrep show very long lines
--max-columns=150
--max-columns-preview

# Search hidden files by default
--hidden

# Exclude .git from searches
--glob=!.git/*

# Smart case matching
--smart-case

# Always show line numbers
--line-number

# Show context by default
--context=2

# Add custom file type
--type-add 'web:*.{html,css,js,ts,tsx,jsx}'
```

> **Tip:** Config file values are prepended to command-line args, so CLI flags override config. Use `--no-config` to ignore config entirely.

## Environment Variables

| Variable | Description |
|----------|-------------|
| `RIPGREP_CONFIG_PATH` | Path to config file |
| `NO_COLOR` | If set (any value), disables color output |

## CLI Reference (Quick)

| Command | Description |
|---------|-------------|
| `rg PATTERN` | Search current directory |
| `rg -i PATTERN` | Case-insensitive |
| `rg -F PATTERN` | Fixed string (fast) |
| `rg -w PATTERN` | Whole word match |
| `rg -v PATTERN` | Invert match |
| `rg -o PATTERN` | Only matching part |
| `rg -l PATTERN` | List filenames with matches |
| `rg -c PATTERN` | Count matches per file |
| `rg -n PATTERN` | Line numbers (default on tty) |
| `rg --column PATTERN` | Column numbers |
| `rg -C 3 PATTERN` | 3 lines context |
| `rg -A 3 PATTERN` | 3 lines after |
| `rg -B 3 PATTERN` | 3 lines before |
| `rg -P 'regex'` | PCRE2 engine |
| `rg -t rust PATTERN` | Only Rust files |
| `rg -g '!target/**'` | Exclude target |
| `rg -u PATTERN` | Ignore .gitignore |
| `rg -uu PATTERN` | Also hidden files |
| `rg -uuu PATTERN` | Also binary files |
| `rg --files` | List searchable files |
| `rg --stats` | Show performance stats |
| `rg -r REPL PATTERN` | Output replacement (not in-place!) |
| `rg -F PATTERN` | Fixed string |
| `rg -L` | Follow symlinks |
| `rg -j 8` | 8 threads |
| `rg -0 -l PATTERN` | Null-separated, for xargs -0 |
| `rg -U PATTERN` | Multiline search |
| `rg -z PATTERN` | Search compressed files |
| `rg -E utf-16 PATTERN` | Search UTF-16 files |
| `rg --pre CMD PATTERN` | Preprocess files |
| `rg --engine=auto PATTERN` | Auto-select regex engine |
| `rg --max-columns 200 PATTERN` | Truncate long lines |
| `rg --debug PATTERN` | Debug output |
| `rg --no-config PATTERN` | Ignore config file |

## Performance Notes

- ripgrep automatically respects `.gitignore` — it won't search `node_modules/`, `target/`, etc.
- It skips hidden files and binary files by default
- For maximum speed, let it use its defaults rather than disabling filters
- Use `-u` flags only when you suspect filtering is hiding results
- `--sort` always forces single-threaded mode — avoid unless you need sorted output
- `-F` (fixed strings) is significantly faster than regex for literal searches
- `--mmap` is faster for single files; buffered I/O is better for large directories
- Use `-j1` to reduce memory usage when parallelism isn't needed
- `--no-unicode` speeds up ASCII-only patterns with `\w`, `\s`, `\d`
