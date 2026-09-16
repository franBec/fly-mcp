# fly-mcp handoff

## Mission

Expose a fly brain as MCP tools for AI agents and prove the stack on
disposable GCP infrastructure: the MaleCNS oracle plus the MCP server on one
VM, provisioned by Terraform in this repo. One transport only, remote
Streamable-HTTP. First consumer: opencode.

## Definition of done

1. `terraform apply -var="allowed_source_cidr=$(curl -4 -s ifconfig.me)/32"`
   creates the VM and the stack boots: dataset prepare, oracle, warmup, MCP
   server, healthchecks green.
2. `opencode mcp list` shows `fly-mcp` as a remote server at
   `http://<ephemeral-ip>:8080/mcp`.
3. An opencode session calls `fly-mcp_ask_fly` and gets a real-backend verdict.
4. `terraform destroy` removes everything. Exposure window: minutes. Total
   cost: under $1.

## Stack

Java 25, Spring Boot 4.1.x, Spring AI 2.0.1, Gradle, package
`dev.pollito.flymcp`.

- MCP transport: `spring-ai-starter-mcp-server-webmvc` with
  `spring.ai.mcp.server.protocol=STREAMABLE`, endpoint `POST /mcp`. The app
  runs on the servlet stack.
- Annotations auto-register: `@McpTool`, `@McpToolParam`, `@McpResource`,
  `@McpPrompt`, `@McpArg`. `spring.ai.mcp.server.type=SYNC` registers
  blocking methods only, so tools stay blocking.
- `spring-boot-starter-webflux` is on the classpath for its WebClient; the
  app explicitly forces the servlet web application type.

## Architecture

- `BrainBackend`: `consult(byte[] png, Reinforcement r)`, `reward(String)`,
  `punish(String)`, `vitals()`, `report()`.
- `MockBrainBackend` (default when `flybrain.base-url` is unset): deterministic
  scores from frame bytes plus a call counter. No clocks, no randomness.
- `HttpBrainBackend`: WebClient against `flybrain.base-url`, 90 second
  timeouts. Real consults average about 5 seconds.
- `FrameRenderer`: renders text into a light-background 320x180 PNG. Keep
  backgrounds light; dark input is a salience pitfall for the real backend.
- MCP surface: `ask_fly`, `reward_fly`, `punish_fly`, `fly_vitals`, resource
  `fly://vitals`, prompt `second-opinion`. Every tool description carries one
  honesty line.

## Oracle contract

```
GET  /vitals   -> {"brain_kind": ..., "consult_count": ..., "memory": {...}}
POST /consult  {"png_b64": "..."} -> {"side": ..., "left_hz": ..., "right_hz": ..., "difference_hz": ..., "gate_spikes": ..., "approach_hz": ...}
POST /reward   -> {"queued": "reward"}
POST /punish   -> {"queued": "aversive"}
GET  /health   -> {"status": "ok"}
```

`FLY_BRAIN=real|mock`. The real backend is Stonkfly's MaleCNS v1.0 connectome,
vendored in the oracle image at pinned commit
`78ef3e05ab0fa086032098558d893667068944a0`, MIT. The mock backend exists so the
compose stack runs with no dataset.

## Local development flow

1. `./gradlew build` and `./gradlew test` are green (mock backend, no infra).
2. `FLY_BRAIN=mock docker compose up -d --build` brings prepare (no-op),
   oracle, warmup, and the MCP server.
3. `npx @modelcontextprotocol/inspector` against `http://localhost:8080/mcp`
   lists every tool, resource, and prompt and can call them.
4. `opencode mcp list` from the repo root shows the committed `opencode.json`
   registration as connected.

## Deployment

- On-demand `e2-highmem-4` in `europe-west4`, Debian 12, 25GB boot disk, 4GB
  swap. Spot risks preemption mid-demo.
- Firewall: allow `tcp:8080` from `var.allowed_source_cidr` only. Port 8000
  stays internal to the Docker network. If the laptop public IP changes, update
  the rule or re-apply.
- Startup script: install Docker, clone this public repo over HTTPS, write
  `.env`, `docker compose up -d --build`.
- Compose services: `prepare` (one-shot dataset), `oracle` (healthcheck
  `/health`), `warmup` (one throwaway consult so the brain and kernel load),
  `mcp` (depends on warmup success, port 8080 published).
- Ephemeral external IP. `terraform destroy` at the end.

## Phases

1. Repo and scaffold. `./gradlew build` green, jar exists.
2. Full MCP surface plus tests. Inspector lists everything against a locally
   running jar. Tests cover renderer output, mock determinism, the HTTP backend
   against MockWebServer, and annotation registration.
3. Vendored oracle plus compose. `docker compose up` with the mock brain serves
   the contract and the MCP server returns backend data end to end.
4. Terraform plus POC deploy. Remote opencode session calls `ask_fly` against
   the real backend, `opencode mcp debug fly-mcp` shows healthy, then destroy.
5. Backlog, not in scope: Spring Security bearer, TLS with a domain, second
   remote consumer, PR reviewer, stdio transport.

## Gotchas

- A SYNC server ignores async annotated methods.
- The mock backend must stay deterministic; it is the test contract.
- `reward_fly` and `punish_fly` queue pulses consumed by the next consult, not
  events. The descriptions say so.
- The real brain's first boot is slow (dataset plus kernel compile, 10 to 30
  minutes). The warmup service absorbs this before the MCP server starts;
  watch `/health` and the warmup logs before opening the demo window.
- opencode fetches tools with a 5 second timeout by default; tool calls with
  about 5 second latencies need verification. If opencode aborts long calls,
  reduce work per consult.
- IP-restricted firewall: if the connection is refused, first check whether the
  laptop dialed IPv6 while the rule is IPv4, then check whether the public IP
  changed.
- Unauthenticated exposure is accepted for minutes only. Keep it short, then
  destroy.
- Java 25 needs no preview flags on Boot 4.1.
