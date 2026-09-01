# bat Skill

Modern replacement for `cat` — syntax highlighting, Git integration, and automatic paging.
Latest: v0.26.x. Binary on some distros is `batcat` (see Pitfalls).

## When to Use

- Viewing code files with syntax highlighting
- Reading config files with Git change indicators
- Concatenating files (falls back to plain `cat` behavior when piped to a file or process)
- Piping output with automatic paging
- Highlighting specific lines or line ranges
- Monitoring logs with syntax highlighting

## Installation Quick Reference

| Platform | Command |
|----------|---------|
| Ubuntu/Debian | `sudo apt install bat` |
| macOS (Homebrew) | `brew install bat` |
| Fedora | `dnf install bat` |
| Arch | `pacman -S bat` |
| Windows (WinGet) | `winget install sharkdp.bat` |
| Windows (Scoop) | `scoop install bat` |
| From source | `cargo install --locked bat` |

## Basic Usage

```bash
bat FILE                    # View file with syntax highlighting (pages if too long)
bat FILE1 FILE2             # View multiple files
bat -n FILE                 # Show line numbers only
bat -p FILE                 # Plain output: no decorations, no colors
bat -pp FILE                # Plain output AND never page (alias for --plain --paging=never)
bat - FILE                  # Read from stdin
bat                         # Read from stdin (no args)
```

## Flags Reference

### Display / Style

| Flag | Description |
|------|-------------|
| `--style=<components>` | Comma-separated style components. Presets: `default`, `full`, `auto`, `plain`. Components: `changes`, `grid`, `header`/`header-filename`/`header-filesize`, `numbers`, `rule`, `snip`. Prefix with `+`/`-` to add/remove from config default. |
| `-n` / `--number` | Show line numbers only (alias for `--style=numbers`) |
| `-b` / `--number-nonblank` | Line numbers for non-blank lines only |
| `-A` / `--show-all` | Show non-printable characters |
| `--nonprintable-notation <notation>` | `unicode` (␇, ␊) or `caret` (^G, ^J) |
| `--decorations <when>` | `auto` (default), `never`, `always` — controls whether decorations show when piped |
| `-f` / `--force-colorization` | Alias for `--decorations=always --color=always` — keeps colors/decorations when piping |
| `--color <when>` | `auto` (default), `never`, `always` |
| `--italic-text <when>` | `always`, `never` (default) |

**Default style components:** `changes`, `grid`, `header-filename`, `numbers`, `snip`

### Paging & Pager

| Flag | Description |
|------|-------------|
| `--paging <when>` | `auto` (default), `never`, `always`. Use `-P` as short for `--paging=never`. |
| `-P` | Short for `--paging=never` |
| `--pager <command>` | Override pager (default: `less`, or `PAGER`/`BAT_PAGER` env vars). Use `--pager=builtin` for bat's built-in `minus` pager. |

### Line Range & Highlighting

| Flag | Description |
|------|-------------|
| `-H N` / `--highlight-line N` | Highlight line(s). Supports ranges: `40`, `30:40`, `:40`, `40:`, `30:+10` |
| `-r N:M` / `--line-range N:M` | Print only lines N to M. Supports: `30:40`, `:40`, `40:`, `40`, `-10:` (last 10), `30:+10`, `30:40:2` (context) |

### Language / Syntax

| Flag | Description |
|------|-------------|
| `-l LANG` / `--language LANG` | Force language (name or extension, e.g. `-l json`, `-l rs`) |
| `-L` / `--list-languages` | List all supported languages |
| `--fallback-syntax LANG` | Fallback when auto-detection fails (alias: `--fallback-language`) |
| `-m PATTERN:SYN` / `--map-syntax` | Map glob pattern to a syntax name (not extension) |
| `--ignored-suffix SUFFIX` | Ignore file suffix (e.g. `.dev` so `.json.dev` uses JSON) |
| `--list-themes` | List all available themes |

### Diff / Git

| Flag | Description |
|------|-------------|
| `-d` / `--diff` | Show only changed lines relative to Git index |
| `--diff-context N` | Lines of context around diff changes |

### Text Formatting

| Flag | Description |
|------|-------------|
| `--wrap <mode>` | `auto` (default), `never`, `character`, `word` |
| `-S` / `--chop-long-lines` | Truncate lines longer than terminal width (alias for `--wrap=never`) |
| `--terminal-width <width>` | Override terminal width. Use `+N`/`-N` as offset. Env: `BAT_WIDTH` |
| `--tabs <T>` | Tab width in spaces (default 4). Use `0` to let pager handle tabs. |
| `-s` / `--squeeze-blank` | Squeeze consecutive empty lines into one |
| `--squeeze-limit <N>` | Max consecutive empty lines |

### Input Safety

| Flag | Description |
|------|-------------|
| `--strip-ansi <when>` | `auto` (default), `always`, `never` — strip ANSI escape sequences from input |
| `--sanitize <when>` | `auto` (default), `always`, `never` — replaces terminal-active control bytes and Unicode bidi/zero-width chars with U+FFFD. Implies `--strip-ansi`. |
| `--binary <behavior>` | `no-printing` (default), `as-text` |
| `-E` / `--quiet-empty` | No output at all when input is empty (useful for empty `git diff`) |

