# STATE — gallery fork

> Ultima actualizacion: 2026-03-30.

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
- HTTP service: bindeado a `127.0.0.1`, errores JSON, payload logging opt-in, Bearer token configurable y reset conversacional por request.
- CI: build debug + release automático en GitHub Actions sobre cambios relevantes del código.

### Gaps activos

- Tests: existe ya una base JVM pequeña para lógica pura, pero `TEST-02` sigue lejos del objetivo 15-20.
- El workflow `Android APK` actual verifica compilación, no batería de tests.
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
