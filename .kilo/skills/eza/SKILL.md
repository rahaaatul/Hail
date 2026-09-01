# eza Skill

Modern, maintained replacement for `ls` written in Rust. Colorful, featureful directory listing with Git integration, extended attributes, icons, hyperlinks, and tree views.

## When to Use

- Listing files and directories (drop-in for `ls`)
- Viewing file metadata (permissions, size, owner, dates, inodes, xattrs)
- Visualizing Git status at a glance (per-file or per-repo)
- Tree-view of directory structure (with depth, gitignore respect)
- Color/age/size scaled listings to spot outliers
- Anything you used `ls`, `tree`, or the abandoned `exa` for

## Basic Usage

```bash
eza                      # Grid (default), colors on
eza -l                   # Long listing
eza -T                   # Tree view
eza -T -L 3              # Tree limited to 3 levels
eza PATH                 # List specific directory or file
```

## Display Options

| Flag | Description |
|------|-------------|
| `-1`, `--oneline` | One entry per line |
| `-G`, `--grid` | Grid (default) |
| `-x`, `--across` | Grid sorted across then down |
| `-l`, `--long` | Extended details and attributes |
| `-R`, `--recurse` | Recurse into directories |
| `-T`, `--tree` | Recurse as a tree |
| `-F`, `--classify=WHEN` | Type indicator after names (`always`/`auto`/`never`) |
| `--color=WHEN` | When to color (`always`/`auto`/`never`) |
| `--color-scale=FIELD` | Highlight levels distinctly (`all`/`age`/`size`) |
| `--color-scale-mode=MODE` | `fixed` or `gradient` (with `--color-scale`) |
| `--icons=WHEN` | Show file type icons (`always`/`auto`/`never`) |
| `--hyperlink=WHEN` | Show entries as terminal hyperlinks |
| `--absolute=MODE` | Print absolute paths (`on`/`follow`/`off`) |
| `-w`, `--width=COLS` | Set screen width |

## Filtering & Sorting

| Flag | Description |
|------|-------------|
| `-a`, `--all` | Show hidden and dotfiles |
| `-aa` | Also show `.` and `..` |
| `-d`, `--list-dirs` | Treat directories as files (no recursion) |
| `-D`, `--only-dirs` | List only directories |
| `-f`, `--only-files` | List only files |
| `-L`, `--level=N` | Limit recursion depth |
| `-r`, `--reverse` | Reverse sort order |
| `-s`, `--sort=FIELD` | Sort field (see below) |
| `--group-directories-first` / `--group-directories-last` | Group ordering |
| `--git-ignore` | Respect `.gitignore` |
| `-I`, `--ignore-glob=GLOB` | Pipe-separated ignore globs (e.g. `*.o\|node_modules`) |
| `--no-symlinks` / `--show-symlinks` | Control symlink visibility |

Valid `--sort` fields: `accessed`, `changed`, `created`, `extension`, `Extension`, `inode`, `modified`, `name`, `Name`, `size`, `type`, `none`. Aliases: `modified`=`date`/`time`/`newest`; reverse has `age`/`oldest`. Capital letters sort uppercase before lowercase.

## Long-View Options (`-l`)

| Flag | Description |
|------|-------------|
| `-b`, `--binary` | Binary size prefixes (KiB, MiB) |
| `-B`, `--bytes` | Size in bytes only |
| `-g`, `--group` | Show group |
| `--smart-group` | Show group only if different from owner |
| `-h`, `--header` | Add column header row |
| `-H`, `--links` | Show hard-link count |
| `-i`, `--inode` | Show inode number |
| `-m`, `--modified` | Use modified timestamp (default) |
| `-M`, `--mounts` | Show mount details (Linux/macOS) |
| `-S`, `--blocksize` | Show allocated block size |
| `-t`, `--time=FIELD` | Timestamp field (`modified`/`changed`/`accessed`/`created`) |
| `-u`, `--accessed` | Use accessed timestamp |
| `-U`, `--created` | Use created timestamp |
| `--changed` | Use changed timestamp |
| `-X`, `--dereference` | Dereference symlinks for metadata |
| `-Z`, `--context` | SELinux security context |
| `-@`, `--extended` | Show extended attributes and sizes |
| `-o`, `--octal-permissions` | Octal permission mode |
| `--no-permissions` / `--no-filesize` / `--no-user` / `--no-time` | Suppress fields |
| `--total-size` | Show recursive directory size |
| `--git` | Git status per file |
| `--git-repos` | Git status per directory |
| `--git-repos-no-status` | Just whether dir is a repo (faster) |
| `--no-git` | Force-off Git (overrides `--git`/`--git-repos`) |
| `--time-style=STYLE` | `default`/`iso`/`long-iso`/`full-iso`/`relative`, or `+%Y-%m-%d %H:%M` |
| `--stdin` | Read filenames from stdin |

