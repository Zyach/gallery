# ROADMAP — gallery fork

> Ultima actualizacion: 2026-03-30.

---

## Visión

Convergencia sostenible con upstream manteniendo: puente HTTP seguro, Tiny Garden, sin chat history persistente.

---

## Fases

### Fase A — Hardening HTTP ← **activa**
- [x] Bind explícito a 127.0.0.1
- [x] Errores JSON estructurados
- [x] Payload logging opt-in only
- [x] Auth Bearer token
- [ ] Inferencia stateless por request
- [x] Rechazar model IDs desconocidos con 404

### Fase B — Safety net
- [x] Fix error silencioso single-turn
- [x] Cap retry loop resetSession
- [ ] 15-20 tests JVM (parsing, SSE, model resolution, thinking tags)
- [x] CI: build automático relevante tras push

### Fase C — Arquitectura
- [ ] Extraer LlmHttpService en componentes (ModelResolver, InferenceGateway, ApiRenderer)
- [ ] Unificar allowlist app/servicio
- [ ] Estandarizar kotlinx.serialization (eliminar Gson del servicio)

### Fase D — Runtime y convergencia upstream
- [ ] Completar migración runtimeHelper (cancelación/benchmark via helper)
- [ ] Convergencia por batches temáticos con upstream
- [ ] Validación funcional thinking + benchmark en dispositivo

### Fase E — Producción
- [ ] SSE streaming real token-by-token
- [ ] Eliminar runBlocking en DataStoreRepository
- [ ] Release signing con keystore propio + minify

---

## Hitos de versión

| Versión | Requisitos |
|---|---|
| 0.3.0-alpha | Estado actual |
| 0.5.0-beta | Fases A + B completas |
| 1.0.0 | Fases A-D completas + validación E2E |
