# Fork Divergences — gallery

> Divergencias deliberadas del fork respecto a `upstream/main`.
> Ultima actualizacion: 2026-03-30.

---

## 1. Puente HTTP local (Every Code)

**Archivos exclusivos del fork:**
- `service/LlmHttpService.kt` — servidor HTTP loopback en `127.0.0.1`
- `data/LlmHttpPrefs.kt` — preferencias del servicio HTTP
- Superficie en `SettingsDialog.kt` (toggle, puerto, logging)

**Razon:** permite que clientes locales (Every Code) invoquen modelos on-device via API REST sin exponer red.

**Endpoints:**
- `GET /ping`
- `GET /v1/models`
- `POST /generate`
- `POST /v1/chat/completions`

---

## 2. Chat history persistente — alineado con upstream

**Archivos eliminados del fork:**
- `data/chathistory/*` (DAO, Room DB, entities, export, crypto, repository)
- `ui/history/*` (ChatHistoryScreen, ChatHistoryViewModel)

**Nota:** upstream tampoco tiene chat history persistente en `main`. Estos archivos fueron codigo propio del fork o de una rama experimental. Su eliminacion deja el fork alineado con upstream en este punto. No es una divergencia activa sino una decision de no reintroducir algo que upstream no tiene.

---

## 3. Thinking mode — parsing de tags en runtime helper

**Archivo modificado:**
- `ui/llmchat/LlmChatModelHelper.kt`

**Divergencia:** upstream no implementa el enrutamiento de thinking tokens; el fork parsea `<think>`/`</think>` tags en `onMessage` y los pasa como `partialThinkingResult` al `ResultListener`. Esto cierra la cadena thinking que upstream deja abierta.

**Impacto:** modelos que emiten tags `<think>` (ej: Gemma-3 thinking) muestran el panel de thinking en la UI.

---

## 4. Benchmark — ruta dedicada desde pantallas de modelo

**Archivos modificados:**
- `GalleryNavGraph.kt` — ruta `benchmark/{modelName}`
- `ModelPageAppBar.kt` — boton de acceso a benchmark
- `ui/benchmark/*` — pantallas presentes

**Divergencia:** upstream no expone benchmark en navegacion todavia; el fork lo conecta directamente.

---

## 5. Documentacion viva en `docs/`

**Archivos exclusivos del fork:**
- `docs/STATE.md`, `docs/ROADMAP.md`, `docs/backlog.md`
- `docs/UPSTREAM_CATCHUP_PLAN.md`
- `docs/FORK_DIVERGENCES.md` (este archivo)

**Razon:** trazabilidad operativa del fork independiente de upstream.
