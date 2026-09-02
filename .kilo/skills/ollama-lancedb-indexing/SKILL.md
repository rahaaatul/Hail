---
name: ollama-lancedb-indexing
description: Use when setting up, debugging, or configuring free local codebase semantic search via Ollama embeddings + LanceDB vector store. Triggers include "idx failed to initialize", indexing not working, no semantic_search results, model pull failures, or installing Ollama in Linux containers.
---

# Ollama + LanceDB Codebase Indexing

## Overview

Free, local, no-API-key codebase indexing. Ollama generates embeddings; LanceDB stores them embedded (no separate server). Kilo Code parses code with Tree-sitter, embeds semantic blocks, and exposes `semantic_search` for natural-language queries.

## When to Use

- Installing Ollama for the first time, especially in a container/CI where systemd is absent
- "idx failed to initialize" or no semantic search results
- Picking or switching an embedding model
- Tuning `searchMinScore`, batch size, or file exclusions
- Confirming an existing setup still works after an IDE or host restart

**When NOT to use:**

- You have a paid hosted embedding provider (OpenAI, Voyage, etc.) and are happy with per-token cost — this skill is the offline alternative
- You need embeddings generated remotely — Ollama must run on the host where Kilo Code executes
- You need multi-GB corpora with sub-100ms retrieval — LanceDB is fine for typical codebases, not for >1M blocks

## Quick Reference

| Task | Command / Action |
|---|---|
| Install (Debian/Ubuntu) | `sudo apt-get install -y zstd && curl -fsSL https://ollama.ai/install.sh \| sh` |
| Pull embedding model | `ollama pull nomic-embed-text` |
| Start server (containers) | `nohup ollama serve > /tmp/ollama.log 2>&1 &` |
| Verify server | `curl -s 127.0.0.1:11434/api/tags \| jq '.models[].name'` |
| Verify embeddings | `curl -s 127.0.0.1:11434/api/embed -d '{"model":"nomic-embed-text","input":"test"}' \| jq '.embeddings[0]\|length'` → 768 |
| Edit config | `~/.config/kilo/kilo.jsonc` → `indexing.*` keys |
| Exclude files | Project root `.kilocodeignore` (gitignore syntax) |
| Apply config | `/reload` in chat or restart IDE |

## Prerequisites

- Ollama binary at `/usr/local/bin/ollama`
- An embedding model pulled (start with `nomic-embed-text`)
- Reachable server on `http://127.0.0.1:11434`

## Setup (Linux)

```bash
# 1. Install — Debian/Ubuntu needs zstd first
sudo apt-get install -y zstd
curl -fsSL https://ollama.ai/install.sh | sh

# 2. Pull model (~270 MB)
ollama pull nomic-embed-text

# 3. Start server (systemd is often absent in containers/devcontainers)
nohup ollama serve > /tmp/ollama.log 2>&1 &

# 4. Verify
curl -s http://127.0.0.1:11434/api/tags | jq '.models[].name'
curl -s http://127.0.0.1:11434/api/embed \
  -d '{"model":"nomic-embed-text","input":"test"}' | jq '.embeddings[0]|length'
# Expect 768
```

**Use `nohup … &` in containers/remote hosts.** The official install creates a systemd unit, which silently no-ops when `systemd` isn't PID 1. Backgrounding with `nohup` works everywhere.

**No `python3`?** Use `jq` for the verification step — installed by default on most dev images.

## Configure Kilo Code

Edit `~/.config/kilo/kilo.jsonc`:

```json
{
  "$schema": "https://app.kilo.ai/config.json",
  "indexing": {
    "enabled": true,
    "provider": "ollama",
    "model": "nomic-embed-text",
    "vectorStore": "lancedb",
    "ollama": { "baseUrl": "http://127.0.0.1:11434" },
    "lancedb": {},
    "searchMinScore": 0.4,
    "searchMaxResults": 50,
    "embeddingBatchSize": 60,
    "scannerMaxBatchRetries": 3
  }
}
```

Reload with `/reload` in chat or restart the IDE — config changes don't apply live.

## Excluding Files

Project root `.kilocodeignore` (same syntax as `.gitignore`):

```
build/
.gradle/
.idea/
local.properties
signing.properties
*.jks
*.keystore
captures/
.externalNativeBuild/
.cxx/
```

Or restrict to specific extensions in `kilo.jsonc`:

```json
"indexing": { "fileExtensions": [".kt", ".java", ".xml"] }
```

## Embedding Models

| Model | Size | Dim | Use when |
|---|---|---|---|
| `nomic-embed-text` | 274 MB | 768 | Default — best balance for code |
| `mxbai-embed-large` | 669 MB | 1024 | Higher quality, slower indexing |
| `all-minilm` | 46 MB | 384 | Fastest, lower recall |

Switching models requires re-indexing; change `model` in `kilo.jsonc` and reload.

## Common Mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `idx failed to initialize` | Ollama unreachable | `curl 127.0.0.1:11434/api/tags` → start with `nohup ollama serve` if 000 |
| Install fails: "requires zstd" | Missing system lib | `apt-get install -y zstd`, then reinstall |
| Install warns "systemd is not running" | No init in container | Expected — fall through to `nohup ollama serve` |
| `ollama list` empty | Model not pulled | `ollama pull nomic-embed-text` |
| Embeddings 0-dim / error | Wrong model name | Match `model` in config exactly: `nomic-embed-text` (no tag) |
| `semantic_search` returns nothing | Indexing not finished, or score too high | Wait for status indicator; try `searchMinScore: 0.3` |
| Indexing stalls | Batch too large for hardware | Lower `embeddingBatchSize` (e.g. 20); `tail -f /tmp/ollama.log` |
| VS Code extension can't see Ollama | Running in remote container | Ollama must be on the host the IDE runs on, not the container |
| Config change has no effect | Not reloaded | `/reload` in chat or restart IDE |
| Re-pulled model didn't help search | Old index still in use | Delete `.kilocode/` index dir or trigger re-index from settings |

## Verify It Works

Natural-language queries that should return results once indexing completes:

- "How is user authentication handled?"
- "Database connection setup"
- "Error handling patterns"
- "API endpoint definitions"

Results include file path, line number, similarity score. If returns are empty, confirm the indexer finished (status indicator in Kilo Code) before lowering the score threshold.
