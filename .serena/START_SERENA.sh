#!/bin/bash
# Start Serena MCP Server for moqui-opencode project

# Set Docker PATH
export PATH="$PATH:/Applications/Docker.app/Contents/Resources/bin"

# Stop any existing Serena container
docker stop serena 2>/dev/null

# Start Serena with proper volume mappings
docker run -d --rm \
  --name serena-opencode \
  -p 9121:9121 \
  -p 24282:24282 \
  -v /Users/me/myprojects:/workspaces/projects:ro \
  -v /Users/me/myprojects/.serena:/workspaces/projects/.serena \
  -v /Users/me/myprojects/moqui-opencode/.serena:/workspaces/projects/moqui-opencode/.serena \
  -v /Users/me/myprojects/moqui-opencode/.gradle:/workspaces/projects/moqui-opencode/.gradle \
  -v /Users/me/.serena-local:/workspaces/serena/config \
  ghcr.io/oraios/serena:latest \
  serena start-mcp-server --context ide --transport streamable-http --port 9121

echo "Serena container started"
echo "Dashboard: http://127.0.0.1:24282/dashboard"
echo "MCP Server: http://127.0.0.1:9121/mcp"
