# Support

This repository is a maintained fork of Google AI Edge Gallery.

## Before opening an issue

Read these first:

- [README.md](README.md)
- [docs/README.md](docs/README.md)
- [docs/STATE.md](docs/STATE.md)
- [docs/FORK_DIVERGENCES.md](docs/FORK_DIVERGENCES.md)
- [DEVELOPMENT.md](DEVELOPMENT.md)

## Where to ask what

Use this fork when the topic is about:

- the local HTTP bridge / Every Code integration
- documented fork divergences
- fork-specific CI, releases, packaging, or docs

Prefer upstream when the topic is clearly about:

- generic Google AI Edge Gallery behavior
- upstream UI/UX not changed in this fork
- upstream model support or platform issues unrelated to fork changes

Upstream repository:

- https://github.com/google-ai-edge/gallery

## Bug reports

Use the issue forms in this repository.

Do not attach full public bug-report archives or secrets. If a maintainer needs a full bug report, share it privately and only on request.

## Build and validation expectations

This fork uses GitHub Actions as the primary Android build validation path.

- Main build workflow: `.github/workflows/android-apk.yml`
- Tag-based release workflow: `.github/workflows/release.yml`

If your local environment lacks a full Android SDK, that is expected; use CI results as the authoritative build signal for this fork.
