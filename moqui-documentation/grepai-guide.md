# GrepAI Semantic Search & Ollama Guide

## Overview

This guide covers two complementary tools:

| Tool | Purpose | Role |
|------|---------|------|
| **Ollama** | Local LLM/embedding runtime | Provides the embedding model for vector search |
| **GrepAI** | Semantic code search | Uses Ollama embeddings to search code by meaning |

## Ollama

### What is Ollama?

Ollama is a **local runtime for LLMs and embedding models**. It runs entirely on your machine, providing:

- **Privacy**: No data leaves your computer
- **Speed**: No network latency
- **Cost**: Free, no API keys needed
- **Offline**: Works without internet

### Ollama Installation

```bash
# macOS (Homebrew)
brew install ollama

# Linux/macOS (curl)
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# Download from https://ollama.ai/download
```

### Ollama CLI Commands

```bash
# Start Ollama server
ollama serve

# Pull a model
ollama pull <model-name>

# List installed models
ollama list

# Show model info
ollama show nomic-embed-text

# Run a chat model
ollama run llama3

# Remove a model
ollama rm <model-name>
```

### Ollama API

Ollama exposes a REST API at `http://localhost:11434`:

```bash
# Check API
curl http://localhost:11434/api/tags

# Generate embeddings
curl http://localhost:11434/api/embeddings \
  -d '{"model": "nomic-embed-text", "prompt": "hello world"}'

# Chat completion
curl http://localhost:11434/api/chat \
  -d '{"model": "llama3", "messages": [{"role": "user", "content": "hi"}]}'
```

### Embedding Models

| Model | Size | Use Case | Recommended |
|-------|------|----------|-------------|
| `nomic-embed-text` | 274 MB | Code/text embeddings | **Yes** |
| `nomic-embed-text-v2-moe` | 397 MB | Multilingual embeddings | For non-English |
| `all-minilm` | ~50 MB | Minimal footprint | Low resources |

### Ollama Configuration

```bash
# Environment variables
OLLAMA_HOST=0.0.0.0          # Listen address
OLLAMA_PORT=11434             # Port
OLLAMA_MODELS=/path/to/models # Custom model directory

# Start with custom settings
OLLAMA_HOST=0.0.0.0 ollama serve
```

### Ollama Troubleshooting

```bash
# Check if running
ps aux | grep ollama | grep -v grep

# View logs (macOS)
tail -f ~/Library/Logs/Ollama/ollama.log

# Restart Ollama
pkill ollama
open -a Ollama  # macOS
# or
ollama serve
```

---

## GrepAI

### What is GrepAI?

GrepAI is a **semantic code search tool** that uses vector embeddings to find code by meaning.

Unlike `grep` (exact text match):
- `grep "auth"` → finds only "auth" literal
- GrepAI "authentication logic" → finds `handleUserSession`, `login()`, `verifyCredentials()`

### GrepAI Installation

```bash
# Download install script
curl -sSL https://raw.githubusercontent.com/yoanbernabeu/grepai/main/install.sh -o install.sh

# Run installer
chmod +x install.sh && ./install.sh

# Manual install (if sudo unavailable)
mkdir -p ~/.local/bin
curl -L "https://github.com/yoanbernabeu/grepai/releases/latest/download/grepai_darwin_arm64.tar.gz" | tar -xz -C ~/.local/bin/
```

### GrepAI CLI Commands

```bash
# Initialize in project
cd /path/to/project
grepai init

# Start watch daemon (background indexing)
grepai watch &

# Check status
grepai status

# Search
grepai search "your query"

# Show token savings
grepai stats

# Configure for AI agents
grepai agent-setup

# Search for authentication code
grepai search "user authentication flow"

# Limit results
grepai search "error handling" --limit 5

# Filter by path prefix
grepai search "authentication" --path src/handlers/
grepai search "authentication" --path documentation/
grepai search "validation" --path src/middleware/ --limit 10

# JSON output for AI agents (--compact saves ~80% tokens)
grepai search "database queries" --json --compact
```

### GrepAI MCP Server

Start as MCP server for OpenCode/Claude Code:

```bash
grepai mcp-serve
```

### GrepAI Search Examples

```bash
# Basic semantic search
grepai search "database connection pooling"
grepai search "error handling middleware"
grepai search "user authentication flow"

# With file filters
grepai search "form validation" --file-pattern "*.xml"
grepai search "entity operations" --file-pattern "*.groovy"

# With limit
grepai search "service definitions" --limit 10
```

### GrepAI Trace (Call Graph)

Requires RPG (Runtime Program Graph) enabled:

```bash
# Enable RPG in .grepai/config.yaml:
# rpg:
#   enabled: true

# Find callers (who calls this function)
grepai trace callers "createDrawing"

# Find callees (what this function calls)
grepai trace callees "TradeFinanceServices.create#LcDrawing"

# Build call graph
grepai trace graph "processAmendment"
```

---

## Combined Setup: Ollama + GrepAI

### Step-by-Step Installation

