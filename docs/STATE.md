# STATE — gallery fork

> Última actualización: 2026-04-04

**Base:** upstream v1.0.11 · **Paridad con upstream:** completa · **Tests:** 88 JVM · **CI:** verde

## Capacidades exclusivas del fork

| Qué | Dónde |
|---|---|
| HTTP bridge — API compatible OpenAI, auth Bearer, SSE real, tool calls | `service/` (12 archivos) |
| 88 tests JVM unitarios | `src/test/` (15 archivos) |
| CI: tests + lint + APK artifacts | `.github/workflows/` |
| `DataStoreRepository` sin `runBlocking` | `data/DataStoreRepository.kt` |
| Errores visibles en single-turn; reset de sesión con max retries | `ui/llmchat/`, `ui/llmsingleturn/` |
| Thinking via SDK nativo (`message.channels["thought"]`) | `LlmChatModelHelper.kt` |

## Adoptado de upstream (completo)

El fork incluye todo el código fuente y assets de upstream v1.0.11:

- Agent Chat + Skills importables (WebView, 8 built-in skills, SkillManager) — `customtasks/agentchat/`
- Promo banner Gemma 4, HomeScreen `gm4=true`, AppTitleGm4, RevealingText — `ui/home/`, `ui/modelmanager/`
- Skills button wired ChatPanel→MessageInputText, loading screen en primer init — SYNC-03
- litertlm 0.10.0, versionCode 23
- Assets multimedia skills: 88 MP3 (virtual-piano), HTML/JS (built-in + featured)
- `model_allowlists/1_0_11.json`, `.github/DISCUSSION_TEMPLATE/skills.yml`, `static.yml` CI

## Tracking automático de upstream

Workflow diario (09:00 UTC) en `.github/workflows/upstream-watch.yml`:
- Compara `upstream/main` contra `.upstream-sha`
- Si hay cambios: abre issue con commits, stat y análisis AUTO/MANUAL/FORK vía Claude Haiku
- Actualiza `.upstream-sha` en main automáticamente

Para activar el análisis IA: añadir `ANTHROPIC_API_KEY` como repo secret.

## Gaps activos

| ID | Descripción |
|---|---|
| BUILD-02 | Release signing propio + minify (requiere keystore manual) |
