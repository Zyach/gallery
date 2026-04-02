# ROADMAP — gallery fork

> Ultima actualizacion: 2026-04-03.

## Objetivo

Converger con upstream manteniendo el bridge HTTP local, Tiny Garden y las mejoras de calidad propias del fork.

## Tracks

### 1. Bridge HTTP — en progreso

SSE rendering e inference gateway ya extraídos. Quedan handlers HTTP y logging.

### 2. Upstream sync — pendiente

Adoptar Agent Chat/Skills, thinking nativo (`message.channels["thought"]`) y UI de agents cuando el bridge y runtime estén estables. Trabajar en batches pequeños con CI verde entre cada uno.

### 3. Release readiness — pendiente

SSE streaming real, signing propio, minify.

## Hitos

| Version | Criterio |
|---|---|
| 0.4.0-alpha | Estado actual: bridge hardened, 65 tests, CI real |
| 0.5.0-beta | Bridge extraído, runtime convergido, upstream sync iniciado |
| 1.0.0 | Agent Chat adoptado, SSE real, release signing, validación E2E completa |

## Reglas de convergencia

- Batches pequeños, CI verde entre cada uno.
- No merge masivo de upstream ni rebase completo.
- No romper bridge HTTP, no reintroducir chat history.
