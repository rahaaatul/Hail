# fzf Skill

General-purpose command-line fuzzy finder written in Go. Interactive filtering for any list (files, history, processes, branches, commits, env vars, etc.) with previews, key bindings, and a rich scripting layer.

## When to Use

- Interactive file/directory selection
- Fuzzy command-history search (replaces Ctrl+R)
- Picking items from any list (branches, processes, containers, packages)
- Building interactive terminal menus and workflows
- Previews while filtering (with bat, eza, git, etc.)
- Custom completion for shell commands

## Basic Usage

```bash
fzf                              # Read paths from $FZF_DEFAULT_COMMAND (or `find .`)
find . -type f | fzf             # Fuzzy-find files from stdin
fzf --preview "cat {}"           # Show preview pane
fd --type f | fzf --preview 'bat --color=always {}'
```

## Layout & Display

| Flag | Description |
|------|-------------|
| `--height N%` | Height below cursor (non-fullscreen) |
| `--layout=LAYOUT` | `default`, `reverse` (input on top), `reverse-list` |
| `--border[=STYLE]` | Box border (`rounded`, `sharp`, `bold`, `double`, `horizontal`, `vertical`, `none`) |
| `--border-label` / `--border-label-pos` | Title text and position (`top`/`bottom`, `N`) |
| `--margin`, `--padding` | Outer spacing |
| `--info=STYLE` | `inline`, `inline-right`, `hidden`, `default` |
| `--inline-info` | Match count on the right |
| `--prompt=STR` | Prompt prefix |
| `--pointer=STR`, `--marker=STR` | Cursor/selection glyphs |
| `--separator=STR`, `--no-separator` | Match-highlight separator |
| `--scrollbar=CHAR[CHAR]` | Scrollbar chars (e.g. `█░`) |
| `--no-scrollbar` | Disable scrollbar |
| `--color=COLSPEC` | `bg+:#363a4f,hl:#ed8796,...` |
| `--ansi` | Preserve ANSI colors in input |
| `--no-bold` | Disable bold highlighting |
| `--tabstop=N` | Tab width |
| `--wrap` | Wrap long lines |
| `--ghost=TEXT` | Placeholder hint in empty query |

## Search & Matching

| Flag | Description |
|------|-------------|
| `-e`, `--exact` | Disable fuzzy matching (substring) |
| `--regex` | Treat query as regex (rather than literal/fuzzy) |
| `-i` / `+i` | Case-insensitive / case-sensitive |
| `-n N,M` | Restrict search to fields N..M (1-indexed, ranges with `-`) |
| `--nth=N[,..]` / `--with-nth=N[,..]` | Search/display only given fields |
| `--scheme=default\|path\|history` | Match scheme hint |
| `--no-sort` / `--sort=mode` | Result ordering |
| `--tac` | Reverse input order |
| `--filter=STR` | Pre-filter (skips interactive prompt) |
| `--print-query` | Print query even if no selection |

### Search Syntax (when `--exact` is off)

| Token | Match Type |
|-------|------------|
| `sbtrkt` | Fuzzy (chars in order) |
| `'wild` | Exact phrase |
| `^music` | Prefix match |
| `.mp3$` | Suffix match |
| `!fire` | Inverse match |
| `!^music` | Inverse prefix |
| `core\|go\|rb$` | OR alternation |
| `*` is escape: `'foo*bar` (literal `*`) |

## Selection & Output

| Flag | Description |
|------|-------------|
| `-m`, `--multi` | Multi-select (Tab/Shift-Tab to mark) |
| `--no-multi` | Force single-select |
| `-1`, `--select-1` | Exit if only one match |
| `-0`, `--exit-0` | Exit if no match |
| `--read0` | Read NUL-separated input |
| `--print0` | Print selection NUL-separated |
| `--expect=KEY[,..]` | Comma keys that also exit (e.g. `ctrl-q`) |
| `--bind=...` | Custom bindings (see below) |
| `--bind 'start:reload:...'` | Reload on launch |

## Preview

