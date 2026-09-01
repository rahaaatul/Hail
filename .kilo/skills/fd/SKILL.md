# fd Skill

Modern replacement for `find` — fast, intuitive file and directory search with sensible defaults and parallelized directory traversal.

## When to Use

- Finding files by name or pattern
- Listing files recursively (better than `ls -R`)
- Searching for files of a specific extension
- Replacing `find . -name`, `find . -iname`, `find . -type f`
- When you need to execute commands on found files
- When you need smart case-insensitive search by default
- When you want to respect (and customize) ignore files
- When you need to filter by file size, modification time, or ownership

## Basic Usage

```bash
fd PATTERN                         # Search current directory recursively (smart-case)
fd PATTERN PATH                    # Search specific path
fd .                               # List all files (like ls -R but recursive)
fd -g 'Cargo.toml'                 # Glob mode (exact name matching)
fd -e rs                           # All Rust files
```

**Note:** `fd` is not installed as `fd` on all systems. On Debian/Ubuntu, the package is `fd-find` and the binary is `fdfind`. Create a symlink or alias: `ln -s $(which fdfind) ~/.local/bin/fd` or `alias fd=fdfind`.

## Quick Reference

```bash
fd pattern                    # Smart-case search
fd -i pattern                 # Force case-insensitive
fd -s pattern                 # Force case-sensitive
fd -e rs -e toml              # Multiple extensions
fd -t f 'foo'                 # Files only
fd -t d 'test'                # Directories only
fd -u pattern                 # Search hidden + ignored files (unrestricted)
fd -H pattern                 # Also search hidden files
fd -I pattern                 # Also search ignored files
fd --max-depth 3 pattern      # Limit depth
fd --changed-within 1w        # Modified within last week
fd -S +10M                    # Larger than 10 MB
fd pattern -X rm              # Execute rm on all results
fd pattern -x touch {. }.bak  # Execute per result with placeholder
```

## Smart Case

By default, `fd` uses smart case:
- `fd make` → case-insensitive (matches `Makefile`, `makefile`, `MAKEFILE`)
- `fd Make` → case-sensitive (matches `Makefile` but not `makefile`)

Override with `-i` (force insensitive) or `-s` (force sensitive).

## Search Options

| Flag | Description |
|------|-------------|
| `-i` / `--ignore-case` | Case-insensitive search |
| `-s` / `--case-sensitive` | Case-sensitive search (default: smart case) |
| `-g` / `--glob` | Glob mode (exact shell pattern matching, wildcards in pattern) |
| `-F` / `--fixed-strings` | Treat pattern as literal string (substring match, not regex) |
| `-p` / `--full-path` | Search full absolute paths instead of just filenames |
| `--max-depth N` / `-d N` | Limit directory traversal to N levels |
| `--min-depth N` | Skip results at depth < N (e.g., `--min-depth 2` skips current dir children) |
| `--exact-depth N` | Only show results at exact depth (alias for `--min-depth N --max-depth N`) |
| `--max-results N` | Stop after N results |
| `-1` | Limit to single result (alias for `--max-results=1`) |
| `--changed-within TIME` | Files modified within a duration (e.g., `1w`, `2 days`, `1 month`) |
| `--changed-before TIME` | Files modified before a date or duration |
| `--changed-after TIME` | Files modified after a date or duration |
| `--size SIZE` | Filter by size (e.g., `+10M`, `-100k`, `1M..10M`) |
| `-o USER[:GROUP]` / `--owner USER[:GROUP]` | Filter by owning user and/or group |
| `-u` / `--unrestricted` | Search hidden and ignored files (`--hidden --no-ignore`) |
| `-H` / `--hidden` | Include hidden files and directories |
| `-I` / `--no-ignore` | Do not respect `.gitignore` |
| `--no-ignore-vcs` | Show results from VCS-ignored files (`.gitignore`, `.git/info/exclude`, global gitignore) |
| `--no-ignore-parent` | Don't read ignore files from parent directories |
| `--no-require-git` | Respect `.gitignore` even outside a git repo |
| `-E GLOB` / `--exclude GLOB` | Exclude files/directories matching glob |
| `--ignore-file PATH` | Additional ignore file (gitignore format) |
| `--ignore-contain NAME` | Ignore directories containing a file/directory named NAME |
| `--prune` | Do not traverse into matching directories |
| `--one-file-system` / `--mount` / `--xdev` | Don't cross filesystem boundaries |
| `--show-errors` | Display filesystem errors (permission denied, dead symlinks) |

