#!/usr/bin/env python3
"""Call Claude API to classify upstream diff files as AUTO / MANUAL / FORK."""
import os
import json
import sys
import urllib.request
import urllib.error

api_key = os.environ.get("ANTHROPIC_API_KEY", "")
if not api_key:
    print("(ANTHROPIC_API_KEY not configured — add it as a repo secret to enable automatic analysis)")
    sys.exit(0)

commits = open("/tmp/commits.txt").read().strip()
stat = open("/tmp/stat.txt").read().strip()

prompt = (
    "Eres un asistente analizando cambios de upstream google-ai-edge/gallery "
    "para mergear en un fork.\n\n"
    "El fork añade: HTTP bridge (service/, LlmHttpService.kt), tests en src/test/, "
    "docs/, mobile-actions/, tools/ y .github/workflows/jvm-tests.yml. "
    "Estos archivos son exclusivos del fork y NO deben sobreescribirse.\n\n"
    f"Nuevos commits upstream:\n{commits}\n\n"
    f"Archivos que difieren entre fork HEAD y upstream/main:\n{stat}\n\n"
    "Para cada archivo modificado, clasifica:\n"
    "- AUTO: se puede hacer checkout directo desde upstream (el fork no tiene contenido propio aquí)\n"
    "- MANUAL: requiere merge cuidadoso (tanto upstream como el fork tienen cambios)\n"
    "- FORK: archivo exclusivo del fork, ignorar versión upstream\n\n"
    "Sé conciso. Máximo 400 palabras."
)

payload = json.dumps({
    "model": "claude-haiku-4-5-20251001",
    "max_tokens": 1024,
    "messages": [{"role": "user", "content": prompt}],
}).encode()

req = urllib.request.Request(
    "https://api.anthropic.com/v1/messages",
    data=payload,
    headers={
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    },
)

try:
    resp = json.loads(urllib.request.urlopen(req).read())
    print(resp["content"][0]["text"])
except urllib.error.HTTPError as e:
    print(f"API error {e.code}: {e.read().decode()}", file=sys.stderr)
    print("(Analysis unavailable)")
