# Fork Divergences

> Diferencias deliberadas respecto a [upstream](https://github.com/google-ai-edge/gallery).

## Fork añade (upstream no tiene)

| Capacidad | Archivos clave |
|---|---|
| Bridge HTTP local en `127.0.0.1` — auth Bearer, SSE, tool calls | `service/LlmHttpService.kt` + 8 helpers |
| 65 tests JVM | `src/test/` (14 archivos) |
| CI con tests, lint, artifacts | `.github/workflows/build_android.yaml` |
| `DataStoreRepository` async (sin `runBlocking`) | `data/DataStoreRepository.kt` |
| Error visible en single-turn | `ui/llmsingleturn/LlmSingleTurnViewModel.kt` |
| Reset session con max retries | `ui/llmchat/LlmChatViewModel.kt` |
| Thinking via `<think>` tag parsing | `ui/llmchat/LlmChatModelHelper.kt`, `ThinkingTagAccumulator.kt` |

## No adoptado de upstream

| Capacidad | Notas |
|---|---|
| Agent Chat + Skills | `customtasks/agentchat/`, `SkillAllowlist`, `SkillsSerializer` — adopción futura |
| Thinking via SDK nativo | `message.channels["thought"]` — más robusto que tag parsing; migración futura |

## Decisiones de diseño

- **Bridge HTTP** es la razón principal del fork. Siempre se preserva.
- **Chat history persistente** no se reintroduce (upstream tampoco lo tiene).
- **Tiny Garden** se preserva.
