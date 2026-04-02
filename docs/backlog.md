# backlog — gallery fork

> Ultima actualizacion: 2026-04-03.

## P1 — Próxima iteración

- `ARCH-01` Extraer handlers HTTP y logging de `LlmHttpService`
- `RUNTIME-01` Completar migración runtime helper en flujos pendientes
- `SYNC-01` Evaluar adopción de Agent Chat / Skills de upstream

## P2 — Después

- `STREAM-01` SSE streaming real token-by-token
- `BUILD-02` Release signing propio + minify
- `THINK-02` Migrar de `<think>` tag parsing a `message.channels["thought"]` nativo

## Completado

- Bridge HTTP: loopback, auth, stateless, JSON errors, payload logging opt-in
- Extracción: InferenceGateway, ResponseRenderer, BodyParser, RouteResolver, ModelResolver, ModelFactory, RequestAdapter, BridgeUtils, ApiModels
- DataStoreRepository sin `runBlocking`
- Error handling: single-turn visible, resetSession con max retries
- 65 tests JVM (14 archivos)
- CI: tests + lint + debug/release + artifacts + Node.js 24
- Thinking mode E2E con `<think>` tag parsing
- Documentación consolidada

## No reabrir

- Bridge HTTP loopback-only, auth bearer, stateless, 404 para modelos desconocidos
- Tiny Garden preservado
- Chat history persistente fuera de alcance
