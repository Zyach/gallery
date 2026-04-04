# STATE — gallery fork

> Última actualización: 2026-04-04

**Base:** upstream v1.0.11 · **Tests:** 89 JVM · **CI:** verde

## Capacidades (fork añade)

| Qué | Dónde |
|---|---|
| HTTP bridge — API compatible OpenAI, auth Bearer, SSE real, tool calls | `service/` (12 archivos) |
| 89 tests JVM unitarios | `src/test/` (16 archivos) |
| CI: tests + lint + APK artifacts | `.github/workflows/` |
| `DataStoreRepository` sin `runBlocking` | `data/DataStoreRepository.kt` |
| Errores visibles en single-turn; reset de sesión con max retries | `ui/llmchat/`, `ui/llmsingleturn/` |
| Thinking via SDK nativo (`message.channels["thought"]`) | `LlmChatModelHelper.kt` |
| Agent Chat + Skills importables (WebView, built-in skills) | `customtasks/agentchat/` |
| Promo banner Gemma 4 (PromoScreenGm4 → HomeScreen con gm4=true) | `ui/home/`, `ui/modelmanager/` |

## Gaps activos

| ID | Descripción |
|---|---|
| BUILD-02 | Release signing propio + minify (requiere keystore manual) |
