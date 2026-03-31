# Informe de Consultoría — gallery fork

> Fecha: 2026-03-30
> Auditoría multi-rol: Software Architect, Product Manager, QA & Security, DevOps

---

## Resumen ejecutivo

El fork de Google AI Edge Gallery tiene una base Android sólida (Compose/Hilt/MVVM) heredada de upstream, con adiciones propias: puente HTTP loopback, thinking mode, benchmark integrado y documentación viva. Se identifican fortalezas en accesibilidad y UX, pero debilidades críticas en seguridad del servicio HTTP, ausencia total de tests y CI mínimo.

**Puntuación global: 38% de madurez productiva.**

---

## Fase 0 — Comparativa con el estado del arte

| Proyecto | Fortalezas relativas |
|---|---|
| google-ai-edge/gallery (upstream) | UX pulida, a11y excelente, pipeline de modelo robusto |
| mlc-llm (MLCChat Android) | Multi-backend, alertas de error consistentes |
| llama.cpp (llama.android) | Sin red, máxima privacidad por diseño, CI sofisticado |

### Nivel SOTA definido

1. Servicio HTTP loopback seguro (auth + bind explícito + stateless)
2. Tests mínimos viables (>20 tests JVM + smoke HTTP)
3. CI que compile release, ejecute tests y lint
4. Thinking mode funcional E2E verificado
5. Benchmark accesible y usable
6. Documentación concisa y sin redundancias

---

## Fase 1 — Diagnóstico de madurez

| Eje | Score | Hallazgos clave |
|---|---|---|
| Calidad del código | 55% | Base Compose/Hilt/MVVM sólida. LlmHttpService (850 LOC) es god class. Mezcla Gson + kotlinx.serialization. Model.kt mezcla datos y estado runtime. |
| Errores y resiliencia | 30% | LlmSingleTurnViewModel descarta errores silenciosamente. resetSession() tiene retry infinito. HTTP expone mensajes internos. |
| Modularidad | 40% | Paquetes bien separados. Runtime helper útil. Pero servicio HTTP monolítico y allowlist duplicada entre app/servicio. |
| Testing | 3% | Zero tests. Dependencias declaradas sin usar. |
| Seguridad | 18% | NanoHTTPD NO bindea a 127.0.0.1. Sin auth HTTP. Debug signing para release. Payload logging auto en debug. |
| CI/CD | 25% | Build debug funcional. Sin tests, lint ni release en CI. |
| Documentación | 70% | Trio STATE/ROADMAP/backlog bien estructurado. Redundancias entre documentos. |
| UX / Accesibilidad | 80% | Best-in-class: LiveRegionMode.Polite, contentDescription extensivo, RTL. |

---

## Fase 2 — Roadmap redefinido

### Críticas — Bloqueadores de estabilidad

| ID | Tarea | Esfuerzo |
|---|---|---|
| SEC-01 | Bind NanoHTTPD a `127.0.0.1` explícitamente | S |
| SEC-02 | Auth Bearer token en HTTP API | S-M |
| SEC-03 | Inferencia HTTP stateless (reset por request) | S |
| SEC-04 | Errores JSON estructurados en HTTP | S |
| SEC-05 | Payload logging solo opt-in | S |
| ERR-01 | Fix error silencioso en LlmSingleTurnViewModel | S |
| ERR-02 | Cap retry loop en resetSession() | S |
| BUILD-02 | Release con keystore propio + minify | S |

### Evolutivas — Rendimiento/UX

| ID | Tarea | Esfuerzo |
|---|---|---|
| ARCH-01 | Extraer LlmHttpService en componentes | M |
| ARCH-02 | Unificar allowlist app/servicio | M |
| ARCH-03 | Estandarizar kotlinx.serialization | S |
| TEST-02 | 15-20 tests JVM | M |
| CI-01 | CI: tests + lint + release | S |
| UX-02 | Strings a11y hardcoded a resources | S |
| HTTP-02 | Rechazar model IDs desconocidos con 404 | S |

### Futuras — Nuevas capacidades

| ID | Tarea | Esfuerzo |
|---|---|---|
| STREAM-01 | SSE streaming real token-by-token | L |
| PERF-01 | Eliminar runBlocking en DataStoreRepository | M |
| ARCH-04 | Separar estado runtime de Model.kt | M-L |

---

## Fase 3 — Estimación de versión

| Criterio | Estado | % |
|---|---|---|
| Core features | ✅ Funcional | 90% |
| Thinking mode E2E | ✅ Implementado, pendiente validación | 75% |
| HTTP bridge | ⚠️ Funcional pero inseguro | 40% |
| Benchmark UX | ⚠️ Presente, pendiente validación | 60% |
| Testing | ❌ Inexistente | 3% |
| CI/CD | ⚠️ Build only | 25% |
| Seguridad | ❌ Crítica | 18% |

**Versión estimada: 0.3.0-alpha** — pre-release, no apta para distribución.

- **0.5.0-beta**: cerrar todas las Críticas
- **1.0.0**: Críticas + Evolutivas + validación E2E

---

## Siguiente iteración recomendada

**Iteración 4 — Hardening HTTP + Safety net**

1. SEC-01 a SEC-05: Seguridad del servicio HTTP
2. ERR-01 + ERR-02: Correcciones de error handling
3. CI-01: Upgrade CI pipeline
4. Primeros tests JVM
