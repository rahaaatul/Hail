# duf Skill

Disk Usage/Free Utility — better alternative to `df` with color-coded output, grouping, filtering, sorting, inode info, and JSON export.

## When to Use

- Checking disk space with visual color-coded bars
- Viewing mounted filesystems grouped by type (local, network, FUSE, etc.)
- Replacing `df -h` for daily disk checks
- Finding nearly-full disks at a glance
- Monitoring inode exhaustion ("disk full but space free")
- Exporting disk data as JSON for monitoring scripts

## Basic Usage

```bash
duf                     # Show all filesystems grouped
duf /path               # Show specific mount
duf --all               # Include pseudo/duplicate/inaccessible
duf --json              # JSON output for scripting
```

## Filtering Options

| Flag | Description |
|------|-------------|
| `--all` | Include pseudo, duplicate, inaccessible filesystems |
| `--only TYPE` | Show only groups: local, network, fuse, special, loops, binds |
| `--hide TYPE` | Hide groups: local, network, fuse, special, loops, binds |
| `--only-fs TYPE` | Show only filesystem types (ext4, tmpfs, xfs, vfat, ...) |
| `--hide-fs TYPE` | Hide filesystem types |
| `--only-mp PATTERN` | Show only mount points (supports wildcards) |
| `--hide-mp PATTERN` | Hide mount points (supports wildcards) |

## Display Options

| Flag | Description |
|------|-------------|
| `--sort COL` | Sort by: mountpoint, size, used, avail, usage, inodes, inodes_used, inodes_avail, inodes_usage, type, filesystem |
| `--output COLS` | Choose columns: mountpoint, size, used, avail, usage, inodes, inodes_used, inodes_avail, inodes_usage, type, filesystem |
| `--inodes` | Show inode usage instead of block usage |
| `--theme THEME` | dark, light, ansi |
| `--style STYLE` | unicode, ascii |
| `--width N` | Max output width |
| `--json` | Output as JSON |
| `--warnings` | Output warnings to STDERR |

## Thresholds

| Flag | Description |
|------|-------------|
| `--avail-threshold "10G,1G"` | Yellow/red thresholds for available space (supports SI/binary prefixes) |
| `--usage-threshold "0.5,0.9"` | Yellow/red thresholds for usage (0.0–1.0 floats) |

## Device Groups

- `local` — Physical disks and partitions
- `network` — NFS, SMB, and other network mounts
- `fuse` — FUSE-mounted filesystems
- `special` — Special filesystems (proc, sysfs, devtmpfs, ...)
- `loops` — Loop devices (snap packages, etc.)
- `binds` — Bind mounts

## Examples

```bash
# Basic usage
duf

# Show all filesystems
duf --all

# Show only local disks
duf --only local

# Hide noise from loops, special, fuse
duf --hide loops,special,fuse

# Show only ext4 and xfs
duf --only-fs ext4,xfs

# Hide tmpfs and squashfs
duf --hide-fs tmpfs,squashfs

# Hide snap mounts with wildcard
duf --hide-mp /snap/*

# Sort by usage (fullest disk first)
duf --sort usage

# Sort by available space (smallest first)
duf --sort avail

# Custom columns
duf --output mountpoint,size,usage

# Show inode usage instead of block usage
duf --inodes

# Inode usage sorted by inodes_usage
duf --inodes --sort inodes_usage

# JSON output for scripting
duf --json

# JSON filtered with jq
duf --json --only local | jq '.[] | select(.usage > 0.9)'

# Set usage color thresholds (50% yellow, 90% red)
duf --usage-threshold="0.5,0.9"

# Set available space thresholds (10G yellow, 1G red)
duf --avail-threshold="10G,1G"

# Light theme
duf --theme light

# ASCII style (no Unicode box drawing)
duf --style ascii

# Specific mount points
duf /home /var /boot

# Combine filters: only ext4, exclude /boot, sort by avail
duf --only-fs ext4 --hide-mp /boot --sort avail

# Wide output
duf --width 120
```

## Understanding Output

```
╭──────────────────────────────────────────────────────────────╮
│ Mountpoint      Size    Used    Avail   Use%  Type  Filesystem│
├──────────────────────────────────────────────────────────────┤
│ /               50G     20G     28G     42%   ext4  /dev/sda1 │
│ /home           100G    60G     37G     62%   ext4  /dev/sda2 │
│ /boot           512M    100M    400M    20%   ext4  /dev/sda3 │
╰──────────────────────────────────────────────────────────────╯
```

- Color-coded bars: green (plenty), yellow (moderate), red (low space)
- Grouped by device type automatically
- Auto-adjusts to terminal width

## JSON Output Format

```json
[
  {
    "device": "/dev/sda1",
    "mountpoint": "/",
    "fs_type": "ext4",
    "total": 53687091200,
    "used": 21474836480,
    "free": 30064771072,
    "inodes": 0,
    "type": "local"
  }
]
```

Notes on JSON:
- Field names are `total`/`free`, not `SIZE`/`AVAIL`
- No precomputed `usage` field — compute as `used / total`
- Inode fields appear when `--inodes` is used

## Common Pitfalls

- `--hide-mp` and `--only-mp` support glob wildcards (e.g., `/snap/*`, `/sys/*`)
- `--json` automatically drops colors — no `--color=never` needed
- `--inodes` swaps block columns for inode columns entirely
- `--output` doesn't include group headings; combine with `--only`/`--hide` for focused views
- `--sort` applies within each group, not across groups
- `--width` caps output but may truncate wide mount points

## Integration

```bash
# Check root filesystem usage
duf --json --only local | jq '.[] | select(.mountpoint == "/") | .used'

# Alert when any disk > 90%
duf --json | jq -r '.[] | select(.usage > 0.9) | "\(.mountpoint): \(.usage * 100 | floor)%"'

# Watch disk usage every 5 seconds
watch -n 5 duf --only local --sort usage

# List mount points only
duf --output mountpoint --json | jq -r '.[].mountpoint'

# Find filesystems with < 5% free
duf --json --only local | jq -r '.[] | select((.free / .total) < 0.05) | .mountpoint'

# Compare with ncdu: duf tells you which disk is full, ncdu tells you which folders
```

## duf vs df vs ncdu

| Tool | What it answers | Best for |
|------|-----------------|----------|
| `df` | How full is each filesystem | Universal, always present |
| `duf` | Same, but readable and grouped | Quick visual check |
| `ncdu` | Which directories eat the space | Hunting files to delete |

Typical workflow: `duf` → spot full disk → `ncdu /` → find big directories.

## Tips

- `duf --only local --sort usage` is the best daily-check alias
- Use `--inodes` when "disk full" but `df` shows free space
- `--hide-mp '/sys/*','/proc/*'` cleans up pseudo-mounts
- JSON output makes it scriptable for monitoring/alerting
- No config file needed — all flags are self-contained
