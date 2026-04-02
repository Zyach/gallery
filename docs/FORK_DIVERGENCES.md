# Fork Divergences

> Diferencias deliberadas respecto a [upstream](https://github.com/google-ai-edge/gallery).

## Fork añade (upstream no tiene)

| Capacidad | Archivos clave |
|---|---|
| Bridge HTTP local en `127.0.0.1` con auth, SSE, tool calls | `service/LlmHttpService.kt` + 8 helpers |
| 65 tests JVM | `src/test/` (14 archivos) |
| CI con tests, lint, artifacts | `.github/workflows/` |
| `DataStoreRepository` async (sin `runBlocking`) | `data/DataStoreRepository.kt` |
| Error visible en single-turn | `LlmSingleTurnViewModel.kt` |
| Reset session con max retries | `LlmChatViewModel.kt` |
| Thinking via `<think>` tag parsing | `LlmChatModelHelper.kt` |

## Fork no tiene (upstream sí)

| Capacidad | Notas |
|---|---|
| Agent Chat + Skills | `customtasks/agentchat/`, `SkillAllowlist`, `SkillsSerializer` |
| Thinking via SDK nativo | `message.channels["thought"]` — más correcto que nuestro tag parsing |
| FCM push notifications | `FcmMessagingService.kt` |
| Promo/onboarding UI | `PromoScreenGm4`, `PromoBannerGm4` |
| Agent progress UI | `CollapsableProgressPanel`, `LogsViewer`, `MessageBodyWebview` |

## Decisiones de diseño

- **Chat history persistente** no se reintroduce (upstream tampoco lo tiene).
- **Tiny Garden** se preserva.
- **Bridge HTTP** es la razón principal del fork.
