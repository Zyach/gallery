# ROADMAP — gallery fork

> Última actualización: 2026-04-04

## Objetivo

Mantener el bridge HTTP operativo mientras se converge con upstream. Fork ahora a paridad completa con upstream v1.0.11.

## Pendiente

- `BUILD-02` Release signing propio + minify (requiere keystore manual)
- Añadir `ANTHROPIC_API_KEY` como repo secret para activar el análisis IA en el workflow de tracking

## Completado (resumen)

## Completado (resumen)

- Workflow de tracking upstream con análisis Claude Haiku (`upstream-watch.yml`, `.upstream-sha`, `.github/scripts/analyze_upstream.py`)
- Fix CI: `MAX_RECOMMENDED_SKILL_COUNT` en Consts.kt, AppModule.kt ordering, eliminar `ThinkingTagAccumulator` (dead code), `customTasks.toList()` en ModelManagerViewModel
- Paridad completa: assets multimedia y archivos secundarios de upstream (88 MP3, HTML/JS skills, model_allowlists)
- Skills button wired ChatPanel→MessageInputText, init loading screen, upstream flag refactors (`SYNC-03`)
- Thinking via SDK nativo `message.channels["thought"]` (`THINK-02`)
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
