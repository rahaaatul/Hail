# dust Skill

Visual disk usage analyzer — better alternative to `du` with intuitive treemap display. Written in Rust.

## When to Use

- Finding large files/directories
- Understanding disk usage visually
- Replacing `du -sh *`
- When you need to find what's eating disk space
- Comparing multiple directories
- Scripting disk usage analysis (JSON output)

## Basic Usage

```bash
dust                    # Show current directory
dust PATH               # Show specific path
dust PATH1 PATH2        # Compare multiple directories
dust -d 2               # Limit depth
dust -n 30              # Show 30 entries (default: terminal height)
```

## Common Patterns

### Display Options

| Flag | Description |
|------|-------------|
| `-d N` / `--depth N` | Max depth (default: 1) |
| `-n N` / `--number-of-lines N` | Number of entries to show |
| `-H` | Show hidden files (default: shown) |
| `-i` | Don't show hidden files |
| `-p` / `--full-paths` | Print full paths |
| `-r` / `--reverse` | Reverse sort (smallest first) |
| `-s` / `--apparent-size` | Apparent size (file length, not disk blocks) |
| `-b` / `--no-percent-bars` | No percentages or ASCII bars |
| `-B` / `--bars-on-right` | Percent bars on right side |
| `-c` / `--no-colors` | No colors (monochrome) |
| `-C` / `--force-colors` | Force colors |
| `--dim` | Dim percent bars (reduce brightness on dark terminals) |
| `-P` | Disable progress indicator |
| `-R` | Screen reader mode (adds depth column, removes bars) |
| `-w N` / `--terminal-width N` | Width of output |

### Filtering

| Flag | Description |
|------|-------------|
| `-e REGEX` / `--filter REGEX` | Only show files matching regex |
| `-v REGEX` / `--invert-filter REGEX` | Exclude files matching regex |
| `-z SIZE` / `--min-size SIZE` | Min size filter (e.g., 1M, 100K) |
| `-f` / `--file-types` | Group by file type |
| `-F` | Show only files (ignore directories) |
| `-D` | Show only directories (ignore files) |
| `-X EXCLUDE` / `--ignore-dir EXCLUDE` | Exclude directory by name |
| `-x` / `--limit-filesystem` | Only current filesystem (skip mounts) |
| `-L` / `--dereference-links` | Follow symlinks (treat as directories) |

### Output Formats

| Flag | Description |
|------|-------------|
| `-j` / `--json` | JSON output |
| `-o FORMAT` | Output format: `si` (powers of 1000), `b`, `kb`, `kib`, `mb`, `mib`, `gb`, `gib` |
| `--skip-total` | No total row |
| `--collapse NAME` | Keep directory collapsed (e.g., `--collapse=node-modules`) |
| `-S SIZE` / `--stack-size SIZE` | Custom stack size (fix stack overflow on huge dirs) |

### Input from File

| Flag | Description |
|------|-------------|
| `--files0-from=FILE` | Read NUL-terminated paths from FILE (`-` for stdin) |
| `--files-from=FILE` | Read newline-terminated paths from FILE (`-` for stdin) |

## Examples

```bash
# Basic usage
dust

# Show with depth 2
dust -d 2

# Show top 20 largest
dust -n 20

# Show hidden files (default behavior)
dust -H

# Hide hidden files
dust -i

# Filter by name (only .log files)
dust -e '\.log$'

# Exclude by name
dust -v '\.git$'

# Only files larger than 100MB
dust -z 100M

# Exclude node_modules
dust -X node_modules

# Show apparent size (file length, not blocks)
dust -s

# JSON output for scripting
dust -j | jq '.[] | select(.size > 1000000)'

# Show specific directory
dust ~/Downloads

# Reverse sort (smallest first)
dust -r

# Only current filesystem (skip mounts)
dust -x

# Show only files (find largest files)
dust -F -z 100M

# Show only directories
dust -D

# Group by file type
dust -t

# Full paths
dust -p

# Compare multiple directories
dust ~/projects ~/downloads

# Sizes in SI units (powers of 1000)
dust -o si

# No colors
dust -c

# Force colors (when piped)
dust -C | less -R

# Collapse specific directories
dust --collapse=.git --collapse=node_modules

# Read paths from file
dust --files-from=paths.txt

# Screen reader friendly
dust -R -p

# Fix stack overflow on huge directories
dust -S 1073741824 /

# Skip total row
dust --skip-total
```

## Understanding Output

```
  0B     ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒  node_modules
 4.0K    ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒  src
 4.0K    ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒  tests
 4.0M    ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒  target
```

- **Left**: Size (human-readable)
- **Middle**: Visual bar (proportional to size)
- **Right**: Directory/file name
- **Grey lines**: Show parent folder hierarchy (shadow indicates parent size)

## Configuration File

Config at `~/.config/dust/config.toml` or `~/.dust.toml`:

```toml
reverse = true
limit-filesystem = true
display-full-paths = true
display-apparent-size = true
no-colors = true
no-bars = true
skip-total = true
ignore-hidden = true
output-format = "si"
number-of-lines = 5
collapse = [".git", "node_modules"]
```

## Integration

```bash
# Find largest directories
dust -d 1 -n 10

# Find large files
dust -F -z 100M

# Combine with fzf
dust -j | jq -r '.[] | "\(.size) \(.name)"' | fzf

# Check specific large directories
dust -d 1 ~/.cache
dust -d 1 /tmp

# JSON processing with jq
dust -j | jq '[.[] | select(.size > 1e9)]'

# Find duplicate inodes (files with same inode)
dust -f -s

# Compare with jq for monitoring
dust -j /var/log | jq '.[] | {name, size}'
```

## Pitfalls and Gotchas

- **Apparent size vs disk usage**: `-s` shows file length (what `ls` shows), not actual disk blocks. Default shows disk usage (what `du` shows).
- **Hard links**: Dust does NOT count hard links multiple times by default. Use `-s` to count them.
- **`-n` is intelligent**: Unlike `head -n`, dust's `-n` shows the largest entries across the entire tree, not just the first N at the top level.
- **Permission errors**: Dust prints at most one "Did not have permissions" message and continues.
- **Stack overflow**: On extremely deep directory trees, you may see "fatal runtime error: stack overflow". Fix with `-S SIZE` (e.g., `-S 1073741824`).
- **Symlinks**: By default, dust does not follow symlinks. Use `-L` to dereference them.
- **Mount points**: Use `-x` to skip mounted filesystems (e.g., NFS, external drives).
- **Config file location**: Use `$XDG_CONFIG_HOME/dust/config.toml` or fall back to `~/.config/dust/config.toml`.
- **Snap limitation**: Dust installed via Snap can only access files in `/home`.
- **Not `du` replacement for scripting**: Dust is optimized for human viewing. Use `du` for portable scripts.

## Tips

- Use `-d 2` or `-d 3` for deeper analysis
- `-z 100M` helps find large files quickly
- `-X` excludes directories you don't care about (can be repeated)
- JSON output (`-j`) is useful for scripting with `jq`
- `--collapse` keeps specified directories expanded instead of recursing
- `-R` mode for screen readers adds a depth column
- `--dim` reduces brightness on dark terminals
- Use `-F -z 1M` to find all files larger than 1MB
- Use `-D -d 2` to see directory sizes at depth 2
- Combine with `jq` for filtering: `dust -j | jq '.[] | select(.size > 1e9) | .name'`
- Multiple directories: `dust ~/projects ~/downloads` compares them side-by-side
- Use `-o si` for SI units (powers of 1000) instead of binary (powers of 1024)
- `-t` groups output by file type for type-based analysis