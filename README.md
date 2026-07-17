<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Nexus AI Android App

This repository contains an Android app built with Jetpack Compose, Hilt, and Room.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project root and set `GEMINI_API_KEY` (see `/home/runner/work/Nexus/Nexus/.env.example`)
5. (Optional) Configure provider API keys inside the app Settings screen (NVIDIA NIM or custom OpenAI-compatible providers)
6. Run the app on an emulator or physical device

## Build

- CI build command: `./gradlew assembleDebug` (JDK 17)
