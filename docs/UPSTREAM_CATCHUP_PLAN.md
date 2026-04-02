# Upstream Catch-Up Plan

> Ultima actualizacion: 2026-04-02.

## Objetivo

Reducir la distancia con `upstream/main` sin romper el bridge HTTP local ni las invariantes del fork.

## Reglas de convergencia

- no hacer un merge masivo de upstream
- no hacer rebase completo del fork sobre upstream
- trabajar en batches pequenos y tematicos
- empujar cada batch y validar en GitHub Actions antes del siguiente

## Orden recomendado

### 1. Bridge HTTP y arquitectura local

Seguir extrayendo logica pura de `LlmHttpService` hasta dejar el servicio como orquestador fino.

### 2. Runtime helper

Terminar la convergencia de `LlmModelHelper` en los flujos pendientes antes de tocar capas mas inestables.

### 3. Data layer compartida

Cerrar allowlist y serializacion para reducir diferencias innecesarias con upstream.

### 4. UI y shell

Adoptar piezas de upstream cuando el runtime y la data layer ya esten estabilizados.

### 5. Cleanup final

Resolver diferencias menores, release readiness y deuda residual.

## Stop rules

- no romper el contrato del bridge HTTP
- no reintroducir chat history persistente
- no tratar una fase como cerrada sin CI verde
