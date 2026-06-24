# PulseLoop Android

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="96" alt="PulseLoop icon">

> **Vibe-coded project notice**
> This app was built entirely with AI assistance (Claude) from scratch — it is not a manually maintained codebase.
> The original app is **[PulseLoopIOS](https://github.com/saksham2001/PulseLoopIOS)**, an iOS app for the same smart ring hardware, which actually did the groundwork for supporting these ring devices in the first place.
> This repository is a full, ground-up Android port of that app, rewritten in Kotlin with Jetpack Compose, targeting the same ring devices and feature set.

## What is this?

PulseLoop Android is a companion app for smart health rings (jring, Colmi R02) that mirrors the functionality of PulseLoopIOS. It connects to your ring over BLE, syncs health data, and provides insights through an AI-powered coach.

## Features

### Onboarding
A five-step guided setup walk you through creating your profile (name, age, sex, height, weight), setting a health baseline, configuring daily goals for steps, sleep, and active minutes, and pairing your ring — all before landing on the main app.

### Ring Pairing & Auto-Reconnect
The pairing screen scans for nearby BLE devices and highlights rings it recognises (jring, Colmi R02) with signal strength (RSSI) so you can pick the right one. Once paired, the app registers the ring with Android's Companion Device Manager, which means the app reconnects automatically whenever the ring comes into range — no manual action required. Connection state (scanning, connecting, connected, disconnected) is reflected everywhere in real time.

### Today Dashboard
The home screen gives you a full picture of your day at a glance:
- A hero insight card summarising the most important health signal of the day
- Six metric tiles covering steps, active calories, heart rate, SpO₂, sleep duration, and distance — each with a mini sparkline of the last 7 readings
- A 7-day trend section with bar or line charts for each metric so you can spot patterns across the week
- A goals progress section showing how close you are to your daily step, sleep, and activity targets
- A chronological timeline of health events throughout the day

### Vitals
Dedicated charts for continuous health metrics:
- **Heart rate**: a line chart of all HR readings; tap any point for a popup showing the exact value and timestamp
- **SpO₂**: a line chart of blood oxygen readings with the same tap-to-inspect behaviour
- **Manual SpO₂ measurement**: a trigger button starts a live measurement from the ring, shown with a pulsing indicator and "Measuring…" status while the ring samples and returns the result

### Sleep
Full sleep architecture visualisation for every night:
- A stage timeline chart showing the chronological sequence of Awake, REM, Light, and Deep sleep blocks — tap anywhere on the chart to see a tooltip with the stage and time at that point (tooltip stays pinned at the top and only tracks horizontally)
- A stage breakdown summary with total time in each stage and a sleep score
- A weekly bar chart of total sleep duration across the past 7 nights, letting you see sleep consistency at a glance

### Activity
Daily activity tracking and workout history:
- A daily summary card with steps, distance, calories, and active minutes for today
- 7-day bar charts for steps and calories to track weekly trends
- A scrollable list of all recorded workout sessions ordered by date, with activity type, duration, and distance

### Live Workout Recording
Start a workout session from the Record tab:
- Choose from multiple activity types
- See live stats during the workout: elapsed time, current heart rate, step count, distance, and estimated calories
- GPS route is recorded continuously throughout the session
- After finishing, a summary screen shows total stats and an interactive map of the GPS route you travelled

### AI Coach
A conversational health coach powered by OpenAI:
- Full chat interface with message history persisted across sessions
- Streaming responses with a live typing cursor so you see the answer appear in real time
- The coach has access to 15 data tools covering heart rate trends, SpO₂ trends, sleep summaries, activity summaries, step goals, workout history, and more — it queries your actual data before answering
- Responses include a title, summary, optional bullet points, an embedded trend chart with labelled axes where relevant, a confidence level (high / medium / low / insufficient data), optional data quality notes, and safety callouts for any health-sensitive observations
- Follow-up suggestion chips appear after each answer so you can dig deeper with one tap
- Start a new conversation or clear history at any time from the header
- Requires signing in to your OpenAI account from Settings

### Settings
- **Profile**: view and edit your name, age, biological sex, height, and weight — used for calorie estimation (Mifflin-St Jeor BMR) and personalised coach context
- **Goals**: configure daily step target, sleep target, and active-minutes target; used for progress tracking on the Today screen
- **Ring management**: view paired ring name, MAC address, battery level, and connection state; manually trigger a sync or forget (unpair) the ring
- **AI Coach**: connect or disconnect your OpenAI account; shows current connection status and the model in use
- **About**: app version

### Debug & Diagnostics
A developer-facing debug screen accessible from the header:
- A live feed of raw BLE packets received from the ring (up to the last 200), useful for verifying protocol decoding
- A wearable event log (up to the last 100 entries) showing higher-level events like syncs and connections
- Export all diagnostics to a JSON file and share it via the system share sheet for offline analysis
- Clear all logs with a single tap

## Supported Devices

| Device | Heart Rate | SpO₂ | Steps | Sleep | Activity |
|--------|-----------|-------|-------|-------|----------|
| jring | ✓ | Manual trigger | ✓ | ✓ | ✓ |
| Colmi R02 | ✓ | ✓ | ✓ | ✓ | ✓ |

> **jring note**: SpO₂ on jring is a manual-only measurement — the ring does not store SpO₂ in its history. Tapping "Measure SpO₂" in the Vitals screen triggers a live reading from the ring.

## Requirements

- Android 15+ (API 35)
- A supported ring device (jring or Colmi R02)
- An OpenAI account to use the AI Coach feature

## Building

1. Clone the repository
2. Create a `local.properties` file at the project root with:
   ```
   sdk.dir=/path/to/your/Android/Sdk
   MAPS_API_KEY=your_google_maps_api_key
   ```
3. Open in Android Studio and run, or build via Gradle:
   ```
   ./gradlew assembleDebug
   ```
