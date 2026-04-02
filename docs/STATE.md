# STATE — gallery fork

> Ultima actualizacion: 2026-04-03.

## Resumen

Fork de [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) para Android on-device AI. Añade un bridge HTTP local para Every Code y mejoras de calidad que upstream no tiene.

**Version: 0.4.0-alpha** · **65 tests JVM** · **CI verde**

## Qué tiene el fork que upstream no tiene

- **Bridge HTTP local** — servicio completo en `127.0.0.1` con auth, SSE, tool calls. 9 componentes extraídos, testeable.
- **65 tests JVM** — upstream tiene cero tests.
- **CI real** — tests + lint + debug + release + artifacts. Upstream solo hace `assembleRelease`.
- **DataStoreRepository async** — eliminado `runBlocking` (upstream tiene TODO abierto `b/423700720`).
- **Error handling corregido** — single-turn muestra errores (upstream los descarta), resetSession con max retries (upstream es infinito).

## Qué tiene upstream que el fork no tiene

- **Agent Chat + Skills** — sistema completo de skills importables, chat con herramientas, SkillManager.
- **Thinking via SDK nativo** — `message.channels["thought"]` vs nuestro parsing de `<think>` tags.
- **FCM push notifications** — `FcmMessagingService`.
- **Promo/onboarding screens** — `PromoScreenGm4`, `PromoBannerGm4`.
- **UI avanzada para agents** — `CollapsableProgressPanel`, `LogsViewer`, `MessageBodyWebview`.

## Estado validado

- CI verde: `Android APK` + `Android JVM Tests`.
- E2E on-device: `/ping`, `/v1/models`, `/generate` con bearer token.
- Node.js 24 adelantado en workflows.

## Gaps activos

| ID | Descripción |
|---|---|
| ARCH-01 | Quedan handlers HTTP individuales y logging en `LlmHttpService` |
| RUNTIME-01 | Migración runtime helper incompleta |
| STREAM-01 | SSE streaming real token-by-token pendiente |
| BUILD-02 | Release signing propio + minify pendientes |
| SYNC-01 | Agent Chat, Skills y thinking nativo de upstream no adoptados |

## Validación

GitHub Actions es el gate primario. Ciclo E2E on-device cuando el cambio lo requiera.
