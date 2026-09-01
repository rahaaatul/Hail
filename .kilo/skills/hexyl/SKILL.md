# hexyl Skill

Command-line hex viewer — colorful, human-readable hex dump with customizable grouping, endianness, and color schemes.

## When to Use

- Viewing binary files with color-coded byte types
- Inspecting file headers (ELF, PNG, ZIP, etc.)
- Debugging binary formats
- Replacing `hexdump`, `xxd`
- Examining specific byte ranges in files
- Piping binary data from curl or other tools

## Basic Usage

```bash
hexyl FILE              # View file in hex
hexyl -n 100 FILE       # View first 100 bytes
echo "test" | hexyl     # View stdin
```

## Display Options

| Flag | Description |
|------|-------------|
| `-n N` / `--length N` | Read first N bytes (supports kB, MB, KiB, MiB, hex like 0xFF) |
| `-c N` / `--bytes N` | Alias for `--length` |
| `-s N` / `--skip N` | Skip first N bytes (negative = from end) |
| `-o N` / `--display-offset N` | Add N to displayed offset |
| `--block-size SIZE` | Block unit size (default: 512) |
| `--panels N` | Number of hex panels (or `auto`) |
| `--terminal-width COLS` | Override terminal width |
| `-v` / `--no-squeezing` | Show all data (don't collapse identical lines) |
| `-p` / `--plain` | Shorthand for `--no-characters --no-position --border=none --color=never` |

## Color & Border

| Flag | Description |
|------|-------------|
| `--color WHEN` | always (default), auto, never, force |
| `--border STYLE` | unicode (default), ascii, none |
| `--print-color-table` | Show byte-type color mapping |

## Byte Grouping & Encoding

| Flag | Description |
|------|-------------|
| `-g N` / `--group-size N` | Bytes per group: 1, 2, 4, or 8 |
| `--endianness END` | little or big (default: big) |
| `-e` | Alias for `--endianness=little` |
| `-b BASE` / `--base BASE` | binary, octal, decimal, hexadecimal (default) |

## Character Display

| Flag | Description |
|------|-------------|
| `-C` / `--characters` | Show character panel (default) |
| `--no-characters` | Hide character panel |
| `-P` / `--no-position` | Hide position/offset column |
| `--character-table MODE` | default, ascii, codepage-1047, codepage-437 |

## Custom Colors (Environment Variables)

```bash
HEXYL_COLOR_ASCII_PRINTABLE=blue
HEXYL_COLOR_ASCII_WHITESPACE="bright green"
HEXYL_COLOR_ASCII_OTHER="#ff7f99"
HEXYL_COLOR_NULL=red
HEXYL_COLOR_NONASCII=magenta
HEXYL_COLOR_OFFSET=cyan
```

Colors: `black`, `blue`, `cyan`, `green`, `magenta`, `red`, `yellow`, `white`, `bright <color>`, or `#abcdef`.

## Color Categories

| Color | Byte Type |
|-------|-----------|
| Bright green | Printable ASCII (0x20–0x7e) |
| Dim green | Whitespace (space, tab, newline) |
| Orange/Red | NULL bytes (0x00) |
| Purple | Non-ASCII / high bytes (0x80–0xff) |

## Examples

```bash
# View file
hexyl file.bin

# View first 256 bytes
hexyl -n 256 file.bin

# View 4 KiB
hexyl -n 4KiB file.bin

# Skip header, show 128 bytes
hexyl --skip 512 --length 128 file.bin

# View from offset 0x100
hexyl -o 0x100 -n 64 file.bin

# Group as 32-bit words (little-endian)
hexyl -g 4 -e file.bin

# Group as 64-bit words (big-endian)
hexyl -g 8 file.bin

# Binary base
hexyl -b binary file.bin

# Octal base
hexyl -b octal file.bin

# Plain output (for piping)
hexyl -p file.bin | less -R

# No characters column
hexyl --no-characters file.bin

# ASCII border
hexyl --border ascii file.bin

# View stdin
cat file.bin | hexyl

# View from curl
curl -s https://example.com/file.bin | hexyl -n 256

# Compare two files
diff <(hexyl file1.bin) <(hexyl file2.bin)

# Show all bytes (no squeezing)
hexyl -v large.bin | less

# View with auto panels
hexyl --panels auto file.bin

# Custom colors
HEXYL_COLOR_NULL=yellow HEXYL_COLOR_NONASCII=red hexyl file.bin
```

## Understanding Output

```
┌────────┬─────────────────────────┬─────────────────────────┬────────┬────────┐
│        │ 00 01 02 03 04 05 06 07 │ 08 09 0a 0b 0c 0d 0e 0f │        │        │
├────────┼─────────────────────────┼─────────────────────────┼────────┼────────┤
│ 000000 │ 7f 45 4c 46 02 01 01 00 │ 00 00 00 00 00 00 00 00 │ .ELF.... ........ │
│ 000010 │ 02 00 3e 00 01 00 00 00 │ 10 00 00 00 00 00 00 00 │ ..>..... ........ │
└────────┴──────────────────────────────────────────────────────────────┯
```

- Left: Position (offset), customizable with `--display-offset`
- Middle: Hex bytes, grouped by `--group-size`
- Right: ASCII representation, controlled by `--character-table`

## Common Pitfalls

- `-n` accepts units: `4KiB`, `1MB`, `0xFF` (hex), not just plain numbers
- `--skip` with negative value seeks from end of file
- `--plain` disables everything: colors, border, characters, position
- `--group-size` > 1 requires `--endianness` to control byte order
- `--no-squeezing` (`-v`) disables the asterisk collapse for identical lines
- `--panels auto` adjusts to terminal width but may not use all space evenly

## Integration

```bash
# Pipe to less with raw control chars
hexyl file.bin | less -R

# Find ELF headers
hexyl file.bin | grep "ELF"

# Inspect boot sector
hexyl --length 512 --group-size 2 -e /dev/sda

# Extract specific range with dd
dd if=file.bin bs=1 skip=256 count=128 | hexyl

# Compare with vimdiff
hexyl file1.bin > /tmp/h1
hexyl file2.bin > /tmp/h2
vimdiff /tmp/h1 /tmp/h2

# Search for byte pattern
hexyl file.bin | grep -E "de ad be ef"
```

## Tips

- Use `-n 16` to quickly check file magic bytes (ELF, PNG, ZIP signatures)
- `--group-size 4 -e` views 32-bit little-endian words (useful for ARM/x86 binaries)
- `--character-table codepage-437` for old DOS/BIOS binary inspection
- Pipe curl output directly: `curl -s URL | hexyl -n 256`
- Use `--print-color-table` to see how your terminal renders each byte category
