# jq Skill

Lightweight command-line JSON processor — query, filter, transform, and aggregate JSON with a powerful DSL.

## When to Use

- Parsing JSON from APIs, kubectl, AWS CLI, docker, gh
- Filtering and extracting specific values
- Transforming JSON structure (rename keys, reshape arrays, merge objects)
- Aggregating data (sum, count, group_by, reduce)
- Processing NDJSON/streaming large files
- Shell scripting with JSON (inject env vars, build configs)

## Basic Usage

```bash
jq '.' FILE.json            # Pretty print
echo '{"a":1}' | jq '.'     # From stdin
jq '.name' file.json        # Extract field
```

## Essential Flags

| Flag | Description |
|------|-------------|
| `-r` | Raw output — strip JSON quotes from strings |
| `-c` | Compact output — one JSON value per line (NDJSON) |
| `-s` | Slurp — read all inputs into a single array |
| `-S` | Sort keys alphabetically |
| `-e` | Exit code based on output (0 if not false/null) |
| `-n` | Use `null` as input (construct JSON from scratch) |
| `-R` | Read raw strings as input, not JSON |
| `--stream` | Stream mode — process as path-value pairs (memory-efficient) |
| `--seq` | Process concatenated JSON/JSON sequences |
| `-M` | Monochrome — no colors |
| `--arg name val` | Pass string variable into filter |
| `--argjson name val` | Pass JSON value (number/object/array) into filter |
| `-f file.jq` | Read filter from file |

## Basic Filters

```bash
# Identity (pretty print)
jq '.' file.json

# Extract field
jq '.name' file.json

# Nested field
jq '.user.email' file.json

# Array index
jq '.items[0]' file.json

# Array slice
jq '.items[0:3]' file.json

# Array length
jq '.items | length' file.json

# Iterate all elements
jq '.items[]' file.json

# All fields
jq 'keys' file.json
```

## Filtering

```bash
# Filter array by condition
jq '.items[] | select(.price > 100)' file.json

# Multiple conditions
jq '.[] | select(.active == true and .age > 18)' file.json

# String contains
jq '.[] | select(.name | contains("test"))' file.json

# Startswith / endswith
jq '.[] | select(.name | startswith("A"))' file.json

# Regex
jq '.[] | select(.email | test("@gmail\\.com"))' file.json

# Existence check
jq '.[] | select(.email != null)' file.json

# Safe navigation (suppress errors)
jq '.users[]?.email' file.json

# Recursive descent (find anywhere)
jq '.. | .id? // empty' file.json
```

## Transforming

```bash
# Select fields
jq '{name, email}' file.json

# Rename fields
jq '{fullName: .name, contact: .email}' file.json

# Add computed field
jq '. + {count: (.items | length)}' file.json

# Remove field
jq 'del(.password)' file.json

# Map
jq '[.items[] | .name]' file.json
jq 'map(.name)' file.json

# Sort
jq 'sort_by(.name)' file.json
jq 'sort_by(-.price)' file.json

# Reverse
jq 'reverse' file.json

# Unique
jq 'unique' file.json
jq 'unique_by(.id)' file.json

# Group by
jq 'group_by(.category)' file.json

# Flatten
jq 'flatten(1)' file.json

# Transpose object of arrays to array of objects
jq 'to_entries | map({(.key): .value}) | transpose | map(add)' file.json
```

## Aggregation

```bash
# Sum
jq '[.items[].price] | add' file.json
jq 'reduce .items[] as $x (0; . + $x.price)' file.json

# Average
jq '[.items[].price] | add / length' file.json

# Min/Max
jq '[.items[].price] | min' file.json
jq '[.items[].price] | max' file.json
jq 'min_by(.price) | .name' file.json

# Count
jq '.items | length' file.json

# Group and count
jq -s 'group_by(.category) | map({category: .[0].category, count: length})' file.json

# Running total
jq 'reduce .items[] as $x (0; . + $x.bytes)' file.json
```

## String Operations

