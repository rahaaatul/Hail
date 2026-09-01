# delta Skill

Syntax-highlighting pager for git, diff, grep, and blame output. Makes diffs readable with syntax highlighting, word-level diffs, and side-by-side comparison.

## When to Use

- Viewing git diffs with syntax highlighting
- Replacing default `git diff` output
- Reviewing code changes
- Comparing files side-by-side
- When you need beautiful, readable diffs
- Viewing `git blame` output with highlighted commits
- Piping `ripgrep --json` or `git grep` output

## Basic Usage

```bash
delta FILE1 FILE2          # Show diff between files
git diff | delta            # Pipe git diff through delta
diff file1 file2 | delta    # Pipe any unified delta output
git log -p | delta          # Enhanced git log with diffs
git blame FILE | delta      # Enhanced blame output
rg --json 'pattern' | delta # Syntax-highlighted grep output
```

## Common Patterns

### Git Integration

```bash
# Set as default pager for git
git config --global core.pager delta
git config --global interactive.diffFilter 'delta --color-only'

# Or use with environment
export GIT_PAGER=delta
export DELTA_PAGER=less  # Use less as delta's internal pager
```

### Display Options

| Flag | Description |
|------|-------------|
| `-s` / `--side-by-side` | Show diffs side by side |
| `-w N` / `--width N` | Set side-by-side width |
| `--line-numbers` | Show line numbers |
| `--line-numbers-left-format FMT` | Format for left line numbers |
| `--line-numbers-right-format FMT` | Format for right line numbers |
| `--file-style STYLE` | File header style (plain, omit) |
| `--file-decoration-style STYLE` | File header decoration |
| `--file-label LABEL` | Label for file (side-by-side) |
| `--hunk-header-style STYLE` | Hunk header style |
| `--hunk-header-decoration-style STYLE` | Hunk header decoration |
| `--syntax-theme THEME` | Syntax theme (e.g., "GitHub", "Dracula") |
| `--plus-style STYLE` | Style for added lines |
| `--minus-style STYLE` | Style for removed lines |
| `--zero-style STYLE` | Style for unchanged lines |
| `--highlight-removed` | Highlight removed lines (default) |
| `--whitespace-error-style STYLE` | Style for whitespace errors |

### Navigation

| Flag | Description |
|------|-------------|
| `--navigate` | Enable n/N navigation between diff sections |
| `--paging MODE` | Paging mode: auto, always, never |
| `--no-paging` | Disable paging |
| `--pager CMD` | External pager command |

### Colors and Style

| Flag | Description |
|------|-------------|
| `--dark` | Use dark theme |
| `--light` | Use light theme |
| `--true-color MODE` | True color: always, never, auto |
| `--color-only` | Only colorize, no structural changes |
| `--diff-highlight` | Emulate diff-highlight |
| `--diff-so-fancy` | Emulate diff-so-fancy |
| `--features FEATURES` | Enable features (e.g., "line-numbers side-by-side") |

### Hyperlinks

| Flag | Description |
|------|-------------|
| `--hyperlinks` | Format commits/files as terminal hyperlinks |
| `--hyperlinks-file-link-format FMT` | Format for file links (e.g., `vscode://file/{path}:{line}`) |
| `--hyperlinks-commit-link-format FMT` | Format for commit links |

### Word-Level Diff

| Flag | Description |
|------|-------------|
| `--word-diff-regex REGEX` | Regex for word boundaries |
| `--max-line-distance FST` | Max line distance for word diff |
| `--wrap-max-lines N` | Max wrapped lines |
| `--wrap-right-percent PCT` | Right column percentage for wrapping |

## Examples

```bash
# Side-by-side diff
delta -s file1.rs file2.rs

# With line numbers
delta --line-numbers file1.rs file2.rs

# Custom theme
delta --syntax-theme "Solarized (dark)" file1.rs file2.rs

# Git diff with delta
git diff | delta --side-by-side

# Show only changes, no context
delta --context-min-color(Some Color) file1.rs file2.rs

# Word-level diff
delta --word-diff-regex '\w+' file1.rs file2.rs

# Hyperlinks for file paths (VS Code)
delta --hyperlinks --hyperlinks-file-link-format "vscode://file/{path}:{line}"

# Compare with whitespace ignored
git diff -w | delta

# Navigate between files in large diff
git diff HEAD~5 | delta --navigate

# Preview all syntax themes
delta --show-syntax-themes --dark
delta --show-syntax-themes --light

# Git blame with delta
git blame src/main.rs | delta

# Ripgrep output with syntax highlighting
rg --json 'fn main' | delta

# Override config for single command
git -c delta.side-by-side=true diff
git -c delta.features='' diff
```

## Git Configuration

Add to `~/.gitconfig`:

```ini
[core]
    pager = delta

[interactive]
    diffFilter = delta --color-only

[delta]
    navigate = true
    light = false
    side-by-side = true
    line-numbers = true
    syntax-theme = Dracula
    # features = side-by-side line-numbers decorations

[delta "decorations"]
    commit-decoration-style = bold yellow box ul
    file-style = bold yellow ul
    file-decoration-style = none
    hunk-header-decoration-style = cyan box ul

[delta "line-numbers"]
    line-numbers-left-style = cyan
    line-numbers-right-style = cyan
    line-numbers-minus-style = 124
    line-numbers-plus-style = 28

[merge]
    conflictstyle = zdiff3

[diff]
    colorMoved = default
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `GIT_PAGER` | Set delta as git pager |
| `DELTA_PAGER` | Delta's internal pager (preferred over PAGER) |
| `DELTA_FEATURES` | Enable features (e.g., "line-numbers side-by-side") |
| `BAT_THEME` | Fallback syntax theme |
| `NO_COLOR` | Disable all colors |

## Integration

```bash
# With lazygit (uses delta automatically if configured)
lazygit

# With ripgrep
rg --json 'pattern' | delta

# With hyperfine
hyperfine --show-output 'cmd1' 'cmd2' 2>&1 | delta

# With git log
git log --oneline -p | delta --navigate

# With diff-so-fancy emulation
git diff | delta --diff-so-fancy

# With diff-highlight emulation
git diff | delta --diff-highlight
```

## Pitfalls and Gotchas

- **PAGER env var conflict**: If `PAGER` contains complex shell commands (e.g., in Cursor IDE), delta may break. Use `DELTA_PAGER` instead or unset `PAGER`.
- **Large diffs**: Delta adds slight latency compared to raw `git diff` due to syntax highlighting and word-diff computation.
- **Side-by-side width**: Requires wide terminals. Use `-w N` to control width or disable for narrow terminals.
- **Theme compatibility**: Some themes look poor on certain terminal backgrounds. Test with `--show-syntax-themes` first.
- **Piping to other commands**: When piping `git diff` to other tools, delta may interfere. Use `git --no-pager diff` to bypass.
- **Color moved support**: Enable `colorMoved = default` in `[diff]` section for Git's `--color-moved` feature.
- **Copy-paste friendly**: Delta removes `+`/`-` markers by default, making output safe to copy.
- **Syntax detection**: Based on file extensions. May misidentify files with unusual extensions.

## Tips

- Delta automatically detects terminal background (dark/light)
- Use `--side-by-side` for wide terminals (>120 cols)
- Combine with `git diff --staged` for reviewing staged changes
- Use `delta --diff-so-fancy` for a different visual style
- Enable `--navigate` for browsing large multi-file diffs with `n`/`N`
- Use `--hyperlinks` with editor-specific formats for clickable file paths
- All bat syntax themes work with delta
