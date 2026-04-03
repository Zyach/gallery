# STATE — gallery fork

> Última actualización: 2026-04-03

Fork de [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) para Android on-device AI. Añade un bridge HTTP local para Every Code y mejoras de calidad que upstream no tiene.

**App version: 1.0.11** (en paridad con upstream) · **65 tests JVM** · **CI verde**

## Qué añade este fork

| Capacidad | Detalle |
|---|---|
| **Bridge HTTP local** | Servicio en `127.0.0.1` — auth Bearer, SSE, tool calls. 9 componentes extraídos. |
| **65 tests JVM** | Upstream tiene cero. 14 archivos de test en `src/test/`. |
| **CI completo** | Tests + lint + debug/release + artifacts. Upstream solo hace `assembleRelease`. |
| **DataStoreRepository async** | Eliminado `runBlocking` (upstream tiene TODO abierto `b/423700720`). |
| **Error handling** | Errores visibles en single-turn; reset session con max retries. |

## No adoptado de upstream

| Capacidad | Motivo |
|---|---|
| Agent Chat + Skills | Pendiente — requiere estabilizar bridge y runtime primero. |
| Thinking via SDK nativo | Usando `ThinkingTagAccumulator` (`<think>` tags). Migración futura a `message.channels["thought"]`. |

## Gaps activos

| ID | Descripción |
|---|---|
| ARCH-01 | Handlers HTTP individuales y logging aún en `LlmHttpService` (pendiente extracción) |
| STREAM-01 | SSE streaming real token-by-token pendiente |
| BUILD-02 | Release signing propio + minify pendientes |

## Validación

GitHub Actions es el gate primario (`build_android.yaml`). Ciclo E2E on-device (logcat vía Shizuku) cuando el cambio lo requiera.
