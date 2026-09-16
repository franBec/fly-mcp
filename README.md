# fly-mcp

A MaleCNS v1.0 fly brain behind MCP tools. Ask the fly a question, it reads its
connectome, and a verdict comes back. The server speaks remote Streamable-HTTP
MCP, so any agent that supports remote MCP can consult it.

The brain is the MaleCNS v1.0 connectome that Google Research and HHMI Janelia
released in September 2026, run through [Stonkfly](https://github.com/nftechie/stonkfly)
(MIT) at a pinned commit. The decoder, the frames, and the reinforcement signals
are engineered. None of it is cognition.

## Honesty, read before sharing any result

- The connectome weights are anatomy, not a living fly.
- The verdict is Stonkfly's DNp20 left/right spike differential with a DNpe017
  gate. It is an engineered mapping, not a discovered decision circuit.
- `reward_fly` and `punish_fly` queue engineered pulses (15 PAM11 dopaminergic
  cells, 2 PPL101 aversive cells). They are not modeled pleasure or pain.
- Frames are light-background 320x180 RGB renders of your text. That is a
  display adapter, not retinal physiology. Dark frames barely activate the
  connectome, which is why the background stays light.
- The likely outcome is a HOLD. That is a valid, honest result.

Every tool description carries the same line: engineered readout on spike data,
comedy oracle, not intelligence.

## The MCP surface

Tools:

- `ask_fly(text)`: renders the text as a frame and returns the verdict
  (BUY/SELL/HOLD) plus raw spike stats.
- `reward_fly(reason)`: queues a reward pulse, consumed by the next `ask_fly`,
  not at call time.
- `punish_fly(reason)`: queues an aversive pulse, consumed by the next
  `ask_fly`, not at call time.
- `fly_vitals()`: brain kind, consult count, memory stats.

Resource: `fly://vitals` (JSON). Prompt: `second-opinion(question, context?)`.

## Layout

```
src/        Spring Boot 4.1 + Spring AI 2.0.1 MCP server (Java 25)
oracle/     FastAPI sidecar wrapping Stonkfly's stonkfly.neural package
infra/      Terraform: one disposable GCP VM, firewall from one source CIDR
compose.yml prepare -> oracle -> warmup -> mcp
```

The MCP server has two backends. `MockBrainBackend` is the default when
`FLYBRAIN_BASE_URL` is unset: deterministic scores from frame bytes plus a call
counter, no clocks, no randomness. `HttpBrainBackend` calls the oracle when
`FLYBRAIN_BASE_URL` points at it.

## Run locally with the mock brain

No dataset, no GCP, no tokens.

```bash
FLY_BRAIN=mock docker compose up -d --build
```

Then list the surface:

```bash
npx -y @modelcontextprotocol/inspector --cli http://localhost:8080/mcp --method tools/list
npx -y @modelcontextprotocol/inspector --cli http://localhost:8080/mcp --method tools/call \
  --tool-name ask_fly --tool-arg 'text=Does this thing work?'
```

The repo ships `opencode.json` pointing `fly-mcp` at `http://localhost:8080/mcp`.
Run `opencode mcp list` from the repo root and the server shows as connected.

Java only loop:

```bash
./gradlew test          # renderer, mock backend, HTTP backend, MCP surface
./gradlew bootRun       # starts with the mock backend on :8080
```

## Deploy to GCP

Prerequisites: a GCP project with billing enabled, `gcloud` authenticated, and
Terraform installed. On NixOS without Terraform, fetch the static binary:

```bash
mkdir -p /tmp/tf && curl -fsSL -o /tmp/tf/tf.zip https://releases.hashicorp.com/terraform/1.14.0/terraform_1.14.0_linux_amd64.zip
unzip -o /tmp/tf/tf.zip -d /tmp/tf && mv /tmp/tf/terraform /tmp/terraform && chmod +x /tmp/terraform
```

Apply, passing the one CIDR that may reach port 8080:

```bash
cd infra
terraform init
terraform apply -var="allowed_source_cidr=$(curl -4 -s ifconfig.me)/32"
```

The VM installs Docker, clones this public repo, writes `.env`, and runs
compose. The prepare service downloads the MaleCNS dataset and builds the
graph, which takes 10 to 30 minutes on first boot. The warmup service then
runs one throwaway consult so the connectome and the LIF kernel load before the
MCP server starts. Watch progress over SSH:

```bash
gcloud compute ssh fly-mcp --zone=europe-west4-a --command='sudo docker compose -f /opt/fly-mcp/compose.yml ps'
```

Register the server in opencode and check it:

```bash
opencode mcp add fly-mcp --url "http://$(terraform output -raw instance_ip):8080/mcp"
opencode mcp list
opencode mcp debug fly-mcp
```

Destroy everything when the window closes:

```bash
terraform destroy
```

Cost: an on-demand `e2-highmem-4` in `europe-west4` runs about $0.20 per hour
plus disk. A few hours stays under $1. The MCP endpoint is unauthenticated, by
design for this POC. The firewall source CIDR is the only access control, so
keep the window in minutes and destroy when done.

If the connection is refused, check two things first: whether the laptop dialed
IPv6 while the rule is IPv4, and whether the public IP changed since the apply.

## Backlog, not in scope

Spring Security bearer auth, TLS with a domain, a second remote consumer, and a
stdio transport. The POC runs one transport only: remote Streamable-HTTP.

## Credits

- [Stonkfly](https://github.com/nftechie/stonkfly) (MIT): the MaleCNS v1.0
  event-driven LIF kernel, visual adapter, decoder, and reinforcement
  scaffolding. Vendored at commit `78ef3e05ab0fa086032098558d893667068944a0`.
- [MaleCNS v1.0](https://male-cns.janelia.org/): Google Research and HHMI
  Janelia, open connectome, source files downloaded under the upstream license.
- [Spring AI](https://docs.spring.io/spring-ai/reference/): MCP server boot
  starter and annotations.

Nothing here is neuroscience research or investment advice.
