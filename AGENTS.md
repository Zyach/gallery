# AGENTS.md — gallery (Project Agent)

## Propósito
Google AI Edge Gallery — app Android para modelos on-device (fork).
Base: upstream v1.0.11. 89 tests JVM. CI verde.

## Documentación viva
- Estado: `docs/STATE.md`
- Roadmap + backlog: `docs/ROADMAP.md`
- Divergencias: `docs/FORK_DIVERGENCES.md`

## Control plane (read-only)
- SITEMAP: `~/agent-core/SITEMAP.json` (leer, nunca escribir)
- JOURNAL: `~/agent-core/JOURNAL.md` (leer para contexto)

## Capabilities disponibles (vía SITEMAP)
| Capability | Layer | Invocación |
|---|---|---|
| android_rish | android_rish | `~/.local/bin/rish-via-termux <cmd>` |
| android_dumpsys | android_rish | `~/.local/bin/termux-dumpsys <service>` |
| android_control | termux_host | `~/.local/bin/androidctl <subcommand>` |
| notify | termux_host | vía `termux-cmd.sh` |
| host_command_exec | termux_host | `~/.local/bin/termux-cmd.sh <cmd>` |
| battery_thermal_status | ubuntu_guest | `python3 ~/agent-core/sensors.py` |

## Reglas de ejecución
- Capa HOST (termux-*, rish, am/pm/dumpsys): delegar vía `termux-cmd.sh` o `termux-bridge.sh`.
- No modificar archivos fuera de este repo.
- No acciones destructivas sin confirmación.
- Directriz operativa para cambios Android: usar el siguiente ciclo de autodepuración y validación E2E on-device cuando sea viable: `commit y push` -> `compilación exitosa de la build en GitHub` -> `descarga de la build generada` -> `instalación mediante termux-api/pm` -> `evaluación del comportamiento esperado y del rendimiento mediante captura dirigida de logcat con precisión vía Shizuku server`.
- La validación en GitHub Actions sigue siendo el primer gate remoto; la validación on-device E2E es la capa de comprobación funcional y de rendimiento posterior cuando el cambio lo requiera.

## Workflow (fases)
1. **Análisis**: leer código, entender estado actual, consultar SITEMAP.
2. **Plan**: ≤10 pasos, mostrar diff antes de aplicar.
3. **Ejecución**: cambios mínimos, verificar tras cada paso.
4. **Registro**: si el cambio afecta al stack, notificar al OS Agent para que actualice JOURNAL/SITEMAP.

## Guardarraíles
- Mostrar diff antes de editar.
- Confirmar antes de borrar archivos o ejecutar comandos con side_effects != "none".
- Iteración 1 en modo auto; escalar si hay ambigüedad.

## Tickets de sistema (obligatorio ante errores de stack)

Si detectas **errores en herramientas/scripts/wrappers/paquetes del stack** (por ejemplo `termux-cmd.sh`, `rish-via-termux`, `thermal-guard`, `watchdog`, `androidctl`, `work-start.sh`, `sensors.py`, `wcl-*`, etc.), **no intentes corregirlos**. Deposita un ticket en el buzón central de AgentOS.

### Cómo reportar

1. Crea un archivo en el buzón de tickets de AgentOS:
   - Ruta: `~/NeoOS/agent-core/inbox/tickets/<proyecto>-<YYYYMMDD>-<HHMMSS>.md`
   - Schema: `~/NeoOS/agent-core/inbox/tickets/_schema.md`
   - Template: `~/NeoOS/agent-core/inbox/tickets/_template.md`
2. Copia el template, rellena el frontmatter YAML y las secciones.
3. El campo `estado` siempre empieza como `abierto`. Solo AgentOS cambia el estado.
4. Si el impacto es alto, notifica adicionalmente:

```bash
termux-cmd.sh 'termux-notification --title "Ticket AgentOS" --content "Ticket de <proyecto>: <resumen>"'
```

### Reglas
- **Solo escritura** en `inbox/tickets/`: crear tu ticket. No modificar tickets de otros proyectos.
- **No modificar** otras carpetas de `~/NeoOS/agent-core/`.
- AgentOS revisa `inbox/tickets/` y actúa según prioridad.
