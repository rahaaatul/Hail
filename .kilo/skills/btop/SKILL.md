# btop Skill

Beautiful system resource monitor — modern replacement for top/htop with GPU support, themes, presets, mouse, and extensive configuration.

## When to Use

- Monitoring CPU, memory, disk, network, GPU in real-time
- Finding resource-hungry processes
- Replacing `top`, `htop`, `bashtop`
- Killing/renicing processes from within the monitor
- Visualizing performance trends with graphs
- Customizable dashboard for system overview

## Basic Usage

```bash
btop                    # Launch interactive monitor
btop -p 1                # Start with preset 1
btop -u 2000             # Update every 2 seconds
btop --tty               # Force TTY mode (16 colors)
btop -d                  # Debug mode
```

## Command Line Options

| Flag | Description |
|------|-------------|
| `-c FILE` / `--config FILE` | Path to config file |
| `-d` / `--debug` | Debug mode with extra logs |
| `-f FILTER` / `--filter FILTER` | Initial process filter |
| `--force-utf` | Override UTF locale detection |
| `-l` / `--low-color` | 256 colors only, no truecolor |
| `-p N` / `--preset N` | Start with layout preset (0-9) |
| `-t` / `--tty` | Force TTY mode (ANSI symbols, 16 colors) |
| `--no-tty` | Force disable TTY mode |
| `-u N` / `--update N` | Update interval in ms (default: 1000, recommended ≥ 2000) |
| `--default-config` | Print default config to stdout |
| `--version` | Show version |

## Keyboard Shortcuts

### Global

| Key | Action |
|-----|--------|
| `q` / `Ctrl+c` | Quit |
| `Esc` / `m` | Toggle main menu |
| `F1` / `?` / `h` | Help |
| `Ctrl+z` | Background btop |
| `Ctrl+r` | Reload config from disk |
| `F2` / `o` | Options menu |
| `+` / `-` | Add/subtract 100ms to update timer |
| `p` / `Shift+p` | Cycle presets forward/backward |

### Panels

| Key | Action |
|-----|--------|
| `1` | Toggle CPU box |
| `2` | Toggle MEM box |
| `3` | Toggle NET box |
| `4` | Toggle PROC box |
| `5` | Toggle GPU box |
| `d` | Toggle disks view in MEM box |
| `i` | Toggle disk IO mode with big graphs |

### Process List

| Key | Action |
|-----|--------|
| `↑`/`↓` or `j`/`k` | Navigate processes |
| `Enter` | Detailed process info |
| `Space` / `e` / `E` | Expand/collapse selected / all |
| `f` / `/` | Filter processes (start with `!` for regex) |
| `F` | Follow selected process |
| `u` | Pause process list |
| `Delete` | Clear filter |
| `c` | Toggle per-core CPU usage |
| `r` | Reverse sort order |
| `Left`/`Right` | Select previous/next sort column |
| `PgUp`/`PgDn` | Page up/down |
| `Home`/`End` | Jump to first/last |
| `t` | Terminate (SIGTERM) |
| `k` | Kill (SIGKILL) |
| `s` | Select signal to send |
| `N` | Renice selected process |
| `%` | Toggle memory display mode (bytes vs percent) |
| `y` | Toggle totals reset for network |

### Network

| Key | Action |
|-----|--------|
| `b` / `n` | Previous/next network device |
| `z` | Toggle totals reset |
| `a` | Toggle auto-scaling |

## Configuration

Config at `~/.config/btop/btop.conf` (auto-generated on first run).

Key settings:

```ini
color_theme = "Default"
theme_background = true
truecolor = true
force_tty = false
vim_keys = false
rounded_corners = true
terminal_sync = true

# Graph symbols: braille, block, tty
graph_symbol = "braille"
graph_symbol_cpu = "default"
graph_symbol_mem = "default"
graph_symbol_net = "default"
graph_symbol_proc = "default"

shown_boxes = "cpu mem net proc"
update_ms = 2000

# Process sorting: pid, program, arguments, threads, user, memory, cpu lazy, cpu direct
proc_sorting = "cpu lazy"
proc_reversed = false
proc_tree = false
proc_colors = true
proc_gradient = true
proc_per_core = false
proc_mem_bytes = true
proc_cpu_graphs = true
proc_info_smaps = false
proc_left = false
proc_filter_kernel = false
proc_follow_detailed = true
proc_aggregate = false

# CPU graphs
cpu_graph_upper = "Auto"
cpu_graph_lower = "Auto"
cpu_invert_lower = true
cpu_single_graph = false
show_cpu_watts = true

# Memory
mem_graphs = true
show_disks = true
only_physical = true
use_fstab = true
zfs_hide_datasets = false
disk_free_priv = false
show_io_stat = true
io_mode = false
io_graph_combined = false
io_graph_speeds = ""

# Network
net_auto = true
net_sync = true
net_download = 100
net_upload = 100
net_iface = ""
base_10_bitrate = "Auto"
```

### Presets

Format: `"box_name:P:G,box_name:P:G"` where P=position (0 or 1), G=graph symbol.
Max 9 presets, separated by whitespace. Cycle with `p`/`Shift+p`.

## Themes

- Built-in: `Default`, `TTY`
- Custom: `.theme` files in `~/.config/btop/themes/`
- Popular: Catppuccin (Mocha, Macchiato, Frappe, Latte), Tokyo Night, Monokai
- Set via options menu (`F2` → color theme) or edit config
- `theme_background = false` uses terminal's own background

## GPU Support

- Shows `gpu0` through `gpu5` boxes
- Requires build with `GPU_SUPPORT=true` (most distro packages)
- Linux only (ROCm SMI / NVML)

## Examples

```bash
# Launch with preset
btop -p 2

# Slow update for battery saving
btop -u 5000

# TTY mode (SSH, containers)
btop --tty

# Low color mode
btop --low-color

# Force UTF-8
btop --force-utf

# Debug mode
btop -d

# Start filtered to a process
btop -f firefox

# Print default config
btop --default-config > ~/.config/btop/btop.conf
```

## Common Pitfalls

- Default `update_ms = 1000` is too aggressive — use 2000+ for smoother graphs and lower CPU
- `proc_info_smaps = true` is very slow — only enable when you need accurate memory
- GPU support requires specific build flags; not all packages include it
- `--tty` forces 16-color mode and disables Unicode box drawing
- `terminal_sync = true` reduces flicker but requires terminal support for sync sequences
- `io_mode` shows big IO graphs but needs `show_io_stat = true`

## Integration

```bash
# Alias to replace top
alias top='btop'

# Minimal container version
btop --tty -p 0

# Watch specific process
btop -f node
```

## Tips

- Press `?` inside btop for full help — it's context-sensitive
- `vim_keys = true` enables h/j/k/l navigation
- `presets` let you switch between focused views (CPU-only, network-only, etc.)
- Mouse works in most terminals — click to sort, select, and scroll
- `Ctrl+r` reloads config without restarting
