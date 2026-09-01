# lazygit Skill

Terminal UI for git — stage, commit, branch, rebase, and diff without leaving the terminal. Written in Go.

## When to use

- Interactive git operations
- Staging specific hunks/lines
- Resolving merge conflicts
- Browsing commit history
- When you want a visual git interface
- Interactive rebasing (squash, fixup, reword, drop)
- Cherry-picking commits
- Managing stashes

## Basic Usage

```bash
lazygit                          # Launch in current directory
lazygit -p PATH                  # Launch with path filter
lazygit -f PATH                  # Filter history for path
lazygit -c KEY=VAL               # Override config value
lazygit --work-tree DIR          # Set working tree
lazygit --git-dir DIR            # Set git directory
lazygit -sm normal               # Screen mode: normal, half, full
lazygit status                   # Launch focused on status panel
```

## Key Bindings

### Global

| Key | Action |
|-----|--------|
| `?` | Show help (context-sensitive) |
| `q` | Quit |
| `Q` | Quit without changing directory |
| `tab` / `h`/`l` | Switch panel |
| `1-5` | Switch to panel 1-5 |
| `x` | Open menu |
| `@` | Open custom commands |
| `c` | Commit |
| `C` | Commit with editor |
| `P` | Push |
| `p` | Pull |
| `R` | Refresh |
| `z` | Undo last git operation |
| `Z` | Redo (after undo) |
| `,` | Open config menu |
| `/` | Filter current panel |
| `ctrl+e` | Diff two refs |

### Files Panel

| Key | Action |
|-----|--------|
| `space` | Stage/unstage file |
| `a` | Stage/unstage all files |
| `d` | Discard changes (shows options) |
| `e` | Edit file in editor |
| `o` | Open file |
| `c` | Commit |
| `A` | Amend last commit |
| `M` | Resolve merge conflicts |
| `s` | Stash options (all/staged/unstaged) |
| `enter` | Enter file for line-level staging |
| `v` | Range-select files |
| `W` | Diff menu (word diff, ignore whitespace) |

### Branches Panel

| Key | Action |
|-----|--------|
| `space` | Checkout branch |
| `n` | New branch |
| `N` | Move unpushed commits to new branch |
| `d` | Delete branch |
| `M` | Merge into current |
| `r` | Rebase onto selected |
| `f` | Force checkout |
| `-` | Switch to previous branch |
| `o` | Open pull request (requires gh auth) |
| `G` | Open PR in browser |
| `g` | Sort options |

### Commits Panel

| Key | Action |
|-----|--------|
| `space` | Checkout commit |
| `g` | Reset options (soft/mixed/hard) |
| `r` | Reword commit |
| `d` | Drop commit |
| `s` | Squash into commit below |
| `f` | Fixup into commit below |
| `e` | Edit (start rebase from here) |
| `i` | Start interactive rebase |
| `F` | Create fixup! commit |
| `S` | Apply all fixup! commits (autosquash) |
| `ctrl+j` / `ctrl+k` | Move commit down/up |
| `C` | Copy (cherry-pick) commit |
| `V` | Paste (cherry-pick) copied commits |
| `enter` | View commit diff |

### Stash Panel

| Key | Action |
|-----|--------|
| `space` | Apply stash |
| `g` | Pop stash (apply + delete) |
| `d` | Drop stash |
| `n` | New branch from stash |
| `enter` | View stash contents |

### Staging Panel (Line-Level)

| Key | Action |
|-----|--------|
| `space` | Stage/unstage line or hunk |
| `a` | Toggle between line/hunk selection |
| `v` | Range-select lines |
| `tab` | Switch staged/unstaged view |
| `enter` | Stage single line |

## Panels

1. **Status** — Current branch, upstream status
2. **Files** — Modified, staged, untracked files
3. **Branches** — Local and remote branches
4. **Commits** — Commit history
5. **Stash** — Stashed changes

## Common Workflows

### Stage and Commit

1. Open lazygit
2. Navigate to Files panel (`2`)
3. Press `space` to stage files (or `enter` for line-level staging)
4. Press `c` to commit
5. Type commit message, press Enter
6. Press `P` to push