### Time-based Filtering

fd supports flexible time specifications:

```bash
# Duration-based (relative)
fd --changed-within 1w          # Modified within last week
fd --changed-within '2 days'    # Modified within 2 days
fd --changed-before '1 month'   # Modified more than a month ago
fd --changed-within '1 hour'    # Modified within last hour

# Date-based (absolute)
fd --changed-before '2024-01-01 12:00:00'  # Before specific date
fd --changed-after 2024-06-01               # After June 1, 2024

# Combine for a range
fd --changed-after 2024-01-01 --changed-before 2024-12-31
```

### Size-based Filtering

```bash
fd --size +10M                  # Larger than 10 MB
fd --size -100k                 # Smaller than 100 KB
fd --size 1M..10M               # Between 1 MB and 10 MB
fd -t f --size +100M            # All files larger than 100 MB
```

## File Type Filtering

| Flag | Description |
|------|-------------|
| `-t f` / `--type file` | Files only |
| `-t d` / `--type directory` | Directories only |
| `-t l` / `--type symlink` | Symlinks only |
| `-t x` / `--type executable` | Executables only (implies `-t f`) |
| `-t e` / `--type empty` | Empty files/directories (use with `-t f` or `-t d` for specific) |
| `-t s` / `--type socket` | Sockets only |
| `-t p` / `--type pipe` | Pipes only |
| `-t c` / `--type char-device` | Character devices only |
| `-t b` / `--type block-device` | Block devices only |
| `-e EXT` / `--extension EXT` | Filter by extension (can be repeated: `-e rs -e toml`) |
| `-E GLOB` / `--exclude GLOB` | Exclude by glob pattern |

```bash
# Files OR directories
fd -t f -t d 'pattern'

# Multiple extensions
fd -e rs -e toml -e json 'pattern'

# Directories named "src", 2–4 levels deep
fd -t d --min-depth 2 --max-depth 4 --max-results 10 'src'

# Find files without extension
fd '^[^.]+$'

# Find empty directories
fd -t d -t e

# Find empty files
fd -t f -t e
```

## Hidden / Ignored Files

| Flag | Description |
|------|-------------|
| `-H` / `--hidden` | Search hidden files and directories |
| `-I` / `--no-ignore` | Do not respect `.gitignore` |
| `-u` / `--unrestricted` | Equivalent to `-HI` (hidden + no-ignore) |
| `--no-ignore-vcs` | Show VCS-ignored files (but still respect `.ignore`/`.fdignore`) |
| `--no-ignore-parent` | Don't read ignore files from parent directories |
| `--no-require-git` | Respect `.gitignore` even outside a git repo |
| `-E GLOB` / `--exclude GLOB` | Exclude files/directories matching glob |

## Symlink Handling

| Flag | Description |
|------|-------------|
| `-L` / `--follow` | Follow symbolic links and traverse linked directories |

By default, symlinks are treated as matches if they fit the pattern but are not traversed. Using `--follow` allows fd to enter directory symlinks. fd has built-in loop detection to prevent infinite recursion.

```bash
# Include symlink targets in the search
fd -L pattern
```

## Output Options

| Flag | Description |
|------|-------------|
| `-0` / `--print0` | Null-separated output (safe for `xargs -0`) |
| `-a` / `--absolute-path` | Show absolute paths |
| `-l` / `--list-details` | Long listing format like `ls -l` (shows permissions, size, etc.) |
| `-c WHEN` / `--color WHEN` | Color output: `always`, `auto`, `never` |
| `--hyperlink WHEN` | Add hyperlinks to output paths: `always`, `auto`, `never` |
| `--strip-cwd-prefix` | Strip the `./` prefix from results |
| `-q` / `--quiet` / `--has-results` | Don't print results (use exit code) |
| `--show-errors` | Display filesystem errors |
| `--path-separator SEP` | Custom path separator (default: OS-specific) |
| `--base-directory PATH` | Change working directory for search |
| `--search-path PATH` | Provide paths as alternative to positional argument |
| `--format FMT` | Custom output template using placeholders |

### Format Templates

The `--format` flag allows custom output using placeholders:

```bash
# Print just the basename
fd --format '{/}' -t f

# Print parent directory and basename
fd --format '{//}/{/}' -t f
```

## Command Execution

| Flag | Description |
|------|-------------|
| `-x CMD` / `--exec CMD` | Execute command for each result (parallel by default) |
| `-X CMD` / `--exec-batch CMD` | Execute command with all results at once |
| `--threads N` / `-j N` | Number of parallel jobs (for `-x`, default: CPU core count) |
| `--batch-size N` | Max arguments per `-X` invocation (0 = unlimited) |

