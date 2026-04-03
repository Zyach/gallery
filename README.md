# gallery fork

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build Android APK](https://github.com/Zyach/gallery/actions/workflows/build_android.yaml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/build_android.yaml)

Fork de [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) que añade un bridge HTTP local para inferencia on-device y mejoras de calidad sobre upstream.

## Qué añade este fork

- **HTTP bridge local** — servicio en `127.0.0.1` con auth Bearer, SSE y tool calls para Every Code
- **65 tests JVM** — upstream tiene cero
- **CI completo** — tests + lint + debug/release + artifacts (upstream solo hace `assembleRelease`)
- **DataStoreRepository async** — eliminado `runBlocking` (upstream tiene TODO abierto `b/423700720`)
- **Error handling** — errores visibles en single-turn; reset session con max retries

## Pendiente de adoptar de upstream

- Agent Chat con Skills importables
- Thinking tokens via SDK nativo (`message.channels["thought"]`)

## Estado

**App v1.0.11** (en paridad con upstream) · [Estado](docs/STATE.md) · [Roadmap](docs/ROADMAP.md) · [Divergencias](docs/FORK_DIVERGENCES.md)

## Build

CI principal: [`build_android.yaml`](.github/workflows/build_android.yaml) — build debug + release + artifacts.

Build local: Android Studio o `./gradlew assembleDebug` desde `Android/src/`.

## Licencia

Apache License 2.0. Ver [LICENSE](LICENSE).
