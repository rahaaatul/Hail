# glow Skill

Terminal Markdown renderer — beautifully display Markdown files in CLI or interactive TUI mode.

## When to Use

- Reading Markdown files in terminal with full formatting
- Browsing documentation directories interactively
- Previewing README files, CHANGELOGs, docs
- Replacing `cat README.md` with styled output
- Fetching and rendering GitHub/GitLab READMEs
- Piping Markdown from curl or other tools

## Two Modes

**CLI Mode** (`glow FILE.md`): Renders one file to stdout. Non-interactive, pipe-friendly.
**TUI Mode** (`glow` with no args): Interactive file browser for Markdown files in current directory and subdirectories.

## CLI Mode Flags

| Flag | Description |
|------|-------------|
| `-s STYLE` | Style: `dark`, `light`, `auto`, or path to JSON stylesheet |
| `-w N` | Word-wrap width (0 = no wrap, default = terminal width, max 120) |
| `-p` / `--pager` | Use pager (default: `less -r` or `$PAGER`) |
| `-t` / `--tui` | Force TUI mode |
| `-n` / `--preserve-new-lines` | Preserve source line breaks |
| `--all` | Show hidden/ignored files in TUI |
| `--mouse` | Enable mouse wheel in TUI |
| `--width N` | Alias for `-w` |
| `-` | Read from stdin |

## TUI Mode Navigation

| Key | Action |
|-----|--------|
| `↑`/`↓` or `j`/`k` | Navigate file list / document |
| `Enter` | Open file |
| `g` / `G` | Jump to start/end of document |
| `b` / `f` | Page backward/forward |
| `/` | Fuzzy filter files |
| `?` | Help |
| `q` | Quit |
| `e` | Edit file in `$EDITOR` |
| `p` | Toggle pager |
| `?` | Show hotkeys |

TUI respects `.gitignore`. Use `--all` to include hidden/ignored files.
TUI auto-watches files for changes (since v2.1.0).

## Styles

Built-in: `dark`, `light`, `auto` (default, detects terminal background).

Custom themes: JSON stylesheets based on Glamour format. Place in config dir or pass path:
```bash
glow -s /path/to/custom.json README.md
```

Community styles: Tokyo Night, Catppuccin, Dracula, etc. Browse Glamour styles gallery.

## Configuration

```bash
glow config    # Opens config in $EDITOR
```

Config file: `glow.yml` (platform-specific location, check `glow --help`).

```yaml
style: "auto"          # dark, light, auto, or JSON path
width: 0               # 0 = auto-detect, else wrap width
pager: false           # true = always use pager in CLI
tui: false             # true = start in TUI by default
all: false             # show hidden/ignored in TUI
showLineNumbers: false # TUI line numbers (v2.1.2+)
mouse: true            # mouse wheel in TUI
preserveNewLines: false # keep source line breaks
```

## Examples

```bash
# Render a file
glow README.md

# TUI mode (browse directory)
glow

# Force TUI for single file
glow --tui README.md

# Pager mode
glow -p README.md

# Light style
glow -s light README.md

# Custom width
glow -w 80 README.md

# No wrap
glow -w 0 README.md

# Preserve line breaks
glow --preserve-new-lines notes.md

# Read from stdin
cat README.md | glow -
curl -s URL | glow -

# Remote README (GitHub)
glow github.com/user/repo
glow https://github.com/user/repo/blob/main/README.md

# GitLab
glow gitlab.com/user/repo

# HTTP URL
glow https://example.com/file.md

# Show all files in TUI
glow --all

# Custom stylesheet
glow -s ~/.config/glow/custom.json README.md

# Edit config
glow config
```

## Supported Markdown

- Headings (H1–H6), emphasis, strikethrough
- Ordered/unordered lists, task lists
- Fenced code blocks with syntax highlighting
- Blockquotes, horizontal rules
- Tables
- Links and images (rendered as terminal links)
- Emoji
- Limited HTML

## Common Pitfalls

- `glow` with no arguments starts TUI browser, not single-file render. Use `glow FILE.md` for direct rendering.
- Remote URLs need network access; local files work offline.
- `$PAGER` with spaces/quotes works since v2.1.2 (fixed parsing)
- Old stash feature was removed in v2.0 — don't follow old tutorials mentioning it
- Very large files (> several MB): use `--pager` or CLI mode, not TUI
- Colors/symbols corruption: check UTF-8 and 256-color support in terminal/tmux

## Integration

```bash
# fzf preview
fzf --preview "glow --style dark {}"

# Pipe from curl
curl -s https://raw.githubusercontent.com/user/repo/main/README.md | glow -w 80

# As PAGER for man pages (markdown only)
export PAGER="glow -p"

# Preview in git log
git log --format=medium -- README.md | glow -

# Browse docs directory
glow ~/projects/*/docs/
```

## Tips

- Use `--tui` when you want Glow's built-in pager and interactive navigation for a single file
- `-w 80` for predictable wrapping in narrow SSH sessions
- `--preserve-new-lines` for notes, lyrics, manually formatted lists
- `glow config` is the easiest way to set persistent preferences
- Works great with `bat` — use `glow` for Markdown, `bat` for code
