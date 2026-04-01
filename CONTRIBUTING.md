# Contributing

This repository is a maintained fork of Google AI Edge Gallery.

## Contribution model for this fork

Contributions are welcome, but the bar is different from an upstream app repository.

Changes are most useful when they are one of these:

- fork-specific improvements to the local HTTP bridge / Every Code integration
- repository hygiene, CI/CD, release automation, and documentation for this fork
- upstream convergence work that is intentional, scoped, and documented
- bug fixes that preserve the fork invariants described in `docs/FORK_DIVERGENCES.md`

## Before opening a PR

Read these documents first:

- [README.md](README.md)
- [docs/README.md](docs/README.md)
- [docs/STATE.md](docs/STATE.md)
- [docs/FORK_DIVERGENCES.md](docs/FORK_DIVERGENCES.md)

## Fork-vs-upstream rule

If the change is generic and belongs naturally in Google AI Edge Gallery without depending on this fork's deliberate differences, consider sending it upstream instead.

If the change is specific to this fork, explain that clearly in the PR.

## PR expectations

Keep pull requests:

- small and focused
- explicit about risk
- consistent with the living docs
- validated through the repository workflows when Android build validation is needed

When project status, priorities, or architectural direction change, update the living docs together:

- `docs/STATE.md`
- `docs/ROADMAP.md`
- `docs/backlog.md`

## Things not to do

- Do not reintroduce persistent chat history.
- Do not weaken the loopback-only HTTP bridge posture.
- Do not remove Tiny Garden support without an explicit product decision.
- Do not treat this fork as if it were a clean-sheet app unrelated to upstream.
