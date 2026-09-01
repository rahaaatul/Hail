# zoxide Skill

Smarter `cd` command that learns your navigation patterns. Jump to frequent/frecent directories with partial names.

## When to Use

- Navigating to frequently visited directories
- Replacing `cd` with fuzzy matching
- Jumping to deep paths without typing full path
- When you want to navigate faster
- Scripting directory resolution without changing cwd

## Basic Usage

```bash
z foo                    # Jump to most frecent directory matching "foo"
z foo bar                # Jump to directory matching "foo" and "bar"
z foo /                  # cd into a subdirectory starting with "foo"
z foo/                   # cd into relative path (literal)
z ~/foo                  # z also works like regular cd
z ..                     # Go up one directory
z -                      # Go to previous directory
zi foo                   # Interactive selection with fzf
z foo<SPACE><TAB>        # Show interactive completions (bash 4.4+/fish/zsh)
```

## Common Patterns

### Jump Options

| Flag | Description |
|------|-------------|
| `-i` / `--interactive` | Interactive selection (fuzzy match with fzf) |
| `-c` / `--current` | Restrict matches to subdirectories of current dir |
| `-e` / `--echo` | Echo the best match without changing directory |
| `-l` / `--list` | List all matches with scores |
| `-s` / `--score` | Show scores when listing matches |
| `-a` / `--all` | Include non-existent directories in results |

### Query Command

```bash
zoxide query foo              # Print best match (no cd)
zoxide query --list foo       # List all matches
zoxide query --score foo      # Show match with score
zoxide query --all foo        # Include deleted directories
zoxide query --interactive    # Interactive selection
```

### Database Management

```bash
zoxide add /path/to/dir       # Manually add directory to database
zoxide remove /path/to/dir    # Remove directory from database
zoxide remove                 # Remove current directory (with -x flag)
```

### Import from Other Tools

```bash
zoxide import autojump        # Import from autojump
zoxide import z               # Import from z
zoxide import z.lua            # Import from z.lua
zoxide import zsh-z           # Import from zsh-z
zoxide import fasd            # Import from fasd
zoxide import atuin           # Import from atuin
```

### Shell Integration

```bash
# Add to shell config (.bashrc/.zshrc)
eval "$(zoxide init bash)"    # or zsh, fish, nushell, powershell, elvish, tcsh, xonsh

# Replace cd entirely (records ALL directory changes)
eval "$(zoxide init bash --cmd cd)"

# Custom command prefix
eval "$(zoxide init bash --cmd j)"    # Creates j and ji commands

# Change hook behavior
eval "$(zoxide init bash --hook none)"     # Never auto-record
eval "$(zoxide init bash --hook prompt)"   # Record at every prompt
eval "$(zoxide init bash --hook pwd)"      # Record on directory change (default)

# Disable command creation (use __zoxide_z directly)
eval "$(zoxide init bash --no-cmd)"
```

## Examples

```bash
# Jump to project
z hail                      # Jumps to /workspaces/Hail if frecent

# Jump with multiple terms (more precise)
z work hail                 # Matches /workspaces/Hail

# Interactive selection
zi                           # Fuzzy find all tracked directories
zi src                       # Interactive cd into subdirectories

# List matches with scores
z -l foo                     # Show all directories matching foo with scores

# Echo without jumping (useful in scripts)
z -e project                # Prints path without cd

# Restrict to subdirectories
z -c src                     # Only match subdirectories of current dir

# Score a directory up
cd /some/path                # Automatically scored by zoxide

# Manually add a directory
zoxide add ~/projects/new    # Add without visiting

# Remove a directory
zoxide remove ~/old-project  # Remove from database

# Use in scripts
PROJECT=$(zoxide query my-project) && cd "$PROJECT"
```

## How It Works

1. zoxide tracks directories you `cd` into (or that you manually `add`)
2. Each directory gets a score based on frequency and recency (frecency)
3. When you type `z pattern`, it finds the highest-scored match
4. Interactive mode (`zi`) lets you fuzzy-select from matches via fzf

## Scoring Algorithm

- **Frecency** = frequency + recency
- More visits = higher score
- Recent visits weighted higher
- Scores decay over time
- `_ZO_MAXAGE` limits total entries (default: 10000)

## Shell Integration Key Bindings

After `eval "$(zoxide init bash)"`:
- `z` — smart cd
- `zi` — interactive cd (requires fzf)
- `z<SPACE><TAB>` — interactive completions

## Environment Variables

| Variable | Description |
|----------|-------------|
| `_ZO_DATA_DIR` | Database location (default: `~/.local/share/zoxide`) |
| `_ZO_ECHO` | Print match before cd (set to 1) |
| `_ZO_EXCLUDE_DIRS` | Directories to exclude (colon-separated globs) |
| `_ZO_FZF_OPTS` | Options for fzf in interactive mode |
| `_ZO_MAXAGE` | Maximum number of entries (default: 10000) |
| `_ZO_RESOLVE_SYMLINKS` | Resolve symlinks before adding (set to 1) |

## Third-Party Integrations

| Tool | Description |
|------|-------------|
| `sesh` | tmux session manager (native) |
| `telescope.nvim` | Neovim fuzzy finder |
| `zoxide.vim` | Vim/Neovim plugin |
| `ranger` | File manager |
| `yazi` | File manager |
| `lf` | File manager |
| `nnn` | File manager |
| `felix` | File manager |
| `joshuto` | File manager |
| `zabb` | Finds shortest query for path |

## Pitfalls and Gotchas

- **Shell init required**: Just installing the binary isn't enough — must run `eval "$(zoxide init bash)"`
- **fzf required for `zi`**: Interactive mode needs fzf installed (v0.51.0+)
- **Hook conflicts**: Some prompt frameworks or custom `cd` functions may interfere. Place init line later in config.
- **`--cmd cd` caution**: Replacing `cd` entirely can break scripts that depend on standard `cd` behavior. Test first.
- **Database location**: On ephemeral systems (NixOS), persist `_ZO_DATA_DIR` to survive reboots.
- **Symlinks**: By default, symlinks are stored as-is. Use `_ZO_RESOLVE_SYMLINKS=1` to resolve them.
- **Warp terminal**: Space+Tab completions not supported in Warp.

## Tips

- Use `z -i` when unsure of exact name
- Combine with fzf for best interactive experience
- Exclude noisy directories: `export _ZO_EXCLUDE_DIRS="/tmp/*:/proc/*:/sys/*"`
- Use `zoxide query` in scripts (not `z`, which is a shell function)
- Multi-keyword jumps are more precise: `z work project` vs `z wp`
- Use `z -l` to debug why a particular match was chosen
- Import existing history from autojump/z when switching