### Other

| Flag | Description |
|------|-------------|
| `--file-name NAME` | Display name for file (use with stdin; also affects syntax detection) |
| `-u` / `--unbuffered` | Display data as soon as available (for `tail -f | bat`). Disables line numbers. |
| `--set-terminal-title` | Set terminal title to filenames |
| `--completion <SHELL>` | Generate shell completions (bash, fish, zsh, ps1) |
| `--diagnostic` | Show diagnostic info for bug reports |
| `--acknowledgements` | Show acknowledgements |
| `--config-file` | Print path to config file |
| `--config-dir` | Print config directory |
| `--generate-config-file` | Generate a default config file |

## Default Behavior: Auto-Detection

`bat` checks if stdout is a pipe/file:
- **Interactive terminal**: full decorations, syntax highlighting, paging
- **Piped/redirected**: falls back to plain `cat` behavior (no decorations, no paging), regardless of flags

This means `bat file > output.txt` produces clean output automatically. To force decorations/colors when piping, use `--decorations=always --color=always` or `-f`.

## Configuration

### Config File Locations

```bash
bat --config-file        # Show user config path
bat --config-dir          # Show config directory
bat --generate-config-file  # Create default config file
```

Default locations:
- Linux: `~/.config/bat/config`
- macOS: `~/Library/Preferences/bat/config` or `~/.config/bat/config`
- Windows: `%APPDATA%\bat\config`
- System-wide: `/etc/bat/config` (Linux/macOS) or `C:\ProgramData\bat\config` (Windows)

Custom location via env vars:
```bash
export BAT_CONFIG_PATH="/path/to/bat.conf"
export BAT_CONFIG_DIR="/path/to/bat"
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `BAT_CONFIG_PATH` | Path to config file |
| `BAT_CONFIG_DIR` | Config directory |
| `BAT_THEME` | Default theme |
| `BAT_THEME_DARK` | Theme for dark backgrounds |
| `BAT_THEME_LIGHT` | Theme for light backgrounds |
| `BAT_STYLE` | Default style components |
| `BAT_PAGER` | Override pager command |
| `BAT_PAGING` | Override paging behavior |
| `BAT_WIDTH` | Terminal width |
| `PAGER` | Fallback pager (if `BAT_PAGER` unset) |
| `COLORTERM` | Must be `truecolor` or `24bit` for 24-bit color themes |

### Sample Config File

```bash
# Theme
--theme="TwoDark"

# Style: line numbers + Git changes + header (no grid, no snip)
--style="numbers,changes,header"

# Enable italic (terminal-dependent)
--italic-text=always

# Map .ino files to C++ syntax
--map-syntax "*.ino:C++"

# Ignore .dev suffix
--ignored-suffix ".dev"
```

### Syntax & Theme Cache

```bash
# Add custom syntaxes
mkdir -p "$(bat --config-dir)/syntaxes"
cd "$(bat --config-dir)/syntaxes"
git clone https://github.com/tellnobody1/sublime-purescript-syntax
bat cache --build

# Add custom themes
mkdir -p "$(bat --config-dir)/themes"
cd "$(bat --config-dir)/themes"
git clone https://github.com/greggb/sublime-snazzy
bat cache --build

# Reset to defaults
bat cache --clear
```

### Dark/Light Mode Auto-Switch

```bash
# macOS dark mode detection
alias cat='bat --theme auto:system --theme-dark default --theme-light GitHub'

# GNOME dark mode detection
sys_color_scheme_is_dark() {
    condition=$(gsettings get org.gnome.desktop.interface color-scheme)
    condition=$(echo "$condition" | tr -d "[:space:]'")
    [ "$condition" == "prefer-dark" ]
}
bat_alias_wrapper() {
    if sys_color_scheme_is_dark; then
        bat --theme=default "$@"
    else
        bat --theme=GitHub "$@"
    fi
}
alias cat='bat_alias_wrapper'
```

## Examples

```bash
# View a file (auto-paging)
bat main.rs

# Show line numbers only
bat -n main.rs

# Show non-blank line numbers only
bat -b main.rs

# Show specific line range
bat --line-range 10:20 main.rs

# Show last 10 lines
bat --line-range -10: main.rs

# Highlight specific lines
bat --highlight-line 42 main.rs
bat --highlight-line 30:40 main.rs

