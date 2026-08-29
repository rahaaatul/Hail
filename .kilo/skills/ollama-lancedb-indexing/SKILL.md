---
name: ollama-lancedb-indexing
description: Setup and troubleshoot Ollama + LanceDB codebase indexing for semantic code search. Use when the user asks about indexing, semantic search, or codebase exploration.
---

# Ollama + LanceDB Codebase Indexing

This skill provides free, local codebase indexing using Ollama for embeddings and LanceDB as the vector store. No API keys or per-token fees required.

## Architecture

Kilo Code connects to a locally running Ollama server to generate embeddings for code blocks, then stores those embeddings in LanceDB (an embedded vector store that requires no separate server). This enables semantic search across the entire codebase.

The indexing pipeline works as follows: Kilo Code parses the code using Tree-sitter to identify semantic blocks (functions, classes, methods), sends each block to the Ollama embedding API, stores the resulting vectors in LanceDB, and provides the `semantic_search` tool for natural language queries.

## Prerequisites

- Ollama installed and running locally
- An embedding model pulled (nomic-embed-text recommended)
- Kilo Code extension with indexing enabled

## Initial Setup

Install Ollama on your local machine:

```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

Pull an embedding model. The `nomic-embed-text` model provides 768-dimensional embeddings and works well for code:

```bash
ollama pull nomic-embed-text
```

Start the Ollama server (if not already running):

```bash
ollama serve
```

Verify the server is accessible:

```bash
curl http://127.0.0.1:11434/api/tags
```

Test that embeddings work:

```bash
curl -s http://127.0.0.1:11434/api/embed -d '{"model":"nomic-embed-text","input":"test code"}' | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Dimensions: {len(d[\"embeddings\"][0])}')"
```

## Configuring Kilo Code

Open Kilo Code Settings and navigate to Indexing. Configure as follows:

- **Global Enable** or **Enable for This Project**: Turn on
- **Embedding Provider**: Ollama
- **Vector Store**: LanceDB (default, embedded, no server needed)
- **Ollama Base URL**: `http://127.0.0.1:11434`

Or edit `~/.config/kilo/kilo.jsonc` directly:

```json
{
  "indexing": {
    "enabled": true,
    "provider": "ollama",
    "model": "nomic-embed-text",
    "vectorStore": "lancedb",
    "ollama": {
      "baseUrl": "http://127.0.0.1:11434"
    },
    "lancedb": {},
    "searchMinScore": 0.4,
    "searchMaxResults": 50,
    "embeddingBatchSize": 60,
    "scannerMaxBatchRetries": 3
  }
}
```

## Troubleshooting

### Index shows "idx failed to initialize"

This means Kilo Code cannot reach the Ollama server. Check:

1. Is Ollama running? Run `ollama serve` if not
2. Is it accessible at `http://127.0.0.1:11434`? Test with `curl http://127.0.0.1:11434/api/tags`
3. Is the embedding model pulled? Check with `ollama list`
4. If Kilo Code runs as a VS Code extension, Ollama must be on your local machine, not a remote container

### Semantic search returns no results

1. Wait for indexing to complete (check status indicator in Kilo Code)
2. Verify the search query is in natural language, not exact keywords
3. Try lowering `searchMinScore` to 0.3 for broader results

### Indexing stalls or fails

1. Reduce `embeddingBatchSize` if hitting rate limits (default 60)
2. Check Ollama logs: `cat /tmp/ollama.log`
3. For local Ollama with llama.cpp, ensure batch size (`-b`) matches micro-batch size (`-ub`)

### Reloading after setup

After configuring indexing, reload Kilo Code or start a new session for changes to take effect. Use `/reload` in the chat or restart the IDE.

## Alternative Embedding Models

| Model | Size | Dimensions | Notes |
|---|---|---|---|
| nomic-embed-text | 137M | 768 | Best balance for code, recommended |
| mxbai-embed-large | 334M | 1024 | Higher quality, slower |
| all-minilm | 23M | 384 | Fastest, lower quality |

Switch models by changing the `model` field in config and re-indexing.

## Excluding Files from Indexing

Create a `.kilocodeignore` file at the project root to exclude files (same syntax as `.gitignore`):

```
build/
.gradle/
local.properties
signing.properties
*.jks
```

Or configure `indexing.fileExtensions` in `kilo.jsonc` to index only specific file types:

```json
{
  "indexing": {
    "fileExtensions": [".kt", ".java", ".xml"]
  }
}
```

## Verification

After setup, test semantic search with natural language queries:

- "How is user authentication handled?"
- "Database connection setup"
- "Error handling patterns"
- "API endpoint definitions"

Results include file paths, line numbers, and similarity scores.
