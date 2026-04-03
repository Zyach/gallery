# Fork Divergences

> Diferencias deliberadas respecto a [upstream](https://github.com/google-ai-edge/gallery).

## Fork añade (upstream no tiene)

| Capacidad | Archivos clave |
|---|---|
| HTTP bridge en `127.0.0.1:9006` — API compatible OpenAI, auth Bearer, SSE real token-by-token, tool calls | `service/` (12 archivos) |
| 89 tests JVM | `src/test/` (16 archivos) |
| CI: tests + lint + APK artifacts | `.github/workflows/` |
| `DataStoreRepository` sin `runBlocking` | `data/DataStoreRepository.kt` |
| Error visible en single-turn | `ui/llmsingleturn/LlmSingleTurnViewModel.kt` |
| Reset de sesión con max retries | `ui/llmchat/LlmChatViewModel.kt` |
| Thinking via `<think>` tag parsing | `ThinkingTagAccumulator.kt`, `ui/llmchat/LlmChatModelHelper.kt` |

## No adoptado de upstream

| Capacidad | Notas |
|---|---|
| Agent Chat + Skills | `customtasks/agentchat/` (~30 archivos) — adopción futura |
| Sistema de promo banners | `PromoBannerGm4` en `GlobalModelManager` — no evaluado |
| Actualización runtime litertlm | Versión más reciente pendiente de adoptar |
| Thinking via SDK nativo | `message.channels["thought"]` — más robusto que tag parsing; migración futura |

## Decisiones de diseño

- El **bridge HTTP** es la razón principal del fork. Siempre se preserva.
- El **chat history persistente** no se reintroduce.
- La tarea **Tiny Garden** se preserva.
