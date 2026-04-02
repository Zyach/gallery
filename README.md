# gallery fork

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android APK](https://github.com/Zyach/gallery/actions/workflows/android-apk.yml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/android-apk.yml)
[![JVM Tests](https://github.com/Zyach/gallery/actions/workflows/jvm-tests.yml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/jvm-tests.yml)

Fork de [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) que añade un bridge HTTP local para inferencia on-device y mejoras de calidad sobre upstream.

## Qué añade este fork

- **HTTP bridge local** — servicio en `127.0.0.1` con auth Bearer, SSE, tool calls para Every Code
- **65 tests JVM** — upstream tiene cero
- **CI completo** — tests + lint + debug/release build + artifacts (upstream solo hace `assembleRelease`)
- **DataStoreRepository async** — eliminado `runBlocking` (upstream tiene TODO abierto)
- **Error handling mejorado** — errores visibles en single-turn, reset con max retries

## Qué tiene upstream que aún no adoptamos

- Agent Chat con Skills importables
- Thinking tokens via SDK nativo (`message.channels["thought"]`)
- FCM push, promo screens

## Estado

**Version: 0.4.0-alpha** · [Estado detallado](docs/STATE.md) · [Roadmap](docs/ROADMAP.md) · [Backlog](docs/backlog.md) · [Divergencias](docs/FORK_DIVERGENCES.md)

## Build

GitHub Actions es el build path principal. Workflows:

- [`android-apk.yml`](.github/workflows/android-apk.yml) — build debug + release + artifacts
- [`jvm-tests.yml`](.github/workflows/jvm-tests.yml) — tests JVM
- [`release.yml`](.github/workflows/release.yml) — releases por tag

Para build local: ver [DEVELOPMENT.md](DEVELOPMENT.md).

## Nota personal

No soy desarrollador profesional. Este proyecto se construye con vibe coding, testing iterativo y asistencia AI.

## Licencia

Apache License 2.0. Ver [LICENSE](LICENSE).
