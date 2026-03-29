# Upstream Catch-Up Plan

## Objective

Bring this fork of `google-ai-edge/gallery` up to date with `upstream/main` as far as practical, while preserving the local Every Code HTTP bridge and keeping loopback behavior stable on `127.0.0.1`.

## Current Fork State

This document reflects the current state of the fork, not the original upstream baseline.

### Product invariants

- Preserve the local HTTP API centered on:
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/service/LlmHttpService.kt`
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/LlmHttpPrefs.kt`
  - the related settings/home UI surfaces that expose or control the bridge
- Keep loopback API behavior stable for local automation and Every Code.
- Do not reintroduce persistent chat history as a product requirement.

### Current integration progress

- Tiny Garden has been restored from upstream under:
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/tinygarden/`
  - `Android/src/app/src/main/assets/tinygarden/`
- Chat history is being actively removed:
  - `data/chathistory/*` deleted in working tree
  - `ui/history/*` deleted in working tree
  - DI/nav/chat/home/settings have been partially unwired
- Thinking/config plumbing has been partially integrated:
  - `Config.kt` includes thinking-related config support
  - `Model.kt` and `ModelAllowlist.kt` expose thinking capability metadata
  - `ChatMessage.kt`, `ChatPanel.kt`, `ChatViewModel.kt`, `MessageBodyThinking.kt` include the UI/data path for thinking
  - imported models can now carry thinking support through `settings.proto`, `ModelImportDialog.kt`, and `ModelManagerViewModel.kt`
- Runtime convergence has materially advanced:
  - `runtime/LlmModelHelper.kt` and `runtime/ModelHelperExt.kt` now exist in the fork
  - `LlmChatModelHelper.kt` implements `LlmModelHelper`
  - `LlmChatViewModel.kt` and `LlmSingleTurnViewModel.kt` use `model.runtimeHelper`
  - `TinyGarden*` and `MobileActions*` now use `runtimeHelper` on their initialization/reset/cleanup paths
- Benchmark groundwork from upstream has been added:
  - `benchmark.proto`
  - `BenchmarkResultsSerializer.kt`
  - `CutoutsSerializer.kt`
  - `ui/benchmark/*`
  - `DataStoreRepository.kt` and `AppModule.kt` now include the base benchmark/cutout persistence plumbing
  - benchmark is not yet wired into navigation or home shell
- GitHub Actions is the primary validation path through:
  - `.github/workflows/android-apk.yml`

### Known local limitations

- Local Android builds may be blocked by missing accepted SDK licenses.
- The thinking UI/data path exists, but full thinking-token propagation still depends on further runtime convergence with upstream.
- Benchmark files and persistence exist in the repo, but the feature is not yet exposed from navigation/home.
- The working tree is frequently dirty during sync work; convergence must assume non-clean baselines.

## Canonical Sync Strategy

### What not to do

- Do **not** attempt a single large `git merge upstream/main` from the current state.
- Do **not** attempt a full-history rebase of the fork onto upstream while the fork still contains in-flight refactors.

### What to do instead

- Use **small themed batches**.
- Prefer **file-by-file adoption** or **targeted cherry-picks**.
- Treat each batch as its own checkpointed phase.
- Push each batch and wait for GitHub Actions before advancing.

## Convergence Phases

### Phase 0 — Stabilize the current refactor set

Goal: finish the work already in progress before pulling more upstream change.

Tasks:

1. Complete chat-history removal and unwiring.
2. Resolve any local regressions introduced during that removal.
3. Confirm no references remain to deleted history modules.
4. Checkpoint the result.

Files to treat as primary in this phase:

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/di/AppModule.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/navigation/GalleryNavGraph.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/HomeScreen.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/SettingsDialog.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatScreen.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatTaskModule.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatViewModel.kt`

Verification gate:

- GitHub Actions `android-apk.yml` green on the stabilization branch.

### Phase 1 — Preserve and harden the HTTP API path

Goal: make the Every Code bridge resilient before deeper upstream adoption.

Tasks:

1. Re-audit `LlmHttpService.kt` against the current helper/runtime APIs.
2. Keep `LlmHttpPrefs.kt` isolated from general app config churn.
3. Validate the settings UI for HTTP port, enable/disable, logging, and fallback behavior.
4. Maintain endpoint compatibility for:
   - `/ping`
   - `/v1/models`
   - `/generate`
   - `/v1/chat/completions`

Primary files:

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/service/LlmHttpService.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/LlmHttpPrefs.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/SettingsDialog.kt`

Verification gate:

- GitHub Actions build green.
- Manual smoke of loopback endpoints after each major runtime/inference batch.

### Phase 2 — Align runtime/build before broader UI sync

Goal: reduce breakage risk by matching upstream’s inference/runtime/build contracts first.

Tasks:

1. Align `build.gradle.kts` and version catalog with upstream where safe.
2. Finish reconciling helper signatures in `LlmChatModelHelper.kt`.
3. Decide how to adopt upstream runtime abstractions without breaking `LlmHttpService`.
4. Restore or align any new runtime/proto/build files required by upstream chat and benchmark flows.

Primary hotspots:

- `Android/src/app/build.gradle.kts`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatViewModel.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/service/LlmHttpService.kt`

Verification gate:

- GitHub Actions build green.
- HTTP bridge still responds correctly after helper/runtime changes.

Current status:

- Partially complete.
- Runtime abstraction is already present and propagated to the main product flows.
- Remaining work in this phase is focused on deeper inference parity and build/dependency convergence, not on introducing the abstraction itself.

### Phase 3 — Adopt upstream files and features with low conflict risk

Goal: reduce diff size by absorbing additive upstream features first.

Candidate blocks:

- runtime helper files that do not overlap fork-only code
- benchmark feature files
- global model manager files
- UI utility components added upstream
- resources and strings with no fork-specific semantics

Strategy:

- Prefer direct file adoption from upstream for files with no fork delta.
- Verify imports and routes afterward rather than trying to batch-merge unrelated hotspots.

Current status:

- In progress.
- Benchmark proto/serializers, data-store plumbing, and `ui/benchmark/*` are already present in the fork.
- The next step for this phase is to connect those pieces safely into the shell only after the required strings/navigation/model assumptions are fully satisfied.

### Phase 4 — Align data/model/config layer

Goal: sync the shared app data layer before the most volatile UI screens.

Primary files:

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Config.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Model.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/ModelAllowlist.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Tasks.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/DataStoreRepository.kt`
- model allowlist JSON files

Guiding rule:

- Upstream structure wins.
- Fork-specific behavior survives only where required for HTTP bridge compatibility or local LiteRT-LM handling.

### Phase 5 — Reach chat/inference/UI parity

Goal: finish convergence in the chat stack after runtime and data layers are stable.

Target areas:

- thinking mode end-to-end
- audio-only messages
- prompt/system-prompt UX
- text copy and chat UX refinements
- `ConfigDialog` parity

Primary files:

- `ui/common/chat/*`
- `ui/llmchat/*`
- `ui/common/ConfigDialog.kt`

Important note:

- The existing thinking pipeline in the fork should be treated as an intermediate scaffold until upstream runtime semantics are fully integrated.

### Phase 6 — Navigation, home shell, and model manager

Goal: adopt upstream shell-level UX after the underlying chat/model layers are stable.

Primary files:

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/navigation/GalleryNavGraph.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/HomeScreen.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/SettingsDialog.kt`
- `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/modelmanager/ModelManagerViewModel.kt`

Rule:

- Take upstream as the visual/structural baseline.
- Reinsert only the minimum fork-specific HTTP surfaces.

### Phase 7 — Peripheral convergence and final cleanup

Goal: close the remaining gap to upstream.

Includes:

- Tiny Garden refresh vs latest upstream
- Mobile Actions refresh
- single-turn screen alignment
- theme/resources parity
- manifest and docs reconciliation

## Checkpoint Structure

Recommended checkpoint names:

- `checkpoint/chat-history-removed`
- `checkpoint/http-api-hardened`
- `checkpoint/runtime-build-aligned`
- `checkpoint/upstream-low-risk-batch`
- `checkpoint/data-layer-aligned`
- `checkpoint/chat-ui-parity`
- `checkpoint/nav-home-aligned`
- `checkpoint/upstream-parity`

Each checkpoint should be pushed and validated by GitHub Actions before the next phase begins.

## GitHub Actions Policy

The repository’s primary validation path is:

- `.github/workflows/android-apk.yml`

Operational rule:

- No phase is considered complete until the workflow is green.

Recommended future improvements:

- add lint to CI
- optionally add release assemble step
- keep APK artifacts per phase branch or tag

## Documentation Policy

This file is the active convergence plan.

`IMPLEMENTATION_PLAN.md` is kept only as a compatibility pointer and should not contain an outdated phase sequence.

`README.md` should describe the fork at a high level, but this file is where the detailed catch-up strategy lives.
