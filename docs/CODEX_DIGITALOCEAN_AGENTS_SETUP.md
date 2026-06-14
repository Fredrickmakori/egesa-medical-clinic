# Codex + DigitalOcean Agents Setup

This project is a Kotlin Multiplatform HIMS with a Ktor backend in `server`, shared clinical/domain code in `shared`, and Android/Desktop clients. Use DigitalOcean with Codex in three separate layers:

1. DigitalOcean Inference can be the model backend for future Codex sessions.
2. DigitalOcean MCP can let Codex manage App Platform apps and managed databases.
3. DigitalOcean ADK agents can be called by code or scripts as HTTP services.

## 1. Secrets

Do not commit DigitalOcean tokens. Set them in PowerShell before starting Codex:

```powershell
$env:DIGITALOCEAN_API_TOKEN = "dop_v1_..."
$env:MODEL_ACCESS_KEY = "sk-do-..."
$env:EGESA_DO_AGENT_URL = "https://agents.do-ai.run/v1/your-agent/development/run"
```

Use a model access key only for inference. Use a DigitalOcean API token for MCP, App Platform, databases, and ADK deploys.

## 2. Codex Inference Backend

Add this to `C:\Users\USER\.codex\config.toml` when you want a future Codex session to use DigitalOcean Inference:

```toml
model_provider = "openai_custom"
model = "openai-gpt-4.1-mini"
model_reasoning_effort = "high"
web_search = "disabled"

[providers.openai_custom]
base_url = "https://inference.do-ai.run/v1"
env_key = "MODEL_ACCESS_KEY"
```

Restart Codex after changing the config. This affects new sessions, not the model already running this thread.

## 3. DigitalOcean MCP

The repo-level `.mcp.json` includes:

```json
{
  "mcpServers": {
    "digitalocean": {
      "command": "npx",
      "args": ["-y", "@digitalocean/mcp", "--services", "apps,databases"]
    }
  }
}
```

Start Codex from this repo after setting `DIGITALOCEAN_API_TOKEN`. The MCP process should inherit the environment variable. Ask Codex to confirm before creating or deleting cloud resources.

## 4. ADK Agent Usage

Keep PHI out of agent prompts. Send only de-identified workflow, schema, or clinical logic questions.

The local ADK agent is configured as a HIMS builder agent. Its shared operating prompt is documented in `docs/LLM_HIMS_BUILDER_PROMPT.md`.

Your local ADK server is currently:

```text
http://127.0.0.1:8080/run
```

The HIMS Ktor backend now exposes a protected proxy endpoint:

```text
POST /agent/support
Authorization: Bearer <HIMS_JWT>
Body: {"prompt":"<de-identified support question>"}
```

The proxy reads `EGESA_DO_AGENT_URL` and defaults to `http://127.0.0.1:8080/run`. Because the ADK agent and HIMS backend both default to port `8080`, run one of them on a different port during local development. For example:

```powershell
$env:EGESA_DO_AGENT_URL = "http://127.0.0.1:8080/run"
$env:PORT = "8081"
```

Example Codex prompt:

```text
Use my DigitalOcean ADK agent from EGESA_DO_AGENT_URL for de-identified clinical workflow reasoning. Call it with POST JSON {"prompt":"<question>"} and Authorization: Bearer from DIGITALOCEAN_API_TOKEN. Do not send patient names, phone numbers, IDs, addresses, or raw clinical notes.
```

Example PowerShell smoke test:

```powershell
$body = @{ prompt = "Summarize a safe outpatient triage workflow without PHI." } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri $env:EGESA_DO_AGENT_URL `
  -Headers @{ Authorization = "Bearer $env:DIGITALOCEAN_API_TOKEN" } `
  -ContentType "application/json" `
  -Body $body
```

## 5. Deployment Prompt For This Repo

After the DigitalOcean MCP server is visible in Codex, use this:

```text
Use the DigitalOcean MCP tools for apps and databases to prepare deployment for Egesa HIMS from this Kotlin Multiplatform repo.

Before creating any cloud resources, show me the proposed App Platform spec, database plan, estimated monthly cost, and environment variables.

Target backend:
- Ktor server module: server
- Build command: ./gradlew :server:installDist -x test
- Run command: ./server/build/install/server/bin/server
- Health check: /health
- Port: 8080

Provision a managed PostgreSQL database and connect it through DATABASE_URL. Configure JWT_SECRET, ENVIRONMENT=production, SUPABASE_URL, and SUPABASE_ANON_KEY as app environment variables. Do not print secret values. After I approve, create staging first, deploy, fetch logs, call /health, then propose production.
```