```bash
# 1. Install Ollama
brew install ollama  # macOS
# or
curl -fsSL https://ollama.ai/install.sh | sh  # Linux

# 2. Start Ollama
ollama serve

# 3. Pull embedding model (required for GrepAI)
ollama pull nomic-embed-text

# 4. Verify model installed
ollama list
# Output:
# NAME                       ID          SIZE      MODIFIED
# nomic-embed-text:latest    0a109f422b  274 MB    2 minutes ago

# 5. Install GrepAI
curl -sSL https://raw.githubusercontent.com/yoanbernabeu/grepai/main/install.sh | sh

# 6. Initialize in project
cd /Users/me/myprojects/moqui-opencode
grepai init
# Select: ollama, gob storage

# Start the watcher daemon, opens a Bubble Tea monitoring UI by default (foreground mode)
grepai watch

# to force plain text output:
grepai watch --no-ui

# Run the watcher as a background daemon with built-in lifecycle management:
# Start the watcher in background
grepai watch --background

# Check if the watcher is running
grepai watch --status

# Stop the background watcher gracefully
grepai watch --stop

# 7. Start indexing (For CI environments, run a one-time index)
grepai watch &
```

### Verify Everything Works

```bash
# 1. Check Ollama
curl http://localhost:11434/api/tags
# {"models": [{"name": "nomic-embed-text:latest", ...}]}

# 2. Check GrepAI status
grepai status
# Files indexed: 302
# Provider: ollama (nomic-embed-text)
# Watcher: running

# 3. Test search
grepai search "ExecutionContext factory pattern"
```

---

## OpenCode Integration

### MCP Configuration

Add to `~/.config/opencode/opencode.json`:

```json
{
  "mcp": {
    "memory": {
      "type": "local",
      "command": ["npx", "-y", "@whenmoon-afk/memory-mcp"],
      "enabled": true
    },
    "serena": {
      "type": "remote",
      "url": "http://127.0.0.1:9121/mcp",
      "enabled": true
    },
    "grepai": {
      "type": "local",
      "command": ["/Users/me/.local/bin/grepai", "mcp-serve"],
      "enabled": true
    }
  }
}
```

### GrepAI MCP Tools in OpenCode

| Tool | Description |
|------|-------------|
| `grepai_search` | Semantic search with natural language |
| `grepai_trace_callers` | Find functions that call a symbol |
| `grepai_trace_callees` | Find functions called by a symbol |
| `grepai_trace_graph` | Generate call graph visualization |
| `grepai_rpg_search` | RPG-based semantic search |
| `grepai_index_status` | Check index health and stats |

### Example OpenCode Commands

```
# Search for form-list patterns semantically
grepai_search "form list widget pattern Moqui"

# Find all references to ExecutionContext
grepai_search "ExecutionContext usage patterns"

# Trace service dependencies
grepai_trace_callees "moqui.trade.finance.DrawingServices.create#LcDrawing"
```
**Tips for OpenCode:**
No need to explicitly type grepai_search - just prompt naturally! The AI will use it automatically when relevant. For example:
- "Find form-list patterns in TradeFinance"
- "How is entity aggregation used in Moqui?"
- "Show me UI widget examples"
The tool is ready to use whenever you need semantic code search.

---

## Configuration Files

### Ollama Config Location

```bash
# macOS
~/.ollama/

# Linux
~/.ollama/

# Config file
~/.ollama/config.json
```

### GrepAI Config Location

```bash
# Project config
/path/to/project/.grepai/config.yaml

# Global config
~/.grepai/config.yaml
```

### Sample GrepAI Config

```yaml
version: 1

embedder:
  provider: ollama
  model: nomic-embed-text
  endpoint: http://localhost:11434
  dimensions: 768
  parallelism: 0

store:
  backend: gob  # local file storage

chunking:
  size: 512
  overlap: 50

watch:
  debounce_ms: 500

search:
  boost:
    enabled: true
    penalties:
      - pattern: /tests/
        factor: 0.5
      - pattern: /docs/
        factor: 0.6
    bonuses:
      - pattern: /src/
        factor: 1.1

rpg:
  enabled: false  # Enable for call graph analysis
  feature_mode: local
  max_traversal_depth: 3
  llm_provider: ollama
  llm_endpoint: http://localhost:11434/v1

ignore:
  - .git
  - .grepai
  - node_modules
  - vendor
  - build
  - target
  - .gradle
```

---

## Troubleshooting

### Ollama Issues

| Problem | Solution |
|---------|----------|
| `ollama: command not found` | Install Ollama or add to PATH |
| `error: connection refused` | Start `ollama serve` |
| Model not found | `ollama pull <model-name>` |
| Out of memory | Reduce model size or close other apps |

```bash
# Fix PATH issues
export PATH="/Applications/Ollama.app/Contents/MacOS:$PATH"

# Or create symlink
sudo ln -sf /Applications/Ollama.app/Contents/Resources/ollama /usr/local/bin/ollama
```

### GrepAI Issues