```bash
fzf --preview 'bat --color=always --style=numbers {}'
fzf --preview 'eza -T -L 2 --color=always {}'
```

Preview window options (`--preview-window`):

| Token | Effect |
|-------|--------|
| `direction:left\|right\|up\|down` | Position |
| `wrap`, `nowrap`, `cycle` | Wrapping behavior |
| `hidden`, `nohidden` | Default visibility |
| `follow`, `nofollow` | Follow tail of growing file |
| `:+N`, `:+N/M`, `:~N` | Scroll offset (current/first/around match) |
| `:<size>` | Size (lines or %, e.g. `right:60%`) |
| `:rounded`, `:sharp`, `:noborder` | Border style |
| `:info`, `:noinfo` | Show info |

Toggle preview: `--bind 'ctrl-/:toggle-preview'` or `change-preview-window(down|hidden|)`.

## Key Bindings (Default)

| Key | Action |
|-----|--------|
| `Ctrl-K` / `Ctrl-J` (or `Ctrl-P`/`Ctrl-N`) | Up / down |
| `Enter` / `double-click` | Accept |
| `Ctrl-C` / `Ctrl-G` / `Ctrl-Q` / `Esc` | Abort |
| `Tab` / `Shift-Tab` | Mark / unmark (multi-select) |
| `Alt-Backspace` | Kill word |
| `Ctrl-A` / `Ctrl-E` | Start / end of line |
| `Ctrl-U` / `Ctrl-D` | Page up / page down |
| Mouse | Scroll, click, double-click; shift-click for multi-select |

## `--bind` Action Reference

Useful actions: `accept`, `abort`, `become(CMD)`, `execute(CMD)`, `execute-silent(CMD)`, `toggle-preview`, `change-preview(STR)`, `change-preview-window(SPEC)`, `reload(BIND)`, `reload-sync(BIND)`, `transform(STR)`, `transform-header(STR)`, `transform-prompt(STR)`, `put(STR)`, `print-query`, `clear-query`, `clear-selection`, `deselect-all`, `select-all`, `toggle-all`, `toggle-in`, `toggle-out`, `toggle-wrap`, `toggle-search`, `down`, `up`, `first`, `last`, `change-nth(SPEC)`, `change-prompt(STR)`, `change-multi`, `change-header(STR)`, `change-border-label(STR)`, `backward-char`, `backward-delete-char`, `backward-word`, `forward-word`, `kill-word`.

Chain with `+`: `--bind 'ctrl-y:execute-silent(echo {} | pbcopy)+abort'`.

Events: `start`, `load`, `focus`, `change`, `result`, `one`, `zero`.

## Shell Integration (highly recommended first step)

```bash
# Bash
eval "$(fzf --bash)"

# Zsh
source <(fzf --zsh)

# Fish
fzf --fish | source

# Nushell
source $"($fzf --nu | str collect)"
```

This installs `Ctrl-T` (paste files), `Ctrl-R` (history), `Alt-C` (cd), and fuzzy completion for bash/zsh/fish/nu.

Disable individually by setting the env var to empty before sourcing:
```bash
FZF_CTRL_R_COMMAND= FZF_ALT_C_COMMAND= eval "$(fzf --bash)"
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `FZF_DEFAULT_COMMAND` | Source command when input is a terminal |
| `FZF_DEFAULT_OPTS` | Default flags (use `fzf --man` to see how quoted) |
| `FZF_DEFAULT_OPTS_FILE` | File with default opts (e.g. `~/.config/fzf/fzf.bash`) |
| `FZF_CTRL_T_COMMAND` / `_OPTS` | Source / opts for Ctrl-T |
| `FZF_CTRL_R_COMMAND` / `_OPTS` | Source / opts for Ctrl-R |
| `FZF_ALT_C_COMMAND` / `_OPTS` | Source / opts for Alt-C |
| `FZF_COMPLETION_*` | Completion tunables |
| `FZF_FIND_SERVER_COMMAND` | Replace the internal `find` server |

`FZF_DEFAULT_COMMAND` is ignored when input is piped — it only runs when fzf is invoked without stdin (e.g. plain `fzf`).

## Recommended Configuration

```bash
# ~/.bashrc or ~/.zshrc
eval "$(fzf --bash)"

