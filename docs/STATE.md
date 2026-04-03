# STATE — gallery fork

> Última actualización: 2026-04-03

**Base:** upstream v1.0.11 · **Tests:** 89 JVM · **CI:** verde

## Capacidades (fork añade)

| Qué | Dónde |
|---|---|
| HTTP bridge — API compatible OpenAI, auth Bearer, SSE real, tool calls | `service/` (12 archivos) |
| 89 tests JVM unitarios | `src/test/` (16 archivos) |
| CI: tests + lint + APK artifacts | `.github/workflows/` |
| `DataStoreRepository` sin `runBlocking` | `data/DataStoreRepository.kt` |
| Errores visibles en single-turn; reset de sesión con max retries | `ui/llmchat/`, `ui/llmsingleturn/` |
| Thinking via `<think>` tag parsing | `ThinkingTagAccumulator.kt` |

## No adoptado de upstream

| Qué | Estado |
|---|---|
| Agent Chat + Skills importables | Planificado — requiere bridge estable |
| Sistema de promo banners (`PromoBannerGm4`) | No evaluado |
| Actualización de runtime litertlm | Pendiente |
| Thinking via SDK nativo (`message.channels["thought"]`) | Bloqueado — SDK no lo expone aún |

## Gaps activos

| ID | Descripción |
|---|---|
| SYNC-02 | Upstream sync: litertlm + AgentChat + Skills + promo banner |
| BUILD-02 | Release signing propio + minify |
| THINK-02 | Migrar thinking de tag parsing a SDK nativo |
