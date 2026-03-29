# STATE — gallery

> Documento vivo del estado real del fork.
> Ultima verificacion documental: 2026-03-30.
> Ultima verificacion tecnica: 2026-03-30.

---

## Resumen ejecutivo

- `gallery` sigue siendo un fork activo de Google AI Edge Gallery orientado a Android.
- La prioridad del fork es converger con `upstream/main` sin perder tres invariantes locales: puente HTTP loopback para Every Code, eliminacion del subsistema de chat history y preservacion de Tiny Garden.
- La documentacion del proyecto pasa a centralizarse en `docs/`.
- El arbol de trabajo esta en una fase intermedia de integracion: hay adopcion parcial del runtime helper nuevo, restauracion de Tiny Garden, base de benchmark en el repo y retirada amplia de chat history.

---

## Invariantes del fork

### 1. Puente HTTP local

- El servicio [`LlmHttpService`](../Android/src/app/src/main/java/com/google/ai/edge/gallery/service/LlmHttpService.kt) sigue siendo una capacidad propia del fork.
- La preferencia y la superficie de configuracion viven en [`LlmHttpPrefs`](../Android/src/app/src/main/java/com/google/ai/edge/gallery/data/LlmHttpPrefs.kt) y [`SettingsDialog`](../Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/SettingsDialog.kt).
- El contrato esperado sigue siendo local sobre `127.0.0.1`.

### 2. Chat history fuera de alcance

- El subsistema historico persistente de chat se esta retirando del fork.
- Ya no debe considerarse parte de la direccion del producto ni de la estrategia de convergencia.
- La existencia de historiales de entrada ligeros en settings/UI no reabre el alcance de `data/chathistory` ni `ui/history`.

### 3. Tiny Garden preservado

- Tiny Garden vuelve a estar presente en assets, task module y pantalla propia.
- La integracion debe mantenerse funcional durante la convergencia con upstream.
- No debe bloquearse la UI principal del experimento por permisos accesorios.

---

## Estado tecnico actual

### Base del producto

- App Android Compose/Hilt con codigo principal bajo `Android/src/app/src/main/java/...`.
- El repo mantiene README y notas de desarrollo en raiz, pero la documentacion operativa pasa a `docs/`.
- Existe workflow CI para generar APK debug en [`.github/workflows/android-apk.yml`](../.github/workflows/android-apk.yml).

### Integraciones y cambios visibles en curso

- `runtime/` ya existe y el acceso via `model.runtimeHelper` esta parcialmente adoptado.
- Tiny Garden ya tiene assets empaquetados y codigo propio en `customtasks/tinygarden/`.
- La base de benchmark existe en `benchmark.proto`, serializers y `ui/benchmark/*`.
- Chat history persistente aparece como eliminado del arbol de trabajo.

### Gaps confirmados hoy

- `README.md` aun apuntaba a `UPSTREAM_CATCHUP_PLAN.md` e `IMPLEMENTATION_PLAN.md` en raiz, pero esos documentos ahora viven en `docs/`.
- No hay evidencia en el arbol actual de una ruta de navegacion activa para `ui/benchmark/*`.
- Benchmark sigue sin exposicion clara en navegacion/home, aunque la base tecnica ya existe en el repo.
- Thinking mode queda verificado solo a nivel estatico en este turno; falta validacion funcional en ejecucion.

---

## Riesgos activos

### Riesgo de compilacion

- La migracion a `runtimeHelper` puede romper llamadas antiguas si quedan firmas desalineadas.
- El siguiente riesgo de compilacion probable ya no esta en recursos duplicados, sino en desajustes finos de runtime/helper cuando se sigan absorbiendo cambios upstream.

### Riesgo funcional

- Thinking mode puede quedar visualmente roto si no se propagan chunks parciales o si la UI expande/colapsa mal durante streaming.
- Benchmark puede parecer “presente” en el repo pero seguir sin experiencia accesible al usuario final.

### Riesgo documental

- Si `README.md`, `STATE`, `ROADMAP` y `backlog` divergen, se pierde trazabilidad sobre que esta realmente decidido y que esta solo en transito.

---

## Estado de validacion

### Verificado por inspeccion de repo

- Presencia del workflow de build APK.
- Presencia del puente HTTP local y sus preferencias.
- Presencia de Tiny Garden en assets y codigo.
- Presencia del runtime abstraction layer.
- Ausencia actual de los planes antiguos en raiz.
- Ausencia de referencias activas a `ui/history` y `data/chathistory` en el arbol Kotlin inspeccionado.
- Recursos `benchmark` y `close` sin duplicados tras la correccion de esta iteracion.

### Verificado en ejecucion local en este turno

- `./gradlew :app:assembleDebug` no devolvio fallo en esta pasada de validacion local.

### No verificado en este turno

- Smoke tests HTTP (`/ping`, `/v1/models`, `/generate`, `/v1/chat/completions`)
- Navegacion visual completa en dispositivo/emulador
- Thinking mode en ejecucion real con streaming parcial

---

## Decision documental vigente

- `docs/STATE.md` describe la realidad observable del fork.
- `docs/ROADMAP.md` define direccion y fases.
- `docs/backlog.md` mantiene la cola priorizada de trabajo.
- `docs/UPSTREAM_CATCHUP_PLAN.md` conserva el playbook tecnico detallado de convergencia.
- `docs/IMPLEMENTATION_PLAN.md` queda como puntero de compatibilidad para referencias antiguas.
- `README.md` debe actuar como puerta de entrada corta y apuntar a `docs/` para el estado vivo del proyecto.

---

## Siguiente lectura recomendada

- [`docs/ROADMAP.md`](./ROADMAP.md)
- [`docs/backlog.md`](./backlog.md)
- [`README.md`](../README.md)
