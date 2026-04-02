# backlog — gallery fork

> Ultima actualizacion: 2026-04-02.

## Prioridad actual

### P1

- `ARCH-01` Seguir extrayendo `LlmHttpService`
  - Estado: SSE rendering movido a `LlmHttpResponseRenderer` (3 metodos duplicados eliminados, ~120 LOC menos en el servicio). Queda orquestacion de inference.
- `HTTP-01` Reforzar smoke tests del bridge local
  - Estado: cobertura JVM amplia y smoke E2E on-device basica completada; falta una capa mas sistematica del servicio vivo.
- `RUNTIME-01` Completar migracion runtime helper
  - Estado: iniciado; benchmark y cancelacion ya avanzaron.

### P2

- `ARCH-02` Cerrar unificacion de allowlist app/servicio
- `ARCH-03` Cerrar estandarizacion de `kotlinx.serialization`
- `STREAM-01` Implementar SSE real token-by-token
- `BUILD-02` Signing propio + minify

## Completado recientemente

- SSE rendering extraido a `LlmHttpResponseRenderer` (~120 LOC menos en servicio)
- tests JVM para SSE text/toolcall/empty payloads
- `PERF-01` Eliminado `runBlocking` de `DataStoreRepository`
- mitigacion del cutoff de Node.js 20 en GitHub Actions
- extraccion incremental del bridge HTTP en helpers puros
- validacion E2E on-device del bridge HTTP con auth y `generate`

## No reabrir sin motivo

- bridge HTTP loopback-only
- auth bearer del bridge
- reset stateless por request
- rechazo de modelos desconocidos con `404`
- Tiny Garden preservado
