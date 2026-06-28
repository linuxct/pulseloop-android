<p align="center" width="100%">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="logo"><br/>
</p>

# PulseLoop for Android<br/> [![Latest Version](https://img.shields.io/github/v/release/linuxct/pulseloop-android)](https://github.com/linuxct/pulseloop-android/releases/latest) ![Compatibility](https://img.shields.io/badge/compat-API%2035%2B-brightgreen)

> **Vibe-coded project notice**
> This app was built entirely with AI assistance (Claude) from scratch — it is not a manually maintained codebase.
> The original app is **[PulseLoopIOS](https://github.com/saksham2001/PulseLoopIOS)**, an iOS app for the same smart ring hardware, which actually did the groundwork for supporting these ring devices in the first place.
> This repository started as a full, ground-up Android port of that app, rewritten in Kotlin with Jetpack Compose, and has since grown its own feature set on top.

## What is this?

PulseLoop Android is a companion app for smart health rings (jring, Colmi R02). It connects to your ring over BLE, syncs health data, lets you browse your history day by day, surfaces insights through an AI-powered coach, and can export everything to your own observability stack.

## What's new in 2.0.0

The headline additions in this release:

- **Two complete UI themes** — a **Legacy** look and a **Material You** look (dynamic wallpaper-based color), switchable at runtime, with the home-screen launcher icon swapping to match.
- **Data export to OpenTelemetry (OTLP)** — push all of your health data to any OTLP/HTTP endpoint (Grafana, Prometheus/Mimir, VictoriaMetrics, ClickHouse, …) and graph it however you like.
- **Extended ring vitals** — blood pressure, blood sugar, stress, and fatigue (jring), read alongside SpO₂, with calibration and an explicit opt-in.
- **Browse your history** — a built-in date/calendar picker lets you scroll back to any past day on the Vitals, Activity, and Sleep screens.

## Features

### Appearance & Themes
PulseLoop ships with two full visual styles you can switch between at any time from Settings:
- **Legacy** — the original, high-contrast PulseLoop look (the default).
- **Material 3 Expressive + Material You (P4 mode)** — Android's Material 3 Expressive design with Material You's dynamic, wallpaper-derived color on Android 12+.

Switching styles re-themes the entire app instantly, and the app's home-screen launcher icon automatically swaps to the matching icon.

### Onboarding
A five-step guided setup walks you through creating your profile (name, age, sex, height, weight), setting a health baseline, configuring daily goals for steps, sleep, and active minutes, and pairing your ring — all before landing on the main app.

### Ring Pairing & Auto-Reconnect
The pairing screen scans for nearby BLE devices and highlights rings it recognizes (jring, Colmi R02) with signal strength (RSSI) so you can pick the right one. Once paired, the app registers the ring with Android's Companion Device Manager, which means the app reconnects automatically whenever the ring comes into range — no manual action required. Connection state (scanning, connecting, connected, disconnected) is reflected everywhere in real time.

### Today Dashboard
The home screen gives you a full picture of your day at a glance:
- A hero insight card summarising the most important health signal of the day
- Metric tiles covering steps, active calories, heart rate, SpO₂, sleep duration, and distance — each with a mini sparkline of the last 7 readings — plus optional blood-pressure and blood-sugar tiles when the extended-vitals feature is enabled
- A 7-day trend section with bar or line charts for each metric so you can spot patterns across the week
- A goals progress section showing how close you are to your daily step, sleep, and activity targets
- A chronological timeline of health events throughout the day

### Vitals
Dedicated charts for continuous health metrics, all with tap-to-inspect tooltips, a dotted reference guideline, and 00:00 / 06:00 / 12:00 / 18:00 time markers:
- **Heart rate**: a line chart of all HR readings; tap any point for a popup showing the exact value and timestamp
- **SpO₂**: a line chart of blood oxygen readings; since the ring does not store SpO₂ in its own history, the app automatically triggers a live measurement on every sync cycle and saves the result
- **Blood pressure** (jring, opt-in): a dual-line systolic/diastolic chart with a combined `123 / 88` tooltip and clinical zone coloring
- **Blood sugar** (jring, opt-in): an estimated glucose chart with in-range / high / low zone coloring
- **Stress & fatigue** (jring): 0–100 scores with plain-language explanations of what they mean
- **Per-characteristic measurement**: each card has its own Measure button to trigger a live reading on demand, shown with a pulsing "Measuring…" indicator
- **Browse history**: pick any past date to review that day's vitals

> Blood pressure and blood sugar are *estimates* produced by an inexpensive ring sensor. They are **off by default**, hidden behind a Settings opt-in, and always treated as low-confidence — including by the AI Coach.

### Sleep
Full sleep architecture visualisation for every night:
- A stage timeline chart showing the chronological sequence of Awake, REM, Light, and Deep sleep blocks — tap anywhere for a tooltip with the stage and time at that point
- A stage breakdown summary with total time in each stage and a sleep score, with naps and awake gaps handled correctly
- A weekly bar chart of total sleep duration across the past 7 nights
- **Browse history**: pick any past night to review its full breakdown

### Activity
Daily activity tracking and workout history:
- A daily summary card with steps, distance, calories, and active minutes
- 7-day bar charts for steps and calories to track weekly trends
- A scrollable list of all recorded workout sessions ordered by date, with activity type, duration, and distance
- **Browse history**: pick any past date to review that day's activity

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
- The coach has access to a broad suite of data tools — heart rate, SpO₂, sleep, activity, steps and goals, workout history, the extended vitals, and personal memory — and queries your actual data before answering
- Responses include a title, summary, optional bullet points, an embedded trend chart with labelled axes where relevant, a confidence level (high / medium / low / insufficient data), optional data-quality notes, and safety callouts for any health-sensitive observations
- Follow-up suggestion chips appear after each answer so you can dig deeper with one tap
- The coach is only aware of blood pressure and blood sugar when you have explicitly enabled the extended-vitals feature
- Configurable OpenAI model; start a new conversation or clear history at any time
- Requires signing in to your OpenAI account from Settings

### Data Export (OpenTelemetry)
Send your health data to your own observability backend as OTLP/HTTP gauge metrics:
- Works with any OTLP-compatible endpoint — Grafana Alloy / Grafana Cloud, Prometheus/Mimir, VictoriaMetrics, ClickHouse, and more
- Authentication options: none, Basic, Bearer token, or a custom header (e.g. multi-tenant `X-Scope-OrgID`); credentials are stored **encrypted** on-device
- **Incremental export** (only new data since the last run) or a **full backfill** of your entire history
- Optional GPS-route export, a Wi-Fi-only toggle, and a built-in "Test connection" check
- Runs in the background via WorkManager; payloads are hand-built protobuf, gzip-compressed
- Self-contained — no SDKs or vendor lock-in; the exporter speaks the OTLP wire format directly

### Settings
- **Profile**: view and edit your name, age, biological sex, height, and weight — used for calorie estimation (Mifflin-St Jeor BMR), personalised coach context, and pushed to the ring to improve its estimates
- **Goals**: configure daily step, sleep, and active-minutes targets
- **Appearance**: switch between the Legacy and Material You themes
- **Estimated vitals & calibration** (jring): opt in to blood pressure / blood sugar, and calibrate them against a reference cuff / glucometer reading
- **Ring management**: view paired ring name, MAC address, battery level, firmware, and connection state; manually sync or forget the ring
- **AI Coach**: connect or disconnect your OpenAI account and choose the model
- **Data export**: configure the OpenTelemetry endpoint and trigger exports
- **About**: app version and update check

### Debug & Diagnostics
A developer-facing debug screen accessible from the header:
- A live feed of raw BLE packets received from the ring (up to the last 200), useful for verifying protocol decoding
- A wearable event log (up to the last 100 entries) showing higher-level events like syncs and connections
- Export all diagnostics to a JSON file and share it via the system share sheet for offline analysis
- Clear all logs with a single tap

## Supported Devices

| Device | Heart Rate | SpO₂ | Steps | Sleep | Activity | Stress | Blood Pressure | Blood Sugar |
|--------|:----------:|:----:|:-----:|:-----:|:--------:|:------:|:--------------:|:-----------:|
| jring | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓\* | ✓\* |
| Colmi R02 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |

> **\* Estimated, opt-in, low-confidence.** Blood pressure and blood sugar are derived estimates from the jring sensor, off by default and gated behind a Settings opt-in. The jring additionally reports a fatigue score; the Colmi R02 additionally reports HRV and skin temperature.

> **jring SpO₂ note**: The jring does not store SpO₂ readings in its internal history the way it does for heart rate. The app works around this by automatically triggering a live SpO₂ measurement on every sync cycle. An additional on-demand measurement can also be triggered at any time from the Vitals screen.

## Requirements

- Android 15+ (API 35)
- A supported ring device (jring or Colmi R02)
- An OpenAI account to use the AI Coach feature (optional)
- An OTLP/HTTP endpoint to use the Data Export feature (optional)

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
