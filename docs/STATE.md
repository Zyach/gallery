# STATE — gallery fork

> Ultima actualizacion: 2026-04-02.

## Resumen

Fork activo de Google AI Edge Gallery para Android on-device AI.

- Version estimada: `0.3.0-alpha`
- Madurez estimada: `38%`
- Enfoque actual: convergencia incremental con upstream sin romper el bridge HTTP local ni Tiny Garden

## Invariantes del fork

1. Bridge HTTP local en loopback (`127.0.0.1`) para Every Code.
2. Chat history persistente fuera de alcance.
3. Tiny Garden preservado.

## Estado validado

- CI verde en `main` para:
  - `Android APK`
  - `Android JVM Tests`
- Validacion E2E on-device ya comprobada para el bridge HTTP:
  - `GET /ping` -> `200`
  - `GET /v1/models` con bearer token -> `200`
  - `POST /generate` con bearer token -> `200`
- Workflows adelantados frente al cutoff de Node.js 20:
  - `checkout`, `setup-java` y `upload-artifact` actualizados
  - `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true` activo
  - `android-actions/setup-android@v3` sigue siendo la unica advertencia no bloqueante

## Arquitectura actual

- App Compose/Hilt con `LlmModelHelper` como contrato runtime.
- El bridge HTTP sigue en `LlmHttpService`, pero la extraccion ya avanza por componentes puros:
  - `LlmHttpApiModels`
  - `LlmHttpRequestAdapter`
  - `LlmHttpModelResolver`
  - `LlmHttpModelFactory`
  - `LlmHttpResponseRenderer`
  - `LlmHttpRouteResolver`
  - `LlmHttpBodyParser`
- Allowlist compartido con `kotlinx.serialization`.
- `DataStoreRepository` ya no depende de `runBlocking`.

## Gaps activos

- `ARCH-01`: la orquestacion principal del bridge sigue en `LlmHttpService`.
- `HTTP-01`: ya hay cobertura JVM y una smoke validacion on-device basica, pero falta cerrar una capa mas formal de smoke tests del servicio vivo.
- `RUNTIME-01`: la convergencia del helper runtime sigue incompleta.
- `ARCH-02` y `ARCH-03`: la unificacion del allowlist esta avanzada, pero no cerrada.
- `STREAM-01`: SSE real token-by-token sigue pendiente.
- `BUILD-02`: signing propio y minify siguen pendientes.

## Politica de validacion

- GitHub Actions es el primer gate remoto.
- Cuando sea viable, los cambios Android se validan tambien con este ciclo E2E on-device:
  - `commit y push`
  - `build verde en GitHub`
  - `descarga del artefacto`
  - `instalacion via Termux API/pm`
  - `verificacion funcional y de rendimiento con logcat dirigido via Shizuku`

## Riesgos activos

- El bridge HTTP debe seguir siendo local-only, autenticado y stateless por request.
- Las iteraciones de `DataStoreRepository` siguen siendo sensibles a carreras de arranque y de persistencia.
- La validacion local de Gradle en este entorno sigue siendo poco fiable por AAPT2/SDK; la referencia principal sigue siendo CI.
