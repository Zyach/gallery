# Gallery Checkpoint – 2026-01-21 (NPU integration)

## Estado del repo
- Rama: `main`
- Cambios sin commit:
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/Types.kt`
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/data/ModelAllowlist.kt`
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/home/ModelImportDialog.kt`
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/modelmanager/ModelManagerViewModel.kt`
  - `Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt`
  - `model_allowlist.json`
  - `Android/src/app/src/main/assets/model_allowlist.json`

## Contexto reciente
- Se agregó soporte de acelerador NPU en UI y allowlist, con fallback automático NPU→GPU→CPU en `LlmChatModelHelper`.
- Se extrajo QAIRT `v2.40.0.251030.zip` y se copiaron libs arm64 a `Android/src/app/src/main/jniLibs/arm64-v8a/` (excluidas del repo).
- Modelo objetivo: `Gemma3-1B-IT_q4_ekv1280_sm8550.litertlm` (SM8550, S23).

## Work-in-progress / Riesgos
- Falta validar en dispositivo que NPU inicializa correctamente y que el fallback funciona.
- Allowlist actualizada para `Gemma3-1B-IT` con `accelerators: npu,gpu,cpu`.

## Próximos pasos sugeridos
1) Decidir estrategia para FunctionGemma: bypass de tool_call sintético o runtime específico.
2) Definir heurística de `tool_choice` (auto/required) y, si aplica, activar streaming de tool_calls.
3) Añadir tests/manual script de validación HTTP.