| Problem | Solution |
|---------|----------|
| `No results found` | Wait for indexing to complete |
| `Provider error` | Check Ollama is running |
| `Index not updating` | `rm -rf .grepai/index && grepai watch &` |

```bash
# Full reset
cd /path/to/project
pkill grepai
rm -rf .grepai
grepai init
grepai watch &
```

### Common Workflow

```bash
# Start fresh session
open -a Ollama  # Start Ollama app

# Check Ollama running
curl http://localhost:11434/api/tags

# Start GrepAI watch
cd /Users/me/myprojects/moqui-opencode
grepai watch &

# Verify
grepai status

# Search
grepai search "your query"
```

---

## Starting & Checking Commands

### Quick Start Script

Run this to start everything:
```bash
#!/bin/bash
# start-grepai.sh - Start Ollama and GrepAI watcher

# 1. Start Ollama (if not running)
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "Starting Ollama..."
    open -a Ollama 2>/dev/null || /Applications/Ollama.app/Contents/MacOS/ollama serve &
    sleep 3
fi

# 2. Start GrepAI watcher
cd /Users/me/myprojects/moqui-opencode
pkill -f "grepai watch" 2>/dev/null || true
nohup ~/.local/bin/grepai watch > ~/.grepai_watch.log 2>&1 &
sleep 2

# 3. Verify
echo "=== GrepAI Status ==="
~/.local/bin/grepai status
echo ""
echo "=== GrepAI Watch Log ==="
tail -5 ~/.grepai_watch.log
```

### Check if GrepAI is Running

```bash
# Method 1: Check process
ps aux | grep "grepai watch" | grep -v grep

# Method 2: Check status
~/.local/bin/grepai status

# Method 3: Check log
tail -10 ~/.grepai_watch.log
```

### Expected Status Output

```
grepai index status
Files indexed: 303
Total chunks: 1911
Index size: 11.9 MB
Last updated: 2026-03-19 06:44:43
Provider: ollama (nomic-embed-text)
Watcher: running  ← This should say "running"
```

### Stop GrepAI Watcher

```bash
# Stop process
pkill -f "grepai watch"

# Or use grepai command
~/.local/bin/grepai watch --stop
```

---

## Auto-Start on Mac

### Option 1: LaunchAgent (Recommended)

A LaunchAgent is set up to auto-start GrepAI when you log in:

```bash
# Location: ~/Library/LaunchAgents/com.grepai.moqui-opencode.plist
# Status
launchctl list | grep grepai

# Unload (to stop auto-start)
launchctl unload ~/Library/LaunchAgents/com.grepai.moqui-opencode.plist

# Reload
launchctl load ~/Library/LaunchAgents/com.grepai.moqui-opencode.plist
```

### Option 2: Manual Script

Use the startup script in the project:

```bash
# Run this to start everything
./start-grepai.sh

# Or manually
cd /Users/me/myprojects/moqui-opencode
./start-grepai.sh
```

---

## How GrepAI Indexing Works

### Architecture

```
+------------------+     +------------------+     +------------------+
|   Initial Scan   | --> |   File Watcher   | --> |  Index Update    |
|   (full index)   |     |   (fsnotify)     |     |  (incremental)   |
+------------------+     +------------------+     +------------------+
                                |
                                v
                        +------------------+
                        |   Debouncing     |
                        |   (500ms)        |
                        +------------------+
```

### Index Process

1. **Initial Scan**: Indexes all files on first run
2. **File Watching**: Monitors filesystem for changes (create, modify, delete)
3. **Debouncing**: Batches rapid changes (500ms delay)
4. **Incremental Update**: Only re-indexes changed files
5. **Embedding**: Generates vector embeddings via Ollama

### What Gets Indexed

| Language | Extensions |
|----------|-----------|
| Java | `.java` |
| Groovy | `.groovy` |
| XML | `.xml` |
| JavaScript | `.js`, `.jsx`, `.ts`, `.tsx` |
| Python | `.py` |
| Go | `.go` |

### Chunking Configuration

```yaml
chunking:
  size: 512      # tokens per chunk
  overlap: 50     # overlap between chunks
```

---

## Troubleshooting

### Performance

1. **First index takes time** - Initial indexing runs embedding for all files
2. **Watch daemon** keeps index updated - Keep it running
3. **RPG disabled by default** - Enable only if you need call graphs (memory intensive)

### Best Practices

1. **Use natural language**: "form validation" not "validateForm"
2. **Be specific**: "Letter of Credit amendment workflow" not "LC"
3. **Start with high-level**: "entity CRUD" then narrow to "create#Trade"
4. **Check status**: `grepai status` before heavy searches

### Security & Privacy

- All processing is **100% local**
- Code never leaves your machine
- No cloud APIs or external services required
- Works offline after initial setup

---

## Resources

- [Ollama Website](https://ollama.ai)
- [GrepAI GitHub](https://github.com/yoanbernabeu/grepai)
- [GrepAI Documentation](https://yoanbernabeu.github.io/grepai/)
- [Ollama Library](https://ollama.com/library)
- [Nomic Embed Text](https://ollama.com/library/nomic-embed-text)
