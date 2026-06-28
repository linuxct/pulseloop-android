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

## Screenshots

PulseLoop ships with **two complete, runtime-switchable UI styles**. Both render the same data and features — they just look and feel different, so you can pick whichever you prefer from **Settings → Appearance**.

### Original design (V1)

The original, high-contrast, purple accents PulseLoop look — the default theme.

| | | |
|:--:|:--:|:--:|
| <img src="https://i.imgur.com/m5qLxFl.png" width="250" alt="Today (light)"><br/>**Today**<br/><sub>Daily dashboard · light</sub> | <img src="https://i.imgur.com/fX8tlY4.png" width="250" alt="Today with extended vitals (dark)"><br/>**Today · extended vitals**<br/><sub>BP &amp; blood-sugar tiles · dark</sub> | <img src="https://i.imgur.com/L2A43Dr.png" width="250" alt="Vitals 24h (light)"><br/>**Vitals · 24h**<br/><sub>Heart rate &amp; SpO₂ with tooltips · light</sub> |
| <img src="https://i.imgur.com/fLpTU6I.png" width="250" alt="Vitals week (light)"><br/>**Vitals · Week**<br/><sub>Weekly HR, SpO₂ &amp; stress · light</sub> | <img src="https://i.imgur.com/TpmZt87.png" width="250" alt="Activity today (dark)"><br/>**Activity · Today**<br/><sub>Steps, calories &amp; workouts · dark</sub> | <img src="https://i.imgur.com/LhmMZGN.png" width="250" alt="Activity week (dark)"><br/>**Activity · Week**<br/><sub>7-day steps &amp; calories · dark</sub> |
| <img src="https://i.imgur.com/mMA8jqc.png" width="250" alt="Sleep night (light)"><br/>**Sleep · Night**<br/><sub>Score &amp; stage architecture · light</sub> | <img src="https://i.imgur.com/t1a0xQM.png" width="250" alt="Sleep week (dark)"><br/>**Sleep · Week**<br/><sub>7-night duration trend · dark</sub> | <img src="https://i.imgur.com/qS0qFre.png" width="250" alt="Settings (dark)"><br/>**Settings**<br/><sub>Profile, ring &amp; goals · dark</sub> |
| <img src="https://i.imgur.com/dTyib3u.png" width="250" alt="Debug (dark)"><br/>**Debug**<br/><sub>Live BLE packet feed · dark</sub> | | |

### Material 3 Expressive with Material You — P4 mode (V2)

Android's Material 3 Expressive design with Material You dynamic color, drawn from your wallpaper on Android 12+.

| | | |
|:--:|:--:|:--:|
| <img src="https://i.imgur.com/uHvYwRI.png" width="250" alt="Today with extended vitals (light)"><br/>**Today · extended vitals**<br/><sub>Step ring + BP &amp; blood sugar · light</sub> | <img src="https://i.imgur.com/8VYEcDn.png" width="250" alt="Today (dark)"><br/>**Today**<br/><sub>Step ring &amp; live timeline · dark</sub> | <img src="https://i.imgur.com/lGvf48S.png" width="250" alt="Vitals 24h (light)"><br/>**Vitals · 24h**<br/><sub>Heart rate &amp; SpO₂ · light</sub> |
| <img src="https://i.imgur.com/agEgHZq.png" width="250" alt="Vitals week (light)"><br/>**Vitals · Week**<br/><sub>Weekly HR, SpO₂ &amp; stress · light</sub> | <img src="https://i.imgur.com/7gu4FoV.png" width="250" alt="Vitals on a past day (light)"><br/>**Vitals · any past day**<br/><sub>Calendar date browsing · light</sub> | <img src="https://i.imgur.com/eqSHv1g.png" width="250" alt="Activity today (light)"><br/>**Activity · Today**<br/><sub>Steps, distance &amp; workouts · light</sub> |
| <img src="https://i.imgur.com/INBIlOn.png" width="250" alt="Choose activity (light)"><br/>**Start a workout**<br/><sub>Choose activity + GPS · light</sub> | <img src="https://i.imgur.com/Q6NECKj.png" width="250" alt="Live workout recording (light)"><br/>**Live recording**<br/><sub>HR zones &amp; live map route · light</sub> | <img src="https://i.imgur.com/cAOfO2x.png" width="250" alt="Sleep night (dark)"><br/>**Sleep · Night**<br/><sub>Score &amp; stage architecture · dark</sub> |
| <img src="https://i.imgur.com/4cnsH3G.png" width="250" alt="AI Coach (dark)"><br/>**AI Coach**<br/><sub>Conversational health Q&amp;A · dark</sub> | <img src="https://i.imgur.com/ONhiABS.png" width="250" alt="Settings (light)"><br/>**Settings**<br/><sub>Vitals opt-in, coach &amp; P4 mode · light</sub> | <img src="https://i.imgur.com/KoTpiTe.png" width="250" alt="Data export (light)"><br/>**Data export**<br/><sub>OpenTelemetry / OTLP setup · light</sub> |

## Features

### Appearance & Themes
PulseLoop ships with two full visual styles you can switch between at any time from Settings:
- **Original** — the original, high-contrast, purple accents PulseLoop look (the default).
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
- **Appearance**: switch between the Original and Material 3 Expressive with Material You themes
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
