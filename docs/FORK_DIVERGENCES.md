# Fork Divergences

> Diferencias deliberadas respecto a [upstream](https://github.com/google-ai-edge/gallery).
> Última actualización: 2026-04-04

El fork está a **paridad completa** con upstream v1.0.11 en código fuente y assets. Las divergencias son exclusivamente adiciones.

## Fork añade (upstream no tiene)

| Capacidad | Archivos clave |
|---|---|
| HTTP bridge en `127.0.0.1:9006` — API compatible OpenAI, auth Bearer, SSE real token-by-token, tool calls | `service/` (12 archivos) |
| 88 tests JVM | `src/test/` (15 archivos) |
| CI: tests + lint + APK artifacts | `.github/workflows/jvm-tests.yml` |
| `DataStoreRepository` sin `runBlocking` | `data/DataStoreRepository.kt` |
| Error visible en single-turn | `ui/llmsingleturn/LlmSingleTurnViewModel.kt` |
| Reset de sesión con max retries | `ui/llmchat/LlmChatViewModel.kt` |
| Thinking via SDK nativo `message.channels["thought"]` | `ui/llmchat/LlmChatModelHelper.kt` |
| Documentación del fork | `docs/` |
| Herramientas de diagnóstico | `tools/` |
| Datasets mobile-actions | `mobile-actions/` |

## Decisiones de diseño

- El **bridge HTTP** es la razón principal del fork. Siempre se preserva.
- El **chat history persistente** no se reintroduce.
- **Thinking** usa `message.channels["thought"]` del SDK nativo desde litertlm 0.10.0. `ThinkingTagAccumulator.kt` eliminado (no tenía callers en producción).
