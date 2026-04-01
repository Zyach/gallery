# gallery fork

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android APK](https://github.com/Zyach/gallery/actions/workflows/android-apk.yml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/android-apk.yml)
[![Release APK](https://github.com/Zyach/gallery/actions/workflows/release.yml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/release.yml)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/Zyach/gallery)](https://github.com/Zyach/gallery/releases)

Maintained fork of [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) focused on preserving a local-only HTTP bridge for Every Code while converging with upstream in controlled batches.

## What this fork is

This repository is not the canonical home of Google AI Edge Gallery. It is a maintained fork with its own operational documentation, CI/release setup, and a small number of deliberate divergences from upstream.

If you want the original project, go to:

- https://github.com/google-ai-edge/gallery

If you want the current state of this fork, stay here and start with the documents below.

## Personal note from the project owner

I am not a software developer and I do not know programming languages professionally. This project has been built purely through vibe coding, iterative testing, and continuous refinement with AI assistance.

## Why this fork exists

This fork exists to keep a few fork-specific capabilities and governance choices that are not treated as part of upstream:

- local HTTP API loopback for Every Code on `127.0.0.1`
- fork-owned operational documentation in `docs/`
- controlled upstream catch-up without reintroducing removed fork experiments

## Deliberate fork differences

The important divergences from upstream are documented here:

- [docs/FORK_DIVERGENCES.md](docs/FORK_DIVERGENCES.md)

Today, the main ones are:

- HTTP bridge in `LlmHttpService` for local-only inference
- Tiny Garden preserved as intended fork state
- chat history kept out of scope and not reintroduced

## Current fork status

- Version: `0.3.0-alpha`
- Build validation: GitHub Actions
- Release flow: tag-driven GitHub Releases

Primary living docs:

- [docs/README.md](docs/README.md)
- [docs/STATE.md](docs/STATE.md)
- [docs/ROADMAP.md](docs/ROADMAP.md)
- [docs/backlog.md](docs/backlog.md)

## Build and release model

This fork assumes GitHub Actions is the authoritative Android build path when local SDK/tooling is unavailable.

- Build workflow: [`.github/workflows/android-apk.yml`](.github/workflows/android-apk.yml)
- JVM tests workflow: [`.github/workflows/jvm-tests.yml`](.github/workflows/jvm-tests.yml)
- Release workflow: [`.github/workflows/release.yml`](.github/workflows/release.yml)
- Releases: https://github.com/Zyach/gallery/releases

## Documentation

Static and living docs are intentionally separated but colocated under `docs/`:

- docs index: [docs/README.md](docs/README.md)
- current project state: [docs/STATE.md](docs/STATE.md)
- planned direction: [docs/ROADMAP.md](docs/ROADMAP.md)
- prioritized queue: [docs/backlog.md](docs/backlog.md)
- upstream convergence playbook: [docs/UPSTREAM_CATCHUP_PLAN.md](docs/UPSTREAM_CATCHUP_PLAN.md)

## Contributing and support

- Contributing guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Support guidance: [SUPPORT.md](SUPPORT.md)
- Security policy: [SECURITY.md](SECURITY.md)
- Issue chooser: https://github.com/Zyach/gallery/issues/new/choose

Use this fork for issues related to:

- the Every Code HTTP bridge
- fork-specific CI, releases, and documentation
- deliberate divergences from upstream

For generic Google AI Edge Gallery behavior that is not specific to this fork, upstream is often the better place.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
