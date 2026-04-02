# backlog — gallery fork

> Ultima actualizacion: 2026-04-02.

---

## Prioridades

- `P0`: bloquea build, CI, invariantes o seguridad.
- `P1`: compromete la siguiente iteración si se pospone.
- `P2`: preparación de batches futuros.

---

## P0

- [x] `SEC-01` Bind NanoHTTPD a 127.0.0.1
- [x] `SEC-02` Auth Bearer token en HTTP API
- [x] `SEC-03` Inferencia HTTP stateless (reset por request)
- [x] `SEC-04` Errores JSON estructurados en HTTP
- [x] `SEC-05` Payload logging solo opt-in
- [x] `ERR-01` Fix error silencioso en LlmSingleTurnViewModel
- [x] `ERR-02` Cap retry loop en resetSession (max 5)
- [x] `THINK-01` Parseo de `<think>` tags en LlmChatModelHelper

---

## P1

- [x] `TEST-02` 15-20 tests JVM
- [x] `CI-01` CI: build automático relevante tras push
- [x] `HTTP-02` Rechazar model IDs desconocidos con 404
- [ ] `RUNTIME-01` Completar migración runtimeHelper
  - Estado: iniciada con unificación de benchmark y cancelación sobre `LlmModelHelper`.
- [ ] `HTTP-01` Smoke tests del puente local
  - Estado: la cobertura JVM del contrato HTTP sigue creciendo, incluyendo adaptación de requests y helpers puros, pero faltan pruebas end-to-end del servicio vivo.
- [x] `DOC-02` Divergencias deliberadas documentadas

---

## P2

- [ ] `ARCH-01` Extraer LlmHttpService en componentes
  - Estado: `ModelResolver`, `ResponseRenderer` y DTO/helpers de request ya extraídos; falta mover la orquestación principal del bridge.
- [ ] `ARCH-02` Unificar allowlist app/servicio
  - Estado: parser compartido del allowlist ya reutilizado por servicio y model manager.
- [ ] `ARCH-03` Estandarizar kotlinx.serialization
  - Estado: Gson ya no se usa en las rutas del allowlist; quedan otras rutas JSON genéricas fuera de este lote.
- [ ] `STREAM-01` SSE streaming real
- [x] `PERF-01` Eliminar runBlocking en DataStoreRepository
- [ ] `BUILD-02` Release signing + minify

---

## Hecho en este ciclo

- [x] Base inicial de tests JVM para parseo de thinking y helpers puros del bridge HTTP.
- [x] Ampliacion de tests JVM para serializers proto y helpers puros de tareas.
- [x] Workflow dedicado de tests JVM separado del workflow de build Android.
- [x] Inicio de `ARCH-01` con extracción de resolver y renderer del bridge HTTP.
- [x] Inicio de `RUNTIME-01` con benchmark/cancelación encaminados a `LlmModelHelper`.
- [x] Inicio de `ARCH-02/03` con parser compartido del allowlist basado en `kotlinx.serialization`.
- [x] Ampliacion de helpers y tests JVM del contrato del bridge HTTP.
- [x] Cierre de `PERF-01` eliminando `runBlocking` de `DataStoreRepository` y moviendo las escrituras a funciones suspendidas.
- [x] Mantenimiento de workflows para adelantarse al cutoff de Node.js 20 en GitHub Actions.
- [x] Nuevo avance de `ARCH-01`/`HTTP-01` con extracción de DTOs y helpers de adaptación de requests del bridge HTTP.
- [x] Bind loopback explícito (SEC-01)
- [x] Auth Bearer opcional-configurable en bridge HTTP (SEC-02)
- [x] Reset conversacional por request para mantener la API HTTP stateless (SEC-03)
- [x] 404 para model IDs desconocidos (HTTP-02)
- [x] Errores JSON (SEC-04)
- [x] Payload logging opt-in (SEC-05)
- [x] Fix error single-turn (ERR-01)
- [x] Cap retry (ERR-02)
- [x] CI build automático relevante tras push (CI-01)
- [x] Thinking mode E2E (THINK-01)
- [x] Doc divergencias (DOC-02)
- [x] Informe de consultoría multi-rol
- [x] Documentación simplificada y consolidada