### Line-Level Staging (Partial Commits)

1. Select file in Files panel, press `enter`
2. `space` to stage line/hunk (`a` toggles line/hunk mode)
3. `v` to range-select multiple lines
4. `tab` to switch between staged/unstaged views
5. Press `c` to commit or `esc` to return

### Interactive Rebase

1. Go to Commits panel (`4`)
2. Navigate to target commit
3. Press `i` to start interactive rebase
4. `s` squash, `f` fixup, `d` drop, `e` edit, `r` reword
5. `ctrl+j`/`ctrl+k` to reorder commits
6. Press `m` for rebase options (continue/abort/skip)

### Cherry-pick

1. Go to Commits panel
2. Press `C` on commits to copy (cherry-pick)
3. Checkout target branch (`space` in Branches panel)
3. Press `V` to paste (apply cherry-picks)

### Amend Last Commit

1. Stage changes in Files panel
2. Press `A` (Shift+a) to amend
3. Or press `c` and select amend option

### Resolve Merge Conflicts

1. Files with conflicts shown in red
2. Press `M` for resolution options
3. `space` to pick hunk (yours/theirs)
4. `b` to keep both
5. `z` to undo resolution

### Undo Last Operation

- Press `z` to undo (reflog-based, covers commits)
- Press `Z` to redo
- Note: Does NOT cover working-tree changes

## Configuration

Config at `~/.config/lazygit/config.yml`:

```yaml
git:
  paging:
    colorArg: always
    pager: delta --dark --paging=never
  autoFetch: true
  autoRefresh: true
  commit:
    signOff: false

gui:
  theme:
    activeBorderColor:
      - green
      - bold
    inactiveBorderColor:
      - default
    selectedLineBgColor:
      - reverse
  showFileTree: true
  showCommandLog: true
  nerdFontsVersion: "3"
  mouseEvents: true

os:
  editPreset: 'nvim'  # or 'vscode', 'vim', 'emacs'

keybinding:
  universal:
    quit: 'q'
    return: '<esc>'

customCommands:
  - key: 'b'
    command: 'gh browse {{.SelectedLocalCommit.Sha}}'
    context: 'commits'
    description: 'Open commit in browser'
```

## Command Line Options

| Flag | Description |
|------|-------------|
| `-f PATH` / `--filter PATH` | Filter history for path |
| `-p PATH` / `--path PATH` | Launch in specific path |
| `-c KEY=VAL` / `--config KEY=VAL` | Override config value |
| `-sm MODE` / `--screen-mode MODE` | Screen mode: normal, half, full |
| `--work-tree DIR` | Set working tree |
| `--git-dir DIR` | Set git directory |
| `--print-config-dir` | Print config directory path |

## Pitfalls and Gotchas

- **Context-sensitive keys**: Same key does different things per panel. Always check `?` for current panel's bindings.
- **Undo limitations**: `z` only undoes git operations (commits, rebases), NOT working-tree changes.
- **Commit confirmation**: v0.62+ changed from `alt+enter` to `ctrl+enter` (Linux/Win) or `cmd+enter` (Mac).
- **Upstream not set**: `P` (push) prompts to set upstream if not configured.
- **Merge conflicts**: Lazygit shows conflicts but doesn't do 3-way merge. Use your editor for complex conflicts.
- **Terminal requirements**: Needs proper color support and modern terminal emulator.
- **Mouse support**: Enable `gui.mouseEvents` in config for click support.
- **Worktree support**: Lazygit handles git worktrees natively.

## Tips

- Press `?` anytime to see available keybindings for current panel
- `z` is your safety net — undo mistakes quickly
- `N` (Shift+n) is the rescue key for "I committed to wrong branch"
- Use `/` in any panel to filter (fuzzy search)
- `@` opens custom commands — extend lazygit with your own git aliases
- `e` on a file opens it in your configured editor
- `W` opens diff options (word diff, ignore whitespace)
- Enable delta as pager for syntax-highlighted diffs
- Use `gh auth login` to enable PR integration (branch icons, `G` to open PR)
- Custom commands can prompt for input and run any shell command
