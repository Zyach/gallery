# Gallery Checkpoint

Fecha: 2026-01-20

## Estado actual
- Repo base: `~/repositories/gallery` (fork de Google AI Edge Gallery) con bridge HTTP local activo.
- Bridge HTTP: `LlmHttpService` expone `/generate`, `/v1/chat/completions`, `/v1/responses` para inferencia local.
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

## Hitos completados
1) Catálogo completo de comandos `termux-*` (help extraído).  
2) Esquema OpenAI tools para Termux (`termux_tools_openai.json`).  
3) Esquema OpenAI tools para Proot (`exec_proot`).  
4) Dataset Premium expandido (variaciones + confirmaciones + ambigüedad).  
5) Dataset combinado con Mobile Actions original, con balanceo.  
6) Estadísticas y muestras de verificación.

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
