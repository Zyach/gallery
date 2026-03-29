# ROADMAP — gallery

> Roadmap vivo del fork.
> Alineado con `docs/STATE.md` y `docs/backlog.md`.
> Ultima actualizacion: 2026-03-30.

Complementos existentes en `docs/`:
- `docs/UPSTREAM_CATCHUP_PLAN.md` como playbook detallado de convergencia tecnica.
- `docs/IMPLEMENTATION_PLAN.md` como entrada de compatibilidad para referencias antiguas.

---

## Vision

Llevar el fork a una convergencia sostenible con `upstream/main` sin perder las capacidades propias que justifican su existencia:

- puente HTTP local para Every Code sobre loopback;
- Tiny Garden operativo;
- superficie de mantenimiento mas pequena mediante retirada del chat history persistente;
- integracion incremental, validable y reversible.

---

## Principios de ejecucion

1. Integrar en lotes pequenos y tematicos, no en una fusion masiva.
2. Mantener checkpoints claros entre lotes.
3. Tratar CI y smoke tests como criterio de salida, no como postdata.
4. Reducir superficie fork-specific donde no aporte valor real.
5. Mantener la documentacion viva sincronizada con el codigo real.

---

## Arquitectura objetivo del fork

```text
gallery
  ├─ README.md                 -> entrada corta del proyecto
  ├─ docs/
  │   ├─ STATE.md              -> que hay hoy y que riesgos existen
  │   ├─ ROADMAP.md            -> hacia donde va el fork
  │   └─ backlog.md            -> cola priorizada y viva
  └─ Android/src/
      └─ app/
          ├─ service/          -> puente HTTP local
          ├─ runtime/          -> helper abstraction para modelos
          ├─ customtasks/      -> Tiny Garden, Mobile Actions, etc.
          └─ ui/               -> surfaces Compose
```

---

## Estado por frentes

| Frente | Estado actual | Meta |
|---|---|---|
| Documentacion viva | 🟡 reubicada, aun por consolidar | `docs/` como unica referencia operativa |
| Puente HTTP local | ✅ presente | preservar contrato loopback y pruebas |
| Chat history persistente | 🟡 retirada en curso | eliminar restos de wiring y deuda |
| Tiny Garden | ✅ restaurado | mantener estabilidad durante sync |
| Runtime helper | 🟡 adopcion parcial | cerrar migracion de llamadas y contratos |
| Benchmark | 🟡 base presente, UX incompleta | exponer experiencia usable y estable |
| Convergencia upstream | 🟡 activa | avanzar por lotes pequenos con gates |

---

## Fases activas

### Fase A — Higiene documental y referencias

Objetivo: que la documentacion deje de mentir sobre la ubicacion y el estado del proyecto.

- Centralizar la documentacion operativa en `docs/`.
- Corregir referencias rotas desde `README.md`.
- Dejar `STATE`, `ROADMAP` y `backlog` como trio sincronizado.
- Reubicar semanticamente los planes heredados dentro de ese sistema documental, sin devolverlos a la raiz.

### Fase B — Estabilizacion del lote actual

Objetivo: cerrar el lote en curso sin arrastrar roturas obvias.

- Resolver conflictos de recursos y wiring visibles.
- Verificar que la retirada de chat history no deja imports, rutas ni pantallas colgantes.
- Verificar que thinking mode sigue vivo extremo a extremo.

### Fase C — Runtime y custom tasks

Objetivo: consolidar el salto al helper abstracto nuevo.

- Alinear `runtimeHelper` en chat, single-turn, Mobile Actions y Tiny Garden.
- Reducir llamadas legacy y divergencias de firma.
- Dejar una base mas limpia para seguir absorbiendo cambios upstream.

### Fase D — Benchmark usable

Objetivo: pasar de groundwork tecnico a funcionalidad visible y segura.

- Resolver duplicados de recursos.
- Evitar parseos sensibles a locale.
- Conectar benchmark con navegacion/home o documentar explicitamente que queda oculto.

### Fase E — Convergencia upstream por batches

Objetivo: continuar la sincronizacion sin reabrir invariantes cerrados.

- Agrupar cambios por tema: runtime, UI/nav, modelos/config, recursos.
- Validar cada lote con build y smoke tests.
- Documentar divergencias deliberadas del fork.

---

## Siguiente iteracion recomendada

### Iteracion 1 — Cerrar el lote actual y dejarlo verificable

1. Resolver las roturas de build mas probables del lote actual.
2. Limpiar contradicciones entre codigo, recursos y navegacion.
3. Ejecutar `:app:assembleDebug` si el entorno Android local lo permite.
4. Dejar documentados los resultados en `STATE` y `backlog`.

### Entregables de salida

- Build local o, si falla, listado exacto de bloqueos del entorno.
- `README.md` y `docs/` sin referencias rotas.
- Lista corta de deuda tecnica inmediata priorizada.

---

## Criterio de exito

La siguiente iteracion se considera cerrada cuando se cumplan a la vez:

1. El repo tiene documentacion viva coherente en `docs/`.
2. No quedan referencias principales a planes/documentos ausentes.
3. El lote actual compila o deja identificado un conjunto concreto de bloqueos verificables.
4. El fork mantiene intactos HTTP loopback, Tiny Garden y la retirada de chat history.

---

## No objetivos por ahora

- Reintroducir chat history persistente.
- Abrir refactors amplios ajenos al lote actual.
- Mover otra vez la estrategia documental fuera de `docs/`.
