"""One-shot warmup: force the brain to load during stack boot.

The real connectome graph and LIF kernel compile take minutes on first use.
Running one throwaway consult here means the first user ask_fly call is a
normal-speed consult instead of a cold start. Exits non-zero if the brain
does not answer, so compose keeps the MCP server from starting unwarmed.
"""

import base64
import io
import json
import os
import sys
import urllib.request

from PIL import Image

FRAME_W, FRAME_H = 320, 180
LIGHT_BG = (235, 240, 249)


def main():
    url = os.environ.get("ORACLE_URL", "http://oracle:8000").rstrip("/") + "/consult"
    image = Image.new("RGB", (FRAME_W, FRAME_H), LIGHT_BG)
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    body = json.dumps({"png_b64": base64.b64encode(buffer.getvalue()).decode("ascii")}).encode("utf-8")
    request = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=1800) as response:
            data = json.load(response)
    except Exception as exc:
        print(f"warmup failed: {type(exc).__name__}: {exc}", flush=True)
        return 1
    print(
        "warmup ok: side=%s brain_kind=%s consults=%s"
        % (data.get("side"), data.get("brain_kind"), data.get("oracle_consult")),
        flush=True,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
