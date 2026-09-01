# hyperfine Skill

Command-line benchmarking tool — compare execution times with statistical analysis, warmup, parameter scans, and export.

## When to Use

- Benchmarking command performance with statistics
- Comparing two or more commands side-by-side
- Measuring optimization improvements over time
- Parameterized benchmarks (e.g., thread counts, compression levels)
- When you need reproducible, statistically sound timing

## Basic Usage

```bash
hyperfine 'command1' 'command2'
hyperfine 'cmd'
```

## Core Options

| Flag | Description |
|------|-------------|
| `-w N` / `--warmup N` | Warmup runs before measurement (fills caches) |
| `-r N` / `--runs N` | Exact number of benchmark runs |
| `-m N` / `--min-runs N` | Minimum runs (default: 10) |
| `-M N` / `--max-runs N` | Maximum runs |
| `-p CMD` / `--prepare CMD` | Run before each timing run (e.g., clear caches) |
| `-c CMD` / `--cleanup CMD` | Run after all runs for a command |
| `-s CMD` / `--setup CMD` | Run once before each set of runs |
| `-C CMD` / `--conclude CMD` | Run after each timing run |
| `-n NAME` / `--command-name NAME` | Name a command in output |
| `-S SHELL` / `--shell SHELL` | Shell to use (bash, zsh, fish, none) |
| `-N` / `--shell=none` | No intermediate shell (best for fast commands) |
| `-i` / `--ignore-failure` | Ignore non-zero exit codes |
| `-u UNIT` / `--time-unit UNIT` | microsecond, millisecond, second |
| `--style TYPE` | auto, basic, full, nocolor, color, none |
| `--sort ORDER` | auto, command, mean-time |

## Parameterized Benchmarks

```bash
# Numeric scan: varies N from 1 to 8
hyperfine -P threads 1 8 'make -j {threads}'

# Custom step size
hyperfine -P delay 0.3 0.7 -D 0.2 'sleep {delay}'

# List of values
hyperfine -L compiler gcc,clang '{compiler} -O2 main.cpp'

# Power-of-two scan
hyperfine -P size 0 3 'sleep $((2**{size}))'
```

## Export Formats

| Flag | Description |
|------|-------------|
| `--export-json FILE` | JSON with individual run data |
| `--export-csv FILE` | CSV summary |
| `--export-markdown FILE` | Markdown table |
| `--export-asciidoc FILE` | AsciiDoc table |
| `--export-orgmode FILE` | Emacs org-mode table |

## Output Control

| Flag | Description |
|------|-------------|
| `--show-output` | Print stdout/stderr (slower, for debugging) |
| `--output TARGET` | null, pipe, inherit, or file path |
| `--input FILE` | Read input from file instead of /dev/null |

`$HYPERFINE_ITERATION` env var is available in commands for per-iteration file naming.

## Examples

```bash
# Compare two commands
hyperfine 'sleep 0.1' 'sleep 0.2'

# Benchmark with warmup and prepare
hyperfine -w 3 \
  --prepare 'sync; echo 3 | sudo tee /proc/sys/vm/drop_caches' \
  'grep -r TODO src/'

# Compare build tools
hyperfine 'make' 'ninja'

# Parameterized benchmark
hyperfine -P threads 1 12 'make -j {threads}'

# Compare compression levels
hyperfine -L level 1,9 'gzip -{level} -c file > /dev/null'

# Benchmark fast command (no shell)
hyperfine --shell=none 'echo hello'

# Shell function benchmark
export -f myfunc
hyperfine 'myfunc arg'

# Compare git branches
hyperfine \
  --warmup 1 \
  --command-name main \
  --prepare 'git switch main' \
  'pytest tests/' \
  --command-name feature \
  --prepare 'git switch feature-branch' \
  'pytest tests/'

# Export results
hyperfine --export-json results.json --export-markdown summary.md \
  'rg pattern' 'grep -r pattern'

# Ignore failures
hyperfine -i 'cmd-that-sometimes-fails'

# Custom time unit
hyperfine -u millisecond 'fast-cmd'
```

## Understanding Output

```
Benchmark 1: cmd1
  Time (mean ± σ):      10.0 ms ±  0.5 ms    [User: 5.0 ms, System: 2.0 ms]
  Range (min … max):    9.0 ms … 11.0 ms    100 runs

Benchmark 2: cmd2
  Time (mean ± σ):       5.0 ms ±  0.3 ms    [User: 2.5 ms, System: 1.0 ms]
  Range (min … max):     4.5 ms …  5.5 ms    100 runs

Summary
  cmd2 ran
     2.00 ± 0.15 times faster than cmd1
```

- **Mean ± σ**: Average and standard deviation. Low σ = consistent results.
- **Range**: Min/max across runs. Wide range = noisy environment.
- **Relative speed**: How many times faster one command is vs another.

## Common Pitfalls

- **High standard deviation**: Background processes (browser, IDE) steal CPU. Close them or run on a quiet machine.
- **CPU frequency scaling**: Laptops throttle after warmup. Set governor to `performance` for consistent results.
- **Disk cache skew**: First run is always slower. Use `--warmup` for warm-cache or `--prepare 'sync; echo 3 | sudo tee /proc/sys/vm/drop_caches'` for cold-cache.
- **Shell overhead**: For commands under 5ms, use `--shell=none`. For sub-millisecond, use language-level benchmarks (Criterion, pytest-benchmark).
- **Not for servers**: hyperfine measures process lifetime. Use load-testing tools for daemon latency.
- **Outlier warnings**: Take them seriously — another process interfered. Re-run on a quiet system.

## Integration

```bash
# CI regression detection
hyperfine --export-json current.json --warmup 3 'python3 build.py'
jq '.results[0] | {mean, stddev}' current.json

# Parse JSON results
jq '.results[] | {command, mean, stddev, min, max}' results.json

# Compare with visualization
# Drop JSON onto Venz (try.venz.dev) for charts

# Track across git history
# Use Chronologer or Bencher for continuous benchmarking
```

## Tips

- Always use `--warmup 3` for I/O-heavy commands
- Use `--shell=none` for commands under 5ms
- `--prepare` resets state between runs; `--setup` runs once per command
- Export to Markdown for documentation
- More runs (`-r 50`) absorb noise from background processes