**Placeholders for `-x`/`-X`:**

| Placeholder | Expands To | Example Input → Output |
|-------------|-----------|----------------------|
| `{}` | Full path | `docs/images/photo.jpg` |
| `{.}` | Path without extension | `docs/images/photo` |
| `{/}` | Basename | `photo.jpg` |
| `{//}` | Parent directory | `docs/images` |
| `{/.}` | Basename without extension | `photo` |

```bash
# Compute checksums (parallel, each result triggers one md5sum)
fd -tf -x md5sum > file_checksums.txt

# Remove found files (all at once with -X)
fd '^\.DS_Store$' -tf -X rm

# Convert images to PNG (sequential execution)
fd -e jpg -x convert {} {.}.png

# Open found files in editor
fd -e rs -X vim

# Sequential: rename with echo per file
fd -e txt --threads=1 -x mv {} {}.bak

# Batch size control (split large result sets)
fd -e log -X rm --batch-size 1000

# Multiple commands (run in order)
fd -e rs -x cmd1 {} \; -x cmd2 {}

# List details like ls -l
fd -e rs -l
```

**Gotcha:** `-x` runs in parallel by default. If command order matters, use `--threads=1` or `-j 1`.

## Examples

```bash
# Find all Rust files
fd -e rs

# Find files starting with "main"
fd '^main'

# Find exact filename (glob mode)
fd -g 'Cargo.toml'

# Find directories named "src"
fd -t d src

# Find recently modified files (last 7 days)
fd --changed-within 1w

# Find large files (> 10 MB)
fd -S +10M

# Find and delete .DS_Store files everywhere
fd -H '^\.DS_Store$' -tf -X rm

# Find and open in editor
fd -e rs -X vim

# Find and convert images
fd -e jpg -x convert {} {.}.png

# Find all test files
fd -g 'test_*.py'

# Search excluding node_modules
fd -E node_modules pattern

# Find empty directories
fd -t d -t e

# Find files modified in last hour
fd --changed-within '1 hour' -t f

# Pipe to xargs
fd -e rs -0 | xargs -0 rg "pattern"

# Find and show with bat (syntax-highlighted preview)
fd -e rs -X bat

# Complex multi-criteria search
fd -H -I --min-depth 2 --max-depth 4 \
   -e rs -e toml \
   --size +1k --size -100m \
   --changed-within 1w \
   --exclude target \
   --exclude '.git' \
   '^[a-z].*' \
   /home/user/projects

# Strip ./ prefix for cleaner output
fd --strip-cwd-prefix -e rs

# Show absolute paths
fd -e rs -a

# Quiet mode (exit code only)
fd -e rs -q && echo "Rust files found"

# Find files owned by specific user
fd -o username

# Find files with specific owner and group
fd -o username:groupname

# Search full path (not just filename)
fd -p '.*/src/.*\.rs$'

# Fixed string search (literal, no regex)
fd -F 'file[0-9].txt'

# Limit results
fd --max-results 10 -e rs

# Find single result
fd -1 -e rs

# Search specific directory with base-directory
fd --base-directory /home/user -e rs

# Use search-path instead of positional argument
fd -e rs --search-path /path/to/search

# Ignore directories containing a specific file
fd --ignore-contain 'package-lock.json' -e js

# Don't traverse into matching directories
fd --prune -t d node_modules

# Show filesystem errors
fd --show-errors /root

# Hyperlinks in supported terminals
fd --hyperlink always -e rs

# List details (like ls -l)
fd -e rs -l
```

## Ignore Files

fd respects (in order of precedence, later overrides earlier):
1. `.ignore` — generic ignore file (shared with ripgrep)
2. `.fdignore` — fd-specific ignore file
3. `.gitignore` — Git ignore rules
4. `.git/info/exclude` — Git's local exclude file
5. Global gitignore (`~/.config/git/ignore` via `core.excludesFile`)
6. Global fd ignore (`~/.config/fd/ignore` or `$XDG_CONFIG_HOME/fd/ignore`)

Use `-I` / `--no-ignore` to ignore all ignore files, or `-u` for hidden + no-ignore.

### Global Ignore File

Create `~/.config/fd/ignore` to ignore patterns globally:

```bash
# Always ignore .git directories even with --hidden
.git/

# Ignore common build directories
target/
node_modules/
__pycache__/
```

## Integration with Other Tools