export FZF_DEFAULT_COMMAND='fd --type f --hidden --follow --exclude .git'
export FZF_DEFAULT_OPTS='
  --height 40%
  --layout reverse
  --border rounded
  --inline-info
  --preview "bat --color=always --style=numbers --line-range=:500 {}"
  --preview-window=right:60%:wrap
  --bind "ctrl-/:toggle-preview"
  --bind "ctrl-y:execute-silent(echo -n {} | xclip -selection clipboard)+abort"
  --color bg+:#363a4f,bg:#24273a,spinner:#f4dbd6,hl:#ed8796
  --color fg:#cad3f5,header:#ed8796,info:#c6a0f6,pointer:#f4dbd6
  --color marker:#f4dbd6,fg+:#cad3f5,prompt:#c6a0f6,hl+:#ed8796
'

export FZF_CTRL_T_COMMAND="$FZF_DEFAULT_COMMAND"
export FZF_CTRL_T_OPTS="--preview 'bat -n --color=always {}' --bind 'ctrl-/:change-preview-window(down|hidden|)'"

export FZF_ALT_C_COMMAND='fd --type d --hidden --follow --exclude .git'
export FZF_ALT_C_OPTS="--preview 'eza --tree --level=2 {}'"
```

## Examples

```bash
# Pick a file and open in $EDITOR
$EDITOR "$(fzf)"

# Multi-open files
$EDITOR "$(fzf -m)"

# Ripgrep-style live source picker with bat preview
rg --line-number --no-heading --color=always "" |
  fzf --ansi --delimiter : \
      --preview 'bat --color=always --highlight-line {2} {1}' \
      --preview-window '+{2}-3:3' \
      --bind 'enter:become(${EDITOR:-vim} {1})'

# Branch checkout
git branch -a --color=always |
  fzf --ansi --preview 'git log --oneline --color=always {1}' |
  sed 's/^..//; s/ .*//' | xargs git checkout

# Interactive git log browser
git log --oneline --color=always |
  fzf --ansi --preview 'git show --stat --color=always {1}' |
  awk '{print $1}' | xargs git show

# SSH host picker
grep -E "^Host " ~/.ssh/config | awk '{print $2}' | fzf | xargs -o ssh

# Docker container picker (multi-stop)
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}" |
  fzf --multi | awk '{print $1}' | xargs docker stop

# Process killer
ps -eo pid,comm,args | fzf -m | awk '{print $1}' | xargs kill

# Env-var inspector
env | fzf

# Dynamic reload (switch between files/dirs live)
fzf --bind 'ctrl-f:reload(fd --type f),ctrl-d:reload(fd --type d)'

# NUL-separated, safe for spaces/newlines
fd --type f --print0 | fzf --read0 --print0 | xargs -0 bat

# Use become to replace fzf with a command (no subshell)
fzf --bind 'enter:become(vim {})'

# Header transform shows file metadata
fzf --preview 'ls -la {}' --bind 'focus:transform-header:file --brief {}'
```

## Tips & Best Practices

- Always `eval "$(fzf --bash)"` (or shell equivalent) first — it gives you `Ctrl-T`, `Ctrl-R`, `Alt-C` for free.
- Use `fd` or `ripgrep` as `FZF_DEFAULT_COMMAND` instead of `find` (faster, respects gitignore).
- `--height 40%` + `--layout reverse` is the modern non-intrusive default.
- Combine with `bat` (previews) and `eza` (tree previews) for a polished UI.
- For huge lists, use `--filter=STR` to bypass the TUI entirely and just print matches.
- For files with spaces, prefer `--read0`/`--print0` + `xargs -0`.
- `--bind 'enter:become(cmd {})'` avoids spawning a subshell (cleaner stdio).
- Theme playground: https://vitormv.github.io/fzf-themes/
- `fzf --man` opens the full reference in your terminal pager.