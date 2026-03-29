# Implementation Plan

This file is kept as a compatibility entrypoint for older references.

## Status

The previous implementation plan in this file is no longer the canonical one.

The fork has moved forward since that draft:

- Tiny Garden has been restored from upstream.
- The local Every Code HTTP bridge remains a hard requirement.
- Chat history has been explicitly de-scoped by product direction and is being removed from the fork.
- Thinking/config plumbing has been partially reintroduced, but the runtime-side convergence with upstream is still incomplete.
- GitHub Actions is the primary build validation path for this repo.

## Canonical Document

Use [UPSTREAM_CATCHUP_PLAN.md](./UPSTREAM_CATCHUP_PLAN.md) as the living plan for rational convergence with `upstream/main`.

## Working Rule

Do not treat this file as the source of truth for current sequencing.

The active sequencing is:

1. Stabilize the in-flight fork refactors.
2. Preserve and harden the HTTP API path.
3. Align runtime/build and inference signatures with upstream.
4. Adopt upstream in small themed batches.
5. Use GitHub Actions as the required gate after each batch.
