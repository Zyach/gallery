# ROADMAP — gallery fork

> Ultima actualizacion: 2026-04-02.

## Objetivo

Converger con upstream de forma gradual, manteniendo estable el bridge HTTP local, Tiny Garden y la direccion sin chat history persistente.

## Tracks activos

### 1. Bridge HTTP

Objetivo: terminar de sacar logica de `LlmHttpService` a componentes puros y cerrar una validacion mas fuerte del servicio vivo.

Estado:

- Hardening basico completado.
- Refactor incremental en progreso.
- Smoke E2E on-device basica ya validada.

Siguiente paso:

- seguir reduciendo la orquestacion dentro de `LlmHttpService`
- ampliar smoke tests del bridge vivo

### 2. Runtime convergence

Objetivo: terminar la migracion a `LlmModelHelper` en flujos clave y reducir acoplamientos al helper concreto.

Estado:

- benchmark y cancelacion ya avanzaron
- faltan mas flujos y limpieza de contratos

### 3. Data and serialization

Objetivo: cerrar la unificacion de allowlist y JSON con `kotlinx.serialization`.

Estado:

- parser compartido ya activo
- quedan rutas residuales por consolidar

### 4. Release readiness

Objetivo: preparar el fork para una beta mas estable.

Pendiente:

- SSE token-by-token real
- signing propio
- minify y endurecimiento de release

## Hitos

| Version | Criterio |
|---|---|
| `0.3.0-alpha` | estado actual |
| `0.5.0-beta` | hardening + safety net consolidados |
| `1.0.0` | bridge, runtime y capas principales estabilizadas con validacion E2E |