## Examples

```bash
# Long + hidden + header + git
eza -lha --git

# Tree, respecting gitignore, 2 levels, with icons
eza -T -L 2 --git-ignore --icons

# Largest items at top (size-scaled)
eza -l --color-scale=size -s size -r | head -20

# Recently modified files (newest first)
eza -l -s modified -r

# Only directories, grouped first
eza -D --group-directories-first

# Octal permissions + extended attributes
eza -lo -@

# Absolute paths with hyperlinks
eza -l --absolute=on --hyperlink

# Custom timestamp format
eza -l --time-style='+%Y-%m-%d %H:%M'

# Read filenames from another command (e.g., git)
git ls-files | eza --stdin -l --git
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `EZA_CONFIG_DIR` | Directory holding `theme.yml` |
| `EZA_GRID_ROWS` | Override grid-row computation |
| `EZA_ICON_SPACING` | Spaces between icon and name |
| `EZA_OVERRIDE_GIT` | Force-on Git output even when not a repo |
| `EZA_WINDOWS_ATTRIBUTES` | `short`/`long` for file-attribute column |
| `EZA_STRICT_ARGS` | Strict argument checking |
| `NO_COLOR` | Disable colors (overrides `--color=always`) |
| `LS_COLORS`, `EXA_COLORS` | Backwards-compat color overrides |

## Themes

Custom theme at `$EZA_CONFIG_DIR/theme.yml` (default `~/.config/eza/theme.yml`):

```yaml
filetypes:
  directory:
    foreground: blue
    style: bold
  executable:
    foreground: green
    style: bold
  symlink:
    foreground: cyan
extensions:
  rs:
    foreground: red
  md:
    foreground: yellow
colour:
  when: auto
  theme: default
```

`LS_COLORS` / `EXA_COLORS` env vars still work and take precedence.

## Integration Examples

```bash
# Pipe to fzf for selection
eza -1 | fzf

# Preview with fzf + bat
eza -1 | fzf --preview "bat --color=always {}"

# Find large files quickly
eza -l --sort=size -r | head -20

# Snapshot a project layout
eza -T --git-ignore --icons -L 3 > tree.txt

# Show only recently changed tracked files
git diff --name-only HEAD~5 | eza --stdin -l --git
```

## Recommended Aliases

```bash
alias ls='eza'
alias ll='eza -l'
alias la='eza -la'
alias lt='eza -T --level=2 --git-ignore'
alias llt='eza -lT --level=3 --git-ignore'
alias llg='eza -l --git'
alias lls='eza -l --sort=size -r'
alias llm='eza -l --sort=modified -r'
```

## Common Pitfalls

- **Migrating from `exa`**: `exa` is unmaintained; eza is its successor. Most flags carry over (`-l`, `-T`, `--git`, `--icons`), but eza added `--absolute`, `--color-scale`, `--total-size`, and stricter sort-field names.
- **`-h` is header, not "human-readable"**: unlike `ls`, eza's binary/bytes flags are `-b`/`-B`. Sizes are human-readable by default in long view.
- **`--git` only works inside a repo**: outside one, you'll see nothing extra. Use `--git-repos` to show dir-level status without `--git`.
- **Tree on huge dirs is slow**: always pair `-T` with `-L` and/or `--git-ignore`.
- **Glob ignore is pipe-separated**, not comma: `-I '*.o|target|node_modules'`.
- **Pipe filenames in via `--stdin`**, not positional args, when chaining from `git ls-files`, `fd`, etc.
- **Icons need a Nerd Font**: without one, expect tofu boxes. Use `--icons=never` if not installed.