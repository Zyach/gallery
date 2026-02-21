# AGENTS.md — gallery (Project Agent)

## Propósito
Google AI Edge Gallery — app Android para modelos on-device (fork/estudio).

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

## Workflow (fases)
1. **Análisis**: leer código, entender estado actual, consultar SITEMAP.
2. **Plan**: ≤10 pasos, mostrar diff antes de aplicar.
3. **Ejecución**: cambios mínimos, verificar tras cada paso.
4. **Registro**: si el cambio afecta al stack, notificar al OS Agent para que actualice JOURNAL/SITEMAP.

## Guardarraíles
- Mostrar diff antes de editar.
- Confirmar antes de borrar archivos o ejecutar comandos con side_effects != "none".
- Iteración 1 en modo auto; escalar si hay ambigüedad.