```bash
# Concatenate
jq '.first + " " + .last' file.json

# Interpolation
jq '"\(.first) \(.last)"' file.json

# Split
jq '.name | split(" ")' file.json

# Join
jq '.items | join(", ")' file.json

# Upper/lower
jq '.name | ascii_upcase' file.json
jq '.name | ascii_downcase' file.json

# Trim
jq '.name | gsub("^\\s+|\\s+$"; "")' file.json

# Base64 encode/decode
jq '.data | @base64' file.json
jq '.encoded | @base64d' file.json

# URL encode
jq '.value | @uri' file.json

# CSV/TSV
jq -r '[.name, .email] | @csv' file.json
jq -r '[.name, .email] | @tsv' file.json

# Regex test
jq '.[] | select(.email | test("@example\\.com$"))' file.json
```

## Conditionals & Error Handling

```bash
# If-then-else
jq 'if .age > 18 then "adult" else "minor" end' file.json

# Alternative operator (fallback for null/false)
jq '.nickname // .name // "anonymous"' file.json

# Try-catch
jq 'try .x catch "missing"' file.json

# Empty (suppress output)
jq 'if .type == "user" then .email else empty end' file.json

# Conditional field inclusion
jq '{name, email, ...(if .admin then {role: "admin"} else {} end)}' file.json
```

## Shell Integration

```bash
# Pass shell variable safely
jq --arg name "$USER" '. + {createdBy: $name}' file.json

# Pass JSON value
jq --argjson count 42 '. + {count: $count}' file.json

# Environment variable
jq -n '{home: $ENV.HOME, user: $ENV.USER}'

# Build JSON from scratch
jq -n '{name: "test", value: 42}'

# Multiple files (slurp)
jq -s '.[0] * .[1]' base.json patch.json

# Merge objects
jq -s 'add' part1.json part2.json

# Update in place (with sponge)
jq '.version = "2.0"' file.json | sponge file.json
```

## Advanced Patterns

```bash
# Recursive walk (find key anywhere)
jq '.. | .secret? // empty' file.json

# Walk (transform entire tree)
jq 'walk(if type == "string" then gsub("old"; "new") else . end)' file.json

# Custom function
jq 'def add_n: . + {n: (.numbers | add)}; .[] | add_n' file.json

# Stream large file
jq --stream 'select(length==2) | .[0] as $p | .[1]' large.json

# to_entries / from_entries (key-value manipulation)
jq 'to_entries | map(select(.key | test("^tmp_"))) | from_entries' file.json

# Debug
jq 'debug, .name' file.json
```

## Common Pitfalls

- `-r` is essential for shell scripts — without it, strings are JSON-quoted
- `//` is the alternative operator, not a comment (jq has no comments in filters)
- `.a,.b` produces two separate outputs, not an array — use `[.a, .b]`
- `add` on empty array returns `null` — use `// 0` or `// ""`
- Integer arithmetic uses IEEE 754 doubles — precision lost beyond 2^53
- `group_by` requires sorted input or `-s` slurp; it sorts groups alphabetically
- `--arg` creates strings; use `--argjson` for numbers/booleans/objects
- Single-quote jq filters in shell to prevent variable expansion
- `null` output usually means a typo in key name (JSON is case-sensitive)
- jq rejects JSON with comments or trailing commas — use `--strict` or preprocess

## Integration

```bash
# With curl
curl -s https://api.github.com/users/octocat | jq '{name, bio, location}'

# With kubectl
kubectl get pods -o json | jq '.items[] | select(.status.phase == "Running") | .metadata.name'

# With AWS CLI
aws ec2 describe-instances | jq '.Reservations[].Instances[] | {id: .InstanceId, type: .InstanceType}'

# With docker
docker inspect container | jq '.[0].Config.Image'

# With fzf
jq -r '.[] | .name' file.json | fzf

# Watch live value
watch -n 5 "curl -s api.example.com/health | jq '.uptime'"
```

## Tips

- Test filters at jqplay.org before embedding in scripts
- Save complex filters to `.jq` files: `jq -f filter.jq input.json`
- Use `try ... catch` for heterogeneous data pipelines
- Use `--stream` for files > 250MB
- Keep a `.jqrc` in `$HOME` for reusable helper functions
