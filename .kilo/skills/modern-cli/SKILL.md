# Modern CLI Tools Overview

Quick reference for modern alternatives to classic Unix commands. Use these tools for a faster, more productive terminal experience.

## The Modern CLI Toolkit

| Classic | Modern | Install | Skill |
|---------|--------|---------|-------|
| `cat` | **bat** | `bat` | Use `bat` for syntax highlighting |
| `ls` | **eza** | `eza` | Use `eza` for colors, icons, git status |
| `find` | **fd** | `fd-find` | Use `fd` for fast, intuitive file search |
| `grep` | **ripgrep` (rg) | `ripgrep` | Use `rg` for fast, git-aware search |
| `sed` | **sd** | `sd` | Use `sd` for readable find-and-replace |
| `du` | **dust** | `dust` | Use `dust` for visual disk usage |
| `df` | **duf** | `duf` | Use `duf` for readable disk free |
| `ps` | **procs** | `procs` | Use `procs` for colorful process list |
| `top`/`htop` | **btop** | `btop` | Use `btop` for beautiful system monitor |
| `cd` | **zoxide** | `zoxide` | Use `z` for smart directory jumping |
| `diff` | **delta** | `delta` | Use `delta` for syntax-highlighted diffs |
| `hexdump` | **hexyl** | `hexyl` | Use `hexyl` for colorful hex viewing |
| `jq` | **fx** | `fx` | Use `fx` for interactive JSON viewing |
| `jq` (grep) | **gron** | `gron` | Use `gron` to make JSON greppable |
| `less` | **glow** | `glow` | Use `glow` for Markdown rendering |
| `fuzzy` | **fzf** | `fzf` | Use `fzf` for fuzzy finding anything |
| `git` (UI) | **lazygit** | `lazygit` | Use `lazygit` for terminal git UI |
| `bench` | **hyperfine** | `hyperfine` | Use `hyperfine` for benchmarking |

## Quick Reference

### File Viewing
```bash
bat FILE              # cat with syntax highlighting
bat -n FILE           # cat with line numbers
hexyl FILE            # hex dump with colors
glow FILE.md          # render Markdown
```

### File Listing
```bash
eza                   # ls with colors and icons
eza -l                # ls -l with git status
eza -la               # include hidden files
eza -T                # tree view
eza -l --git          # with git status
```

### File Search
```bash
fd PATTERN            # find files by name
fd -e rs              # find by extension
fd -t d               # find directories only
fd -H                 # include hidden files
```

### Text Search
```bash
rg PATTERN            # grep recursively
rg -t rust            # search only Rust files
rg -C 3               # with context
rg --replace TEXT     # replace matches
```

### Text Processing
```bash
sd 'old' 'new' FILE   # find and replace
```

### Navigation
```bash
z PATTERN             # smart cd
z -i                  # interactive cd
```

### Git
```bash
lazygit               # terminal git UI
git diff | delta      # syntax-highlighted diffs
```

### JSON
```bash
fx FILE.json          # interactive JSON viewer
gron FILE.json        # make JSON greppable
jq '.key' FILE.json   # query JSON
```

### System
```bash
btop                  # system monitor
procs                 # process list
dust                  # disk usage
duf                   # disk free
```

### Fuzzy Finding
```bash
fzf                   # fuzzy find files
history | fzf         # fuzzy search history
```

### Benchmarking
```bash
hyperfine 'cmd1' 'cmd2'  # compare commands
```

## Recommended Aliases

Add to your shell config (`~/.bashrc`, `~/.zshrc`):

```bash
# Modern replacements
alias cat='bat --paging=never'
alias ls='eza'
alias ll='eza -l'
alias la='eza -la'
alias lt='eza -T'
alias find='fd'
alias grep='rg'
alias top='btop'
alias du='dust'
alias df='duf'
alias ps='procs'
alias cd='z'

# Git
alias lg='lazygit'

# JSON
alias jq='fx'
```

## Shell Integration

```bash
# zoxide
eval "$(zoxide init bash)"

# fzf
eval "$(fzf --bash)"

# Add to PATH if needed
export PATH="$HOME/.local/bin:$PATH"
```

## Tips

1. **Prefer `bat` over `cat`** — syntax highlighting makes reading code easier
2. **Prefer `eza` over `ls`** — colors, icons, and git status at a glance
3. **Prefer `fd` over `find`** — faster, simpler, respects .gitignore
4. **Prefer `rg` over `grep`** — faster, respects .gitignore, better defaults
5. **Use `z` instead of `cd`** — jump to frequent directories faster
6. **Use `lazygit`** — staging hunks and resolving conflicts is easier
7. **Use `fzf` everywhere** — fuzzy find files, history, processes, etc.
8. **Use `delta` for diffs** — syntax highlighting makes changes clear