```bash
# Search within found files using ripgrep
fd -e rs -X rg 'pattern'

# Fuzzy find with fzf (modern, fast file listing)
fd | fzf

# Preview with bat in fzf
fd | fzf --preview "bat --color=always --style=numbers --line-range=:500 {}"

# fzf with fd as default source
export FZF_DEFAULT_COMMAND='fd --type f --hidden --follow --exclude .git'
export FZF_DEFAULT_OPTS="--preview 'bat --style=numbers --color=always --line-range=:500 {}'"

# Open selected file in vim
fd | fzf | xargs -o vim

# Copy found files
fd -e rs -t f | xargs -I{} cp {} /backup/

# Find all Python files and count lines
fd -e py -X wc -l

# Search file contents within fd results
fd -e rs -X rg --color=always 'TODO' | bat -p

# Use fd with ripgrep for content search
fd -e rs -0 | xargs -0 rg "pattern"

# Chain fd with other tools
fd -e rs | xargs -I{} sh -c 'echo "Processing {}"; process_file {}'

# Use fd output with tar for backup
fd -e rs -0 | tar -czf backup.tar.gz --null -T -

# Find and chmod
fd -t f -x chmod 644 {}

# Find and git add
fd -e rs -X git add
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `LS_COLORS` | Determines color scheme for output |
| `NO_COLOR` | Disables colorized output |
| `XDG_CONFIG_HOME` | Location for global ignore file (`$XDG_CONFIG_HOME/fd/ignore`) |
| `HOME` | Fallback for global ignore file (`$HOME/.config/fd/ignore`) |

## Performance Notes

- fd is significantly faster than `find` for most operations
- It parallelizes directory traversal (use `-j N` to control)
- Respects `.gitignore` by default (skips `node_modules/`, `target/`, `.git/`, etc.)
- Use `-u` / `--unrestricted` only when you need to search everything
- Limit depth with `--max-depth` for faster results in deep trees
- Use `--quiet` for exit-code-only checks (faster than listing results)
- `--changed-within` and `--size` filters reduce I/O on large trees
- `-c never` disables color output for piping to tools that don't support it
- `--prune` can speed up searches by skipping matching directories entirely

## Common Pitfalls and Gotchas

1. **Not installed as `fd` on Debian/Ubuntu** — The package is `fd-find` and the binary is `fdfind`. Create a symlink: `ln -s $(which fdfind) ~/.local/bin/fd` or `alias fd=fdfind`.

2. **`fd` is case-insensitive by default (smart case)** — `fd Make` matches only `Make` (uppercase), but `fd make` matches `Makefile`, `makefile`, etc. Use `-s` to force case-sensitive or `-i` to force insensitive.

3. **`-x` is parallel by default** — If your command can't handle concurrent execution (e.g., appending to the same file), use `--threads=1` or `-j 1`.

4. **Symlinks not followed by default** — Use `-L` / `--follow` to traverse into linked directories. fd has loop detection, but it can still be slower with symlinks.

5. **Hidden files excluded by default** — `.env`, `.git/config`, etc. won't be searched. Use `-H` or `-u`.

6. **`fd pattern` matches filenames, not content** — To search file contents, pipe to ripgrep: `fd -e rs -X rg 'pattern'`.

7. **Glob mode (`-g`) uses shell patterns, not regex** — `fd -g '*.rs'` works, but `fd -g 'file[0-9].rs'` uses glob bracket expressions. For regex-like matching, use `fd 'file[0-9]'`.

8. **`--max-depth 0` doesn't work** — The shallowest meaningful depth is `--min-depth 1 --max-depth 1` for immediate children only.

9. **Large result sets can overwhelm** — Use `--max-results N` to cap output, or pipe to `head`.

10. **`.DS_Store` matching** — Use `fd '^\.DS_Store$'` with `-H` to find hidden dotfiles on macOS.

11. **Patterns starting with `-`** — Use `fd -- pattern` or `fd -e pattern` to search for patterns that look like flags.

12. **Regex special characters in shell** — Always quote patterns with special characters: `fd '[pattern]'` not `fd [pattern]`.

13. **`-t x` implies `-t f`** — The executable type filter automatically includes only files, so `fd -t x` won't show directories.

14. **`-t e` (empty) behavior** — Without additional type filter, it matches both empty files AND empty directories. Use `-t f -t e` or `-t d -t e` to be specific.

15. **Chaining `fd -X fd`** — This can explode the result set. Use `--prune` on the first fd or combine filters in a single command instead.
