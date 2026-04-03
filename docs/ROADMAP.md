# ROADMAP — gallery fork

> Última actualización: 2026-04-03

## Objetivo

Mantener el bridge HTTP local operativo mientras se converge progresivamente con upstream. Adoptar Agent Chat/Skills cuando el bridge y runtime estén estables.

## Pendiente

### P2 — Próxima iteración

- `BUILD-02` Release signing propio + minify
- `THINK-02` Migrar de `<think>` tag parsing a `message.channels["thought"]` nativo

## Completado

- `STREAM-01` SSE streaming real token-by-token en `/v1/responses` vía `PipedOutputStream` + `executeStreaming()` en `LlmHttpInferenceGateway`
- `ARCH-01` Extraer `LlmHttpLogger` y `LlmHttpAllowlistLoader` de `LlmHttpService` + tests; mover `GenReq` a ApiModels
- Upstream sync a v1.0.11 (GlobalModelManager, BenchmarkScreen, SOC/NPU filtering, FCM, agent UI components)
- Bridge HTTP: loopback, auth Bearer, stateless, SSE, tool calls, JSON errors
- Extracción bridge: InferenceGateway, ResponseRenderer, BodyParser, RouteResolver, ModelResolver, ModelFactory, RequestAdapter, BridgeUtils, ApiModels
- DataStoreRepository sin `runBlocking`
- Error handling: single-turn visible, reset session con max retries
- 65 tests JVM (14 archivos)
- CI: tests + lint + debug/release + artifacts
- Thinking mode con `<think>` tag parsing

## Restricciones

- Batches pequeños, CI verde entre cada uno. No merge masivo ni rebase completo.
- No romper bridge HTTP. No reintroducir chat history persistente.
