# Gallery Checkpoint

Fecha: 2026-01-20

## Estado actual
- Repo base: `~/repositories/gallery` (fork de Google AI Edge Gallery) con bridge HTTP local controlado por Settings.
- Bridge HTTP: `LlmHttpService` expone `/generate`, `/v1/chat/completions`, `/v1/responses` para inferencia local.
- Bridge HTTP solo arranca si el toggle está activado; puerto configurable desde Settings.
- Toggle de historial de chat persiste en `LlmHttpPrefs` desde Settings.
- Toggle de logging de payload HTTP añadido en Settings (logs a archivo cuando está activo).
- LlmHttpService: logging estructurado (request_start/done/error/cancel), límites de payload, timeout con cancel y reset de conversación.
- Heurística de low‑memory ajustada (umbral dinámico) para reducir fallback prematuro a CPU.
- Allowlists versionados actualizados para usar GPU por defecto (orden `gpu,cpu`).
- Dataset Termux/Proot creado y combinado con Mobile Actions original.
- Esquemas OpenAI tools listos para Termux y Proot.
- Recomendaciones aplicadas: tools unificados y balanceo del dataset Premium.

## Artefactos generados (no versionados)
- Carpeta `mobile-actions/` dentro del repo para dataset y esquemas.
  - `termux_commands_catalog.md` / `.json`
  - `termux_tools_openai.json`
  - `proot_tools_openai.json`
  - `premium_dataset.jsonl` (expandido con variaciones, confirmaciones y casos ambiguos)
  - `combined_dataset.jsonl`
  - `dataset_stats.json`, `dataset_samples.md`
- Nota: `mobile-actions/` está en `.gitignore`.
- Diagnósticos locales (no versionados):
  - `tools/edgegallery_diag.sh` y `tools/edgegallery_analyze.py`.
  - Runs en `/sdcard/Download/edgegallery/runs/<id>` con `logcat_llm.txt`, `summary.json`, `llm_http.log`.

## Hitos completados
1) Catálogo completo de comandos `termux-*` (help extraído).  
2) Esquema OpenAI tools para Termux (`termux_tools_openai.json`).  
3) Esquema OpenAI tools para Proot (`exec_proot`).  
4) Dataset Premium expandido (variaciones + confirmaciones + ambigüedad).  
5) Dataset combinado con Mobile Actions original, con balanceo.  
6) Estadísticas y muestras de verificación.
7) Fix: Settings guarda el toggle de historial de chat en `LlmHttpPrefs`.
8) GPU por defecto en allowlists (`model_allowlist.json` y `model_allowlists/1_0_4..1_0_9.json`).
9) Logging DEBUG de payloads en `LlmHttpService` para auditar prompts entrantes.
10) Toggle de logging de payloads HTTP en Settings + log a archivo (`llm_http.log`).
11) Bridge HTTP arranca solo si está habilitado (evita overhead al inicio).
12) Diagnóstico local automatizado (`edgegallery_diag.sh` + `edgegallery_analyze.py`).
13) Heurística de low‑memory ajustada para reducir fallback prematuro a CPU.

## Próximos pasos (hoja de ruta)
1) Ajustar casos sensibles y confirmaciones por comando (policy real).  
2) Definir allow-list operacional (Termux + Proot) para ejecución segura.  
3) Integrar tool_calls en `LlmHttpService` (respuesta OpenAI-compatible).  
4) Validar en Every Code el gateway de tools (parseo + ejecución + confirmación).  
5) Entrenar/finetunar modelo (FunctionGemma o equivalente) con dataset combinado.  
6) Pruebas E2E: Termux‑API + Proot + flujo HTTP completo.  
7) Medición de precisión de tool_calls y tasa de rechazo correcta.

## Notas de seguridad
- Acciones destructivas requieren confirmación explícita.
- `exec_proot` debe pasar por allow‑list y validación de argumentos.
- Si falla la policy o el parsing, fallback a texto.