# View multiple files
bat src/*.rs

# Show non-printable characters (caret notation)
bat -A --nonprintable-notation caret /etc/hosts

# Show non-printable (unicode notation)
bat -A --nonprintable-notation unicode /etc/hosts

# Plain mode (no decorations, no paging)
bat -p main.rs
bat -pp main.rs   # -pp also disables paging

# Force colors when piping to another tool
bat -f main.rs | other_command

# Strip ANSI sequences from input file
bat --strip-ansi=always output.log

# Sanitize untrusted input
bat --sanitize=always suspicious.txt

# Squeeze blank lines
bat -s logfile.log

# View file with custom width offset
bat --terminal-width -20 main.rs

# Quiet empty (e.g., empty git diff)
git diff | bat --diff

# Unbuffered streaming (tail -f)
tail -f /var/log/syslog | bat -u --paging=never -l log

# Map syntax by glob pattern
bat --map-syntax '*.conf:INI' /etc/some.conf

# Ignore suffix for language detection
bat --ignored-suffix .dev config.json.dev

# Generate shell completions
bat --completion bash > /etc/bash_completion.d/bat
```

## Integration with Other Tools

### fzf (Preview)

```bash
# Basic preview
fzf --preview "bat --color=always --style=numbers --line-range=:500 {}"

# Preview with line highlighting
fzf --delimiter : --preview 'bat --color=always --highlight-line {2} {1}'

# Theme preview
bat --list-themes | fzf --preview="bat --theme={} --color=always /path/to/file"
```

### fd (Find)

```bash
# Preview all found files
fd -e rs -X bat

# Preview with line numbers
fd -e py -X bat -n
```

### ripgrep (batgrep)

```bash
# Search with highlighted output
batgrep 'pattern' src/

# Combine ripgrep with bat
rg -C 5 'pattern' --color=always | bat
```

### git

```bash
# View old version of file
git show v1.0:src/main.rs | bat -l rs

# View diff with highlighting
batdiff() {
    git diff --name-only --relative --diff-filter=d -z | xargs -0 bat --diff
}

# Show changed files
git diff --name-only | xargs bat

# Show diff with context
bat --diff --diff-context=5 --style=changes file.rs
```

### tail (Live Monitoring)

```bash
# Monitor log with highlighting
tail -f /var/log/syslog | bat --paging=never -l log

# Unbuffered live monitoring
tail -f /var/log/app.log | bat -u --paging=never -l log
```

### man pages

```bash
# Colorized man pages
export MANPAGER="bat -plman"
man 2 select
```

### Help text

```bash
# Colorize --help output
cp --help | bat -pl help

# Helper function for any command's help
bathelp() { "$1" --help 2>&1 | bat --plain --language=help; }

# zsh global alias
alias -g -- -h='-h 2>&1 | bat --language=help --style=plain'
alias -g -- --help='--help 2>&1 | bat --language=help --style=plain'

# fish abbreviation
abbr -a --position anywhere -- --help '--help | bat -plhelp'
```

## Common Pitfalls

1. **`batcat` vs `bat` on Ubuntu/Debian** — Older releases install the binary as `batcat` due to a name clash. Fix: `ln -s /usr/bin/batcat ~/.local/bin/bat` or `alias bat=batcat`.

2. **Tabs are expanded by bat, not the pager** — bat expands tabs to 4 spaces by default. Tab stops set via `LESS` or `--pager` are ignored. Use `--tabs=0` to let the pager handle tabs.

3. **Language from stdin needs `-l`** — When piping stdin, bat can only detect language from shebang lines. Use `-l LANG` to force it: `echo '{"key":"val"}' | bat -l json`.

4. **Color themes need `COLORTERM=truecolor`** — bat falls back to 8-bit colors if it can't detect truecolor support. Ensure your terminal sets `COLORTERM`.

5. **less options auto-added but fragile** — bat auto-adds `-R`, `-F`, `-K` to `less`. These are skipped if you pass `--pager="less -R"` (with args) or if `BAT_PAGER` contains args. For full control, set `BAT_PAGER="less -RFK"`.

6. **File encodings** — bat supports UTF-8 and UTF-16 natively. For other encodings (e.g., Latin-1), convert first: `iconv -f ISO-8859-1 -t UTF-8 file.php | bat`.

7. **Garbled output from ANSI in files** — Files containing ANSI escape sequences need `--strip-ansi=always` or `--sanitize=always` to avoid garbled output.

8. **`-pp` disables paging** — Using `-p` once gives plain output but may still page. Use `-pp` (or `--style=plain --paging=never`) to disable both decorations and paging.

9. **8-bit terminal themes** — On terminals without truecolor, use `ansi`, `base16`, or `base16-256` themes for best results.

## Aliases

```bash
# bat as cat (no paging)
alias cat='bat --paging=never'

# bat with line numbers
alias catn='bat -n'

# Plain cat behavior
alias catp='bat -p'
```

## Notes

- bat detects language from file extension or shebang line
- `--style` component prefixes: `+grid` adds, `-grid` removes, relative to config/env default
- Default style: `changes`, `grid`, `header-filename`, `numbers`, `snip`
- Popular themes: `TwoDark`, `Monokai Extended`, `Solarized (dark)`, `Solarized (light)`, `GitHub`, `Dracula`, `Nord`, `OneHalfDark`, `ansi`, `base16`
- `bat cache --build` compiles custom syntaxes/themes into a binary cache for faster startup
