Neural Processing Units (NPUs) offer specialized hardware blocks optimized for deep learning workloads. They are increasingly available in modern systems on a chip (SoCs), especially on mobile devices. Their high-performing nature makes them a great fit for running LLM inference.

[LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)is a C++ library designed to efficiently run language model pipelines on a wide range of devices, from mobile phones to embedded systems. It provides developers with the tools to create and deploy sophisticated language model workflows, now with seamless NPU integration.

## NPU Vendors

LiteRT-LM supports running LLMs using NPU acceleration with the following vendors. Choose the instructions depending on which vendor you would like to try:

- [Qualcomm AI Engine Direct](https://ai.google.dev/edge/litert/next/litert_lm_npu#qualcomm)
- [MediaTek NeuroPilot](https://ai.google.dev/edge/litert/next/litert_lm_npu#mediatek)

## Quick Start

1. Follow the[Prerequisites](https://github.com/google-ai-edge/LiteRT-LM?tab=readme-ov-file#prerequisites)to set up the environment and repository.
2. Ensure`adb`is installed and a device is connected.
3. See the[Quick Start](https://github.com/google-ai-edge/LiteRT-LM?tab=readme-ov-file#quick-start)and the`litert_lm_main`[command line demo](https://github.com/google-ai-edge/LiteRT-LM?tab=readme-ov-file#command-line-demo-usage-).

### Qualcomm AI Engine Direct

<br />

**Step 1:** Download the`.litertlm`model

<br />

Download a`.litertlm`matching your SoC (examples below). You can query your device SoC:  

    SOC_MODEL=$(adb shell getprop ro.soc.model | tr '[:upper:]' '[:lower:]')
    echo "https://huggingface.co/litert-community/Gemma3-1B-IT/blob/main/Gemma3-1B-IT_q4_ekv1280_${SOC_MODEL}.litertlm"

|   Model   |  SoC   |   Quantization    | Context size | Model Size (MB) |                                                      Download                                                      |
|-----------|--------|-------------------|--------------|-----------------|--------------------------------------------------------------------------------------------------------------------|
| Gemma3-1B | SM8750 | 4-bit per-channel | 1280         | 658             | [download](https://huggingface.co/litert-community/Gemma3-1B-IT/blob/main/Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm) |
| Gemma3-1B | SM8650 | 4-bit per-channel | 1280         | 658             | [download](https://huggingface.co/litert-community/Gemma3-1B-IT/blob/main/Gemma3-1B-IT_q4_ekv1280_sm8650.litertlm) |
| Gemma3-1B | SM8550 | 4-bit per-channel | 1280         | 657             | [download](https://huggingface.co/litert-community/Gemma3-1B-IT/blob/main/Gemma3-1B-IT_q4_ekv1280_sm8550.litertlm) |

<br />

<br />

<br />

**Step 2:**Download and extract the QAIRT libraries

<br />

Download the QAIRT SDK, extract it, and set`QAIRT_ROOT`:  

    unzip <your_file.zip> -d ~/
    QAIRT_ROOT=~/qairt/2.34.0.250424

<br />

<br />

<br />

**Step 3:**Build the LiteRT-LM runtime / libraries

<br />

Install Android NDK r28b+ and build:  

    bazel build --config=android_arm64 //runtime/engine:litert_lm_main
    bazel build --config=android_arm64 \
        @litert//litert/vendors/qualcomm/dispatch:dispatch_api_so

<br />

<br />

<br />

**Step 4:**Run the model on device

<br />

Set your device path and push assets:  

    export DEVICE_FOLDER=/data/local/tmp/
    adb shell mkdir -p $DEVICE_FOLDER
    export MODEL_PATH=<path-to-model.litertlm>
    adb push $MODEL_PATH $DEVICE_FOLDER/model.litertlm
    adb push $QAIRT_ROOT/lib/aarch64-android/* $DEVICE_FOLDER/
    adb push bazel-bin/runtime/engine/litert_lm_main $DEVICE_FOLDER/
    adb shell chmod +x $DEVICE_FOLDER/litert_lm_main

Run:  

    adb shell "cd $DEVICE_FOLDER && \
      QAIRT_ROOT=$DEVICE_FOLDER \
      ./litert_lm_main --model=model.litertlm \
      --prompt='Explain the history of LiteRT in 3 bullet points' \
      --device=qualcomm_npu --rounds=1"

<br />

<br />

### MediaTek NeuroPilot

<br />

**Steps overview**

<br />

MediaTek flow mirrors Qualcomm: use a`.litertlm`built for your SoC, include NeuroPilot runtime libraries, build`litert_lm_main`, push assets, and run with`--device=mediatek_npu`.

<br />

<br />