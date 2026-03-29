# backlog — gallery

> Cola viva y priorizada del fork.
> Alineada con `docs/STATE.md` y `docs/ROADMAP.md`.
> Ultima actualizacion: 2026-03-30.

---

## Criterio de priorizacion

- `P0`: bloquea build, CI, validacion o invariantes del fork.
- `P1`: no bloquea hoy, pero compromete la siguiente iteracion si se pospone.
- `P2`: mejora de robustez, limpieza o preparacion de batches futuros.

---

## P0 — Hacer ahora

- [x] `DOC-01` Corregir referencias documentales rotas en entrada principal.
  - DoD: `README.md` apunta solo a documentos existentes en `docs/`.

- [x] `BUILD-01` Resolver errores de build evidentes del lote actual.
  - DoD: `:app:assembleDebug` pasa o deja bloqueos exactos documentados.

- [x] `RES-01` Eliminar el conflicto de recursos por `benchmark` duplicado.
  - DoD: no hay claves duplicadas en `strings.xml` que rompan merge/aapt.

- [ ] `THINK-01` Verificar la cadena completa de thinking mode.
  - DoD: los partial thinking chunks llegan al estado/UI y el panel no se autoexpande de forma incorrecta.

- [x] `NAV-01` Asegurar que la retirada de chat history no deja navegacion muerta ni imports colgantes.
  - DoD: sin referencias activas a `ui/history` ni `data/chathistory`.

---

## P1 — Siguiente sprint

- [ ] `RUNTIME-01` Completar la migracion a `model.runtimeHelper` en los flujos que queden legacy.
  - DoD: firmas alineadas en chat, single-turn, Mobile Actions y Tiny Garden.

- [ ] `BENCH-01` Decidir la exposicion de benchmark al usuario.
  - DoD: benchmark queda cableado a navegacion/home o explicitamente desactivado/documentado.

- [ ] `BENCH-02` Corregir parseos sensibles a locale en benchmark.
  - DoD: no se parsean strings formateados mediante `toDouble()` en rutas de UI/resultados.

- [ ] `HTTP-01` Ejecutar smoke tests del puente local.
  - DoD: `/ping`, `/v1/models`, `/generate` y `/v1/chat/completions` responden en loopback.

- [ ] `DOC-02` Dejar una nota corta de divergencias deliberadas del fork respecto a upstream.
  - DoD: las diferencias intencionales quedan resumidas y enlazables.

---

## P2 — Preparacion de siguientes lotes

- [ ] `SYNC-01` Agrupar el catch-up restante por lotes pequenos tematicos.
  - DoD: cada lote tiene objetivo, riesgo y validacion de salida.

- [ ] `UX-01` Revisar superficies home/settings tras la retirada de chat history.
  - DoD: sin affordances confusas ni opciones huerfanas.

- [ ] `TEST-01` Definir una receta minima de validacion local del fork.
  - DoD: build, CI y smoke HTTP quedan descritos en orden de ejecucion.

- [ ] `DOC-03` Evaluar si hace falta un `docs/CHANGELOG.md` o un indice documental ligero.
  - DoD: decision tomada y consistente con el tamano real del proyecto.

---

## Hecho en este ciclo

- [x] `DOC-00` Recentrar la documentacion viva en `docs/`.
- [x] `DOC-00b` Crear `STATE`, `ROADMAP` y `backlog` como trio base del proyecto.
- [x] `ITER-01a` Corregir duplicados de `benchmark` y `close` en recursos y revalidar build local.
- [x] `ITER-01b` Confirmar por inspeccion que no quedan referencias Kotlin activas a chat history eliminado.

---

## Siguiente iteracion sugerida

La siguiente iteracion deberia centrarse en cerrar el lote tecnico actual antes de abrir otro:

1. thinking/runtime wiring;
2. benchmark y navegacion;
3. smoke tests del puente HTTP;
4. validacion funcional en dispositivo/emulador.
