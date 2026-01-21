# Gallery Checkpoint – 2026-01-21 (update)

## Estado del repo
- Rama: `main`
- Cambios sin commit:
  - `Android/src/app/src/main/assets/model_allowlist.json` (sin track)

## Contexto reciente
- HTTP bridge expuesto en `127.0.0.1:9006`.
- `/v1/models` devuelve 7 modelos, incluye `FuncionGemma-270M` desde allowlist modificada en dispositivo.
- `/v1/chat/completions` con modelo por defecto (`Gemma-3n-E2B-it`) funciona (texto). Con `tools` responde texto (no emite `tool_calls`).
- FunctionGemma (`FuncionGemma-270M`/`TinyGarden-270M`) aún sin soporte: timeouts/llm error si se fuerza modelo.
- `/ping` OK.

## Work-in-progress / Riesgos
- Integración de tool_calls: DTOs añadidos, pero no se fuerza tool_call; modelos de función no soportados aún.
- Allowlist en assets fue modificada localmente (no trackeada en git) y también en dispositivo.

## Próximos pasos sugeridos
1) Decidir estrategia para FunctionGemma: bypass de tool_call sintético o runtime específico.
2) Definir heurística de `tool_choice` (auto/required) y, si aplica, activar streaming de tool_calls.
3) Añadir tests/manual script de validación HTTP.
