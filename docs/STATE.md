# STATE — gallery fork

> Ultima actualizacion: 2026-04-01.

---

## Resumen

Fork activo de Google AI Edge Gallery para Android on-device AI. Tres invariantes: puente HTTP loopback (Every Code), Tiny Garden preservado, chat history no reintroducido.

**Version estimada: 0.3.0-alpha** | **Madurez: 38%**

---

## Invariantes del fork

1. **Puente HTTP local** — `LlmHttpService` sobre `127.0.0.1` (NanoHTTPD). Endpoints: `/ping`, `/v1/models`, `/generate`, `/v1/chat/completions`.
2. **Chat history fuera de alcance** — Alineado con upstream (tampoco lo tiene).
3. **Tiny Garden preservado** — Assets, task module y pantalla funcionales.

---

## Estado técnico

- App Compose/Hilt con runtime helper abstracto (`LlmModelHelper`).
- Thinking mode: parseo de `<think>` extraído a acumulador dedicado para streaming robusto, incluyendo tags partidos entre chunks.
- Benchmark: ruta dedicada y botón en app bar.
- Runtime helper: benchmark y cancelación empiezan a unificarse bajo `LlmModelHelper`.
- HTTP service: bindeado a `127.0.0.1`, errores JSON, payload logging opt-in, Bearer token configurable y reset conversacional por request.
- Allowlist: el parser compartido empieza a unificarse entre app y servicio con `kotlinx.serialization`.
- HTTP bridge: `ModelResolver` y `ResponseRenderer` empiezan a salir de `LlmHttpService` como componentes reutilizables.
- CI: build debug + release automático en GitHub Actions sobre cambios relevantes del código.
- GitHub hygiene del fork: issue forms, Dependabot, release workflow por tag, release notes config, CODEOWNERS, SECURITY/SUPPORT y docs index.

### Gaps activos

- Tests: `TEST-02` queda cubierto con una base JVM de 20+ casos sobre thinking parser, helpers HTTP, serializers proto y helpers de tasks.
- GitHub Actions separa ahora build Android y tests JVM en workflows distintos.
- `RUNTIME-01` ya está iniciado, pero no completado: aún faltan más flujos y limpieza de contratos alrededor del helper.
- `ARCH-02` y `ARCH-03` ya están iniciadas, pero no cerradas: el allowlist aún tiene deuda residual fuera del parser compartido.
- `ARCH-01` ya está iniciada, pero no cerrada: la orquestación principal del bridge sigue en el servicio.
- `HTTP-01` avanza por la vía JVM/pura del contrato del bridge, pero faltan smoke tests end-to-end contra el servicio vivo.
- `PERF-01` cerrada: `DataStoreRepository` ya no usa `runBlocking`; lecturas síncronas van por snapshots en memoria y las escrituras pasan por funciones suspendidas desde coroutines de UI/viewmodel.
- Validación funcional de thinking/benchmark en dispositivo.

---

## Riesgos

| Riesgo | Severidad | Estado |
|---|---|---|
| HTTP bind a 0.0.0.0 | Crítico | ✅ Corregido (127.0.0.1) |
| Sin auth HTTP | Alto | ✅ Bearer token configurable |
| Payload logging auto | Alto | ✅ Corregido (opt-in) |
| Error silencioso single-turn | Medio | ✅ Corregido |
| Retry infinito resetSession | Medio | ✅ Corregido (max 5) |
| Errores plaintext HTTP | Medio | ✅ Corregido (JSON) |
| Model IDs desconocidos aceptados | Medio | ✅ Corregido (404) |

---

## Decisión documental

| Documento | Propósito |
|---|---|
| `docs/STATE.md` | Estado actual observable |
| `docs/ROADMAP.md` | Dirección y fases |
| `docs/backlog.md` | Cola priorizada de trabajo |
| `docs/FORK_DIVERGENCES.md` | Diferencias intencionales vs upstream |
| `docs/INFORME_CONSULTORIA_2026-03-30.md` | Auditoría multi-rol completa |
