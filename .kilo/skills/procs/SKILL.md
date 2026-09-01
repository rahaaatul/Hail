# procs Skill

Modern replacement for `ps` — colorful process viewer with search, watch, tree, and JSON output.

## When to Use

- Listing running processes with human-readable formatting
- Finding processes by name, PID, or multiple keywords
- Monitoring processes in real-time (like `top`)
- Replacing `ps aux`, `ps -ef`
- Inspecting Docker containers, TCP/UDP ports, environment variables per process
- Exporting process data as JSON for scripts

## Basic Usage

```bash
procs                  # List all processes
procs NAME             # Filter by name
procs PID              # Show specific PID
procs --json            # JSON output
```

## Display Options

| Flag | Description |
|------|-------------|
| `-a` / `--all` | Show all users' processes |
| `--list` | Show available columns |
| `--only COL` | Show only specified column(s) |
| `--insert COL` | Insert column at Slot/MultiSlot position |
| `--no-header` | Hide header row |
| `--color WHEN` | Color mode: auto, always, never |
| `--pager` | Use pager for output |
| `--json` | Output as JSON |
| `-V` | Show version |

## Sorting

| Flag | Description |
|------|-------------|
| `--sorta COL` | Sort ascending by column |
| `--sortd COL` | Sort descending by column |

Common sort columns: `pid`, `cpu`, `mem`, `rss`, `virt`, `start`, `command`, `user`

Watch mode shortcuts: `n` (next sort column), `p` (prev sort column), `a` (ascending), `d` (descending).

## Search Logic

| Flag | Description |
|------|-------------|
| `--and` | Match all keywords (AND) |
| `--or` | Match any keyword (OR) |
| `--nand` | Hide processes matching all keywords |
| `--nor` | Hide processes matching any keyword |

Search is case-sensitive by default; configure via `~/.procs.toml`.

## Watch Mode

```bash
procs --watch              # 1-second refresh
procs --watch-interval 5   # 5-second refresh
procs -W chrome            # Watch with other options
```

Watch mode shortcuts: `q` (quit), `n`/`p` (next/prev sort column), `a`/`d` (ascending/descending).

## Tree View

```bash
procs --tree               # Show process tree
procs --tree --tree-depth 3 # Tree with depth limit
procs --tree --watch        # Tree in watch mode
```

## Columns Available

| Column | Description |
|--------|-------------|
| `pid` | Process ID |
| `ppid` | Parent PID |
| `user` | Owner |
| `cpu` | CPU usage |
| `mem` | Memory usage |
| `virt` | Virtual memory |
| `rss` | Resident set size |
| `state` | Process state |
| `start` | Start time |
| `time` | CPU time |
| `command` | Command line |
| `docker` | Docker container name |
| `env` | Environment variables |
| `nice` | Nice priority |
| `processor` | Currently assigned CPU |
| `tcp_port` | Bound TCP ports |
| `udp_port` | Bound UDP ports |
| `threads` | Thread count |
| `work_dir` | Current working directory |
| `read_bytes` | Read bytes from storage |
| `write_bytes` | Written bytes to storage |

## Configuration

Config at `~/.procs.toml`:

```toml
[search]
logic = "And"          # And, Or, Nand, Nor
case = "Smart"         # Smart, Insensitive, Sensitive
numeric_search = "Exact"
nonnumeric_search = "Partial"

[display]
show_self = false
show_header = true
show_footer = false
show_kthreads = true
color_mode = "Auto"    # Auto, Always, Disable
theme = "Auto"         # Auto, Dark, Light
separator = "│"
tree_symbols = ["│", "─", "┬", "├", "└"]

[sort]
column = 0
order = "Descending"
```

Load custom config: `procs --load-config /path/to/config.toml`
Use built-in config: `procs --use-config default`

## Examples

```bash
# List all processes
procs -a

# Find specific process
procs firefox

# Show process tree
procs --tree

# Watch processes (like top)
procs --watch

# Sort by CPU descending
procs --sortd cpu

# JSON output for scripting
procs --json | jq '.[] | select(.cpu > 50)'

# Filter with OR logic
procs --or chrome firefox

# Hide self process
procs --no-self

# Show Docker container names
procs --only docker,command,user,cpu,mem

# Show specific PID with environment
procs --insert env 1234

# Tree with depth 2
procs --tree --tree-depth 2

# Generate shell completions
procs --gen-completion bash
```

## Common Pitfalls

- `--tree` reorders processes by dependency, not by sort column
- JSON output (`--json`) disables colors and pager automatically
- `--watch` uses 1-second interval by default; use `--watch-interval` for slower refresh
- Some columns (Docker, Env, TcpPort) require root for full data
- `--color=never` is needed when piping to avoid ANSI escape codes

## Integration

```bash
# Pipe to fzf for interactive selection
procs --color=never | fzf

# Find and kill
procs --color=never chrome | awk '{print $1}' | xargs kill

# Watch high-CPU processes
procs -W --sortd cpu

# Export to JSON and analyze
procs --json | jq '.[] | select(.cpu > 10) | {pid, command, cpu}'

# Find processes on specific port
procs --json | jq '.[] | select(.tcp_port != null) | .tcp_port'

# Count processes by user
procs --json | jq 'group_by(.user) | map({user: .[0].user, count: length})'
```

## Tips

- Use `--watch` for continuous monitoring instead of `top`
- `--tree` shows parent-child relationships clearly
- `--json` makes procs scriptable alongside jq
- `--list` reveals all available columns for customization
- Use `--load-config` to switch between named process views
