# ROADMAP — gallery fork

> Última actualización: 2026-04-03

## Objetivo

Mantener el bridge HTTP operativo mientras se converge con upstream. Adoptar Agent Chat/Skills cuando el bridge y runtime estén estables.

## Pendiente

- `BUILD-02` Release signing propio + minify
- `THINK-02` Migrar `<think>` tag parsing a `message.channels["thought"]` (bloqueado — SDK no lo expone)

## Completado (resumen)

- Promo banner Gemma 4, AppTitleGm4, modelo de navegación PromoScreen→Home (`SYNC-02c`)
- AgentChat + Skills importables (WebView, 8 built-in skills, SkillManager) (`SYNC-02b`)
- Litertlm 0.9.0 → 0.10.0 + versionCode 23 (`SYNC-02a`)
- SSE streaming real token-by-token en `/v1/responses` (`STREAM-01`)
- Extracción de `LlmHttpLogger` y `LlmHttpAllowlistLoader` + tests (`ARCH-01`)
- Upstream sync a v1.0.11 (GlobalModelManager, BenchmarkScreen, filtrado SOC/NPU, FCM)
- HTTP bridge: loopback, auth Bearer, SSE, tool calls, stateless
- 89 tests JVM (16 archivos), CI verde
- `DataStoreRepository` async, thinking mode, error handling

## Restricciones

- Batches pequeños, CI verde entre cada uno. Sin merge masivo ni rebase completo.
- No romper el bridge HTTP. No reintroducir chat history persistente.
