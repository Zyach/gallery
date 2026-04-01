# Google AI Edge Gallery ✨

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android APK](https://github.com/Zyach/gallery/actions/workflows/android-apk.yml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/android-apk.yml)
[![Release APK](https://github.com/Zyach/gallery/actions/workflows/release.yml/badge.svg)](https://github.com/Zyach/gallery/actions/workflows/release.yml)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/Zyach/gallery)](https://github.com/Zyach/gallery/releases)

**Explore, Experience, and Evaluate the Future of On-Device Generative AI with Google AI Edge.**

> Maintained fork of [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) with a local-only HTTP bridge for Every Code and additional fork governance in `docs/`.

The Google AI Edge Gallery is an experimental app that puts the power of cutting-edge Generative AI models directly into your hands, running entirely on your Android *(available now)* and iOS *(available now via TestFlight)* devices. Dive into a world of creative and practical AI use cases, all running locally, without needing an internet connection once the model is loaded. Experiment with different models, chat, ask questions with images and audio clip, explore prompts, and more!

## Personal note from the project owner

I am not a software developer and I do not know programming languages professionally. This project has been built purely through vibe coding, iterative testing, and continuous refinement with AI assistance.

> [!IMPORTANT]
> You must uninstall all previous versions of the app before installing this one. Past versions will no longer be working and supported.

## iOS Testing via TestFlight

We're excited to announce that the app is now available for testing on iOS through TestFlight! We invite you to be among the first to try it out and share your feedback.

***How to Join***:

- Follow this [**public invitation link**](https://testflight.apple.com/join/nAtSQKTF) to get access.

- Availability: Access is on a first-come, first-served basis. TestFlight currently limits the number of testers to 10,000.
  
- Supported device models: iOS devices with at least 6GB of RAM.

We appreciate your help with this early testing phase. Your feedback is invaluable as we work to improve the app. Once we've gathered and addressed all the feedback, we aim to officially launch on the App Store early 2026.

<img width="480" alt="01" src="https://github.com/user-attachments/assets/09dbcf7e-a298-4063-920e-bfc88591f4a2" />
<img width="480" alt="02" src="https://github.com/user-attachments/assets/e2986bba-f807-42e1-9d5e-a5a978fa97e9" />
<img width="480" alt="03" src="https://github.com/user-attachments/assets/ad3aa9ab-e3b6-4a12-bbd4-885bb202aa0f" />
<img width="480" alt="04" src="https://github.com/user-attachments/assets/6441e752-e5f5-4753-9611-fa0122cdae49" />
<img width="480" alt="05" src="https://github.com/user-attachments/assets/a5ebcf15-640a-4c11-93ce-b92fe365f1a3" />
<img width="480" alt="06" src="https://github.com/user-attachments/assets/973c7a66-1906-400e-8fac-ee9b13b21aa1" />
<img width="480" alt="07" src="https://github.com/user-attachments/assets/d3227bc6-8d78-47a1-bbfa-93f009117882" />

## ✨ Core Features

*   **📱 Run Locally, Fully Offline:** Experience the magic of GenAI without an internet connection. All processing happens directly on your device.
*   **🤖 Choose Your Model:** Easily switch between different models from Hugging Face and compare their performance.
*   **🌻 Tiny Garden**: Play an experimental and fully offline mini game that uses natural language to plant, water, and harvest flowers.
*   **📳 Mobile Actions**: Use our [open-source recipe](https://github.com/google-gemini/gemma-cookbook/blob/main/FunctionGemma/%5BFunctionGemma%5DFinetune_FunctionGemma_270M_for_Mobile_Actions_with_Hugging_Face.ipynb) to learn model fine-tuning, then load it in app to unlock offline device controls.
*   **🖼️ Ask Image:** Upload images and ask questions about them. Get descriptions, solve problems, or identify objects.
*   **🎙️ Audio Scribe:** Transcribe an uploaded or recorded audio clip into text or translate it into another language.
*   **✍️ Prompt Lab:** Summarize, rewrite, generate code, or use freeform prompts to explore single-turn LLM use cases.
*   **💬 AI Chat:** Engage in multi-turn conversations.
*   **📊 Performance Insights:** Real-time benchmarks (TTFT, decode speed, latency).
*   **🧩 Bring Your Own Model:** Test your local LiteRT `.litertlm` models.
*   **🔗 Developer Resources:** Quick links to model cards and source code.

## 🏁 Get Started in Minutes!

1. **Check OS Requirement**: Android 12 and up
2.  **Download the App:**
    - Install the app from [Google Play](https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery).
    - For users without Google Play access: install the apk from the [**latest release**](https://github.com/google-ai-edge/gallery/releases/latest/)
3.  **Install & Explore:** For detailed installation instructions (including for corporate devices) and a full user guide, head over to our [**Project Wiki**](https://github.com/google-ai-edge/gallery/wiki)!

## 🛠️ Technology Highlights

*   **Google AI Edge:** Core APIs and tools for on-device ML.
*   **LiteRT:** Lightweight runtime for optimized model execution.
*   **LLM Inference API:** Powering on-device Large Language Models.
*   **Hugging Face Integration:** For model discovery and download.

## ⌨️ Development

Check out the [development notes](DEVELOPMENT.md) for instructions about how to build the app locally.

## 🤝 Feedback

This is an **experimental Beta release**, and your input is crucial!

*   🐞 **Found a bug in this fork?** [Open an issue here](https://github.com/Zyach/gallery/issues/new/choose)
*   💡 **Have a fork-specific idea?** [Start with the issue chooser](https://github.com/Zyach/gallery/issues/new/choose)
*   🧭 **Need context first?** Read [docs/README.md](docs/README.md), [docs/STATE.md](docs/STATE.md), and [docs/FORK_DIVERGENCES.md](docs/FORK_DIVERGENCES.md)

## 📄 License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.

## 🔗 Useful Links

*   [**Project Wiki (Detailed Guides)**](https://github.com/google-ai-edge/gallery/wiki)
*   [Hugging Face LiteRT Community](https://huggingface.co/litert-community)
*   [LLM Inference guide for Android](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
*   [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
*   [Google AI Edge Documentation](https://ai.google.dev/edge)

---

## ✨ Fork changes

This fork adds:

- **HTTP API loopback** — `LlmHttpService` on `127.0.0.1` for local-only inference via Every Code
- **Thinking mode** — `<think>` tag parsing routes thinking tokens to collapsible UI panel
- **Benchmark in navigation** — accessible from model screens

## Fork governance

- Living docs index: [docs/README.md](docs/README.md)
- Current status: [docs/STATE.md](docs/STATE.md)
- Roadmap: [docs/ROADMAP.md](docs/ROADMAP.md)
- Backlog: [docs/backlog.md](docs/backlog.md)
- Deliberate divergences from upstream: [docs/FORK_DIVERGENCES.md](docs/FORK_DIVERGENCES.md)
- Security policy: [SECURITY.md](SECURITY.md)
- Support guidance: [SUPPORT.md](SUPPORT.md)

**Version: 0.3.0-alpha** | [Full status](docs/STATE.md) | [Roadmap](docs/ROADMAP.md) | [Backlog](docs/backlog.md) | [Divergences](docs/FORK_DIVERGENCES.md)
