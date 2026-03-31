# backlog — gallery fork

> Ultima actualizacion: 2026-03-30.

---

## Prioridades

- `P0`: bloquea build, CI, invariantes o seguridad.
- `P1`: compromete la siguiente iteración si se pospone.
- `P2`: preparación de batches futuros.

---

## P0

- [x] `SEC-01` Bind NanoHTTPD a 127.0.0.1
- [x] `SEC-02` Auth Bearer token en HTTP API
- [ ] `SEC-03` Inferencia HTTP stateless (reset por request)
- [x] `SEC-04` Errores JSON estructurados en HTTP
- [x] `SEC-05` Payload logging solo opt-in
- [x] `ERR-01` Fix error silencioso en LlmSingleTurnViewModel
- [x] `ERR-02` Cap retry loop en resetSession (max 5)
- [x] `THINK-01` Parseo de `<think>` tags en LlmChatModelHelper

---

## P1

- [ ] `TEST-02` 15-20 tests JVM
- [x] `CI-01` CI: tests + lint + release build
- [x] `HTTP-02` Rechazar model IDs desconocidos con 404
- [ ] `RUNTIME-01` Completar migración runtimeHelper
- [ ] `HTTP-01` Smoke tests del puente local
- [x] `DOC-02` Divergencias deliberadas documentadas

---

## P2

- [ ] `ARCH-01` Extraer LlmHttpService en componentes
- [ ] `ARCH-02` Unificar allowlist app/servicio
- [ ] `ARCH-03` Estandarizar kotlinx.serialization
- [ ] `STREAM-01` SSE streaming real
- [ ] `PERF-01` Eliminar runBlocking en DataStoreRepository
- [ ] `BUILD-02` Release signing + minify

---

## Hecho en este ciclo

- [x] Bind loopback explícito (SEC-01)
- [x] Auth Bearer opcional-configurable en bridge HTTP (SEC-02)
- [x] 404 para model IDs desconocidos (HTTP-02)
- [x] Errores JSON (SEC-04)
- [x] Payload logging opt-in (SEC-05)
- [x] Fix error single-turn (ERR-01)
- [x] Cap retry (ERR-02)
- [x] CI upgrade: tests + lint + release (CI-01)
- [x] Thinking mode E2E (THINK-01)
- [x] Doc divergencias (DOC-02)
- [x] Informe de consultoría multi-rol
- [x] Documentación simplificada y consolidada
