#!/bin/bash
# fly-mcp VM startup script. Idempotent: safe on every boot.

set -euo pipefail

METADATA="http://metadata.google.internal/computeMetadata/v1/instance/attributes"
md() { curl -s -H 'Metadata-Flavor: Google' "${METADATA}/$1" || true; }

REPO_URL="$(md fly-mcp-repo-url)"
REPO_REF="$(md fly-mcp-repo-ref)"
FLY_BRAIN="$(md fly-mcp-fly-brain)"
REPO_URL="${REPO_URL:-https://github.com/franBec/fly-mcp.git}"
REPO_REF="${REPO_REF:-main}"
FLY_BRAIN="${FLY_BRAIN:-real}"

# --- docker (official convenience script; idempotent) ---
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
fi

# --- git for the repo clone ---
if ! command -v git >/dev/null 2>&1; then
  apt-get update
  apt-get install -y --no-install-recommends git
fi

# --- small swap (the kernel can spike past 12GB briefly) ---
if [ ! -f /swapfile ]; then
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# --- clone / update repo ---
mkdir -p /opt/fly-mcp
if [ ! -d /opt/fly-mcp/.git ]; then
  git clone "$REPO_URL" /opt/fly-mcp
fi
git -C /opt/fly-mcp fetch origin
git -C /opt/fly-mcp checkout "$REPO_REF"
git -C /opt/fly-mcp pull --ff-only origin "$REPO_REF" || true

# --- .env (never committed) ---
printf 'FLY_BRAIN=%s\n' "$FLY_BRAIN" > /opt/fly-mcp/.env
chmod 600 /opt/fly-mcp/.env

# --- bring the stack up (prepare -> oracle -> warmup -> mcp) ---
cd /opt/fly-mcp
docker compose up -d --build

echo "fly-mcp startup complete: $(date)" >> /var/log/fly-mcp-startup.log
