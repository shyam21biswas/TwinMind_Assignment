# TwinMind — AI Voice Recording & Meeting Summary App

A TwinMind-inspired Android voice recording app that captures audio in real time, transcribes it using **Gemini 2.5 Flash**, and generates structured meeting summaries — all with a clean, premium light UI.

---
## Screenshots

<p align="center">
  <img src="https://drive.google.com/uc?export=view&id=1Mv3Qj7bljR75lNZ5ouSwoTVYAP30VUQE" alt="Screenshot 1" width="180"/>
  <img src="https://drive.google.com/uc?export=view&id=1tqOrbSMyUtfSdZIWkTXzsZxEB93zbdwP" alt="Screenshot 2" width="180"/>
  <img src="https://drive.google.com/uc?export=view&id=1b8ywjaxK2-flGoXQTKc0KmTgGa2ZYwnb" alt="Screenshot 3" width="180"/>
  <img src="https://drive.google.com/uc?export=view&id=1aUpAVGMG5D-goCaSPWw-PVq04T3UWqCB" alt="Screenshot 4" width="180"/>
  <img src="https://drive.google.com/uc?export=view&id=1L2k7acQJuNu1wQAyxMik-7R0Ld4fx8A3" alt="Screenshot 5" width="180"/>
  
</p>

---

## Demo Video

🎥 [Watch Demo Video Part1](https://drive.google.com/file/d/1mQcQapqAKdlBrz682Rj-0cpnmw-nWbhm/view?usp=drive_link)
🎥 [Watch Demo Video Part2](https://drive.google.com/file/d/1TJYgEJx_v-8owsSziZA16azxzTHWd5LR/view?usp=drive_link)


---

## APP LINK

🎥 [APP DEBUG APK](https://drive.google.com/file/d/1orLC8zDQ1EhrHC7SgcCEf-s_A1FOe2NN/view?usp=drive_link)

---
## Architecture

```

UI (Jetpack Compose)
  ↓
ViewModel (StateFlow → UiState)
  ↓
Repository (single source of truth)
  ↓
DAO / Worker / ForegroundService
  ↓
Room DB  ·  Gemini 2.5 Flash API  ·  AudioRecord
```

**Principles:**
- MVVM with Hilt dependency injection
- Room as the single source of truth for all session data
- WorkManager for reliable background transcription & summary (survives app kill)
- ForegroundService for stable audio capture with notification controls
- Coroutines + Flow for fully reactive UI
- No over-engineered domain/use-case layers — simple and practical

---

## Tech Stack

| Layer          | Technology                          |
|----------------|--------------------------------------|
| Language       | Kotlin                               |
| UI             | Jetpack Compose + Material 3         |
| Navigation     | Navigation Compose                   |
| DI             | Hilt                                 |
| Database       | Room                                 |
| Async          | Coroutines + Flow                    |
| Background     | WorkManager + ForegroundService      |
| AI             | Gemini 2.5 Flash (REST API)          |
| Network        | OkHttp                               |
| Min SDK        | 24                                   |

---

## Setup Instructions

### 1. Clone the project

```bash
git clone <repo-url>
cd Shyam_assignment
```

### 2. Configure Gemini API Key

Open (or create) `local.properties` in the project root and add:

```properties
GEMINI_API_KEY=your_gemini_api_key_here
```

> Get a free key from [Google AI Studio](https://aistudio.google.com/apikey)

### 3. Build & Run

Open in Android Studio, sync Gradle, and run on a physical device (API 24+).

> **Important:** Audio recording requires a real device with a microphone. Emulators will not produce real audio input.

---

## Demo Flow

1. **Launch** the app → Dashboard shows seeded sample meetings
2. **Tap "Capture"** (pill-shaped FAB) → Recording screen opens
3. **Grant permissions** when prompted (microphone, notifications)
4. **Tap the record button** → ForegroundService starts, timer counts up live
5. Audio is captured in **30-second WAV chunks** with ~2 s overlap
6. Each finalized chunk is **auto-enqueued for Gemini transcription** via WorkManager
7. **Tap Stop** → Session is saved, navigates to Summary screen
8. Summary screen **auto-triggers Gemini summary generation** from the full transcript
9. View the result: **Title, Summary, Action Items, Key Points**, and full **Transcript**
10. **Return to Dashboard** → Session card shows **"Completed"** status chip

### Dashboard FAB behavior

| State           | FAB appearance                                                       |
|-----------------|----------------------------------------------------------------------|
| **Idle**        | Blue-purple pill: 🎙 **Capture** — tap to start recording            |
| **Recording**   | Two stacked pills:                                                   |
|                 | • Red **Stop** pill — stops recording and navigates to meeting details |
|                 | • Pulsing **Recording** pill — tap to view the live recording screen   |

### Session lifecycle

```
RECORDING → STOPPED → (transcription completes) → (summary completes) → COMPLETED
```

The session is marked **COMPLETED** automatically after Gemini finishes summary generation. If summary fails, the session stays **STOPPED** and a Retry button appears.

---

## Project Structure

```
app/src/main/java/com/example/shyam_assignment/
├── TwinMindApp.kt                    # Application class (Hilt, notification channel, recovery)
├── MainActivity.kt                   # Single-activity Compose host
│
├── navigation/
│   ├── Screen.kt                     # Route definitions
│   └── NavGraph.kt                   # Navigation graph
│
├── di/
│   └── AppModule.kt                  # Hilt modules (DB, DAOs, repos, WorkManager)
│
├── data/
│   ├── api/
│   │   ├── GeminiApiService.kt       # Gemini REST client (transcription + summary)
│   │   └── GeminiModels.kt           # Request/response DTOs
│   ├── local/
│   │   ├── AppDatabase.kt            # Room database (4 entities)
│   │   ├── DatabaseSeeder.kt         # Sample seed data for first launch
│   │   ├── dao/
│   │   │   ├── RecordingSessionDao.kt
│   │   │   ├── AudioChunkDao.kt
│   │   │   ├── TranscriptSegmentDao.kt
│   │   │   └── SummaryDao.kt
│   │   └── entity/
│   │       ├── RecordingSessionEntity.kt
│   │       ├── AudioChunkEntity.kt
│   │       ├── TranscriptSegmentEntity.kt
│   │       └── SummaryEntity.kt
│   ├── model/
│   │   └── Meeting.kt
│   └── repository/
│       ├── RecordingRepository.kt
│       ├── TranscriptRepository.kt
│       ├── SummaryRepository.kt
│       └── impl/
│           ├── RecordingRepositoryImpl.kt
│           ├── TranscriptRepositoryImpl.kt
│           └── SummaryRepositoryImpl.kt
│
├── service/
│   ├── RecordingService.kt           # ForegroundService — chunked AudioRecord
│   ├── RecordingServiceState.kt      # Shared observable state (service ↔ ViewModel)
│   ├── EdgeCaseHandlers.kt           # Phone call, battery, BT, storage, silence
│   └── WavWriter.kt                  # PCM-to-WAV file writer
│
├── worker/
│   ├── ChunkTranscriptionWorker.kt   # WorkManager: audio chunk → Gemini → transcript
│   ├── SummaryWorker.kt              # WorkManager: transcript → Gemini → summary
│   └── RecoveryManager.kt            # Startup recovery for interrupted sessions
│
└── ui/
    ├── theme/
    │   ├── Color.kt                  # TwinMind color palette
    │   ├── Theme.kt                  # Light theme configuration
    │   └── Type.kt                   # Typography
    └── screens/
        ├── dashboard/
        │   ├── DashboardScreen.kt    # Meeting list, FAB, recording banner
        │   ├── DashboardUiState.kt
        │   └── DashboardViewModel.kt
        ├── recording/
        │   ├── RecordingScreen.kt    # Record button, timer, status, transcript preview
        │   ├── RecordingUiState.kt
        │   └── RecordingViewModel.kt
        └── summary/
            ├── SummaryScreen.kt      # Title, summary, action items, key points, transcript
            ├── SummaryUiState.kt
            └── SummaryViewModel.kt
```

---

## Features

### 1. Recording Pipeline

| Feature                     | Detail                                                                 |
|-----------------------------|------------------------------------------------------------------------|
| Audio capture               | `AudioRecord` (16 kHz, mono, 16-bit PCM)                              |
| Chunking                    | 30-second WAV chunks with ~2 s overlap between consecutive chunks      |
| Service                     | `ForegroundService` with persistent notification (Stop / Resume)       |
| Chunk finalization           | Last chunk saved synchronously (`runBlocking`) to prevent data loss    |
| File format                 | WAV (PCM wrapped with proper WAV header via `WavWriter`)               |

### 2. Edge-Case Handling

| Scenario                    | Behavior                                                               |
|-----------------------------|------------------------------------------------------------------------|
| **Phone call**              | Recording pauses automatically, resumes when call ends. Detected via AudioManager mode polling + TelephonyManager broadcast + TelephonyCallback (triple strategy, no runtime permission required) |
| **Audio focus loss**        | Notification shows Resume / Stop actions                               |
| **Bluetooth / headset**     | Hot-swap without breaking session; warning shown with new source name  |
| **Low storage** (< 50 MB)  | Checked before start and every 30 s; recording stops gracefully        |
| **Low battery** (< 3%)     | Recording stops with error message                                     |
| **Silence detection**       | After 10 s of silence (RMS-based), shows warning: *"No audio detected — Check microphone"*. Recording continues. Warning clears when sound resumes |

### 3. Transcription (Gemini 2.5 Flash)

- Each WAV chunk is base64-encoded and sent inline to Gemini
- Prompt: *"Transcribe this audio chunk accurately. Preserve the original spoken language. Do not summarize. Do not add explanations. Return only the transcript text."*
- WorkManager with exponential backoff (up to 3 retries)
- Unique work per chunk (`enqueueUniqueWork` with `KEEP` policy) — prevents duplicate API calls
- Early-exit if chunk is already transcribed
- Results persisted to Room as `TranscriptSegmentEntity`, ordered by `chunkIndex`

### 4. Summary Generation (Gemini 2.5 Flash)

- Full ordered transcript sent to Gemini with structured JSON prompt
- Output schema: `{ title, summary, actionItems, keyPoints }`
- Prompt enforces: no markdown, no extra keys, no commentary — pure JSON
- WorkManager with exponential backoff (up to 3 retries)
- Unique work per session (`enqueueUniqueWork` with `KEEP` policy)
- Retry uses `REPLACE` policy to force a fresh attempt
- Early-exit if summary is already completed
- On success: session status updated to **COMPLETED**
- Empty action items / key points: cards hidden gracefully on the summary screen

### 5. Recovery (App Kill / Crash)

- `RecoveryManager` runs on app startup (`Application.onCreate`)
- Detects sessions stuck in `RECORDING` / `PAUSED` → marks as `STOPPED`
- Re-enqueues pending chunk transcriptions (skips already completed)
- Re-enqueues pending summary generations (skips already completed)
- UI state fully restored from Room on relaunch

### 6. Dashboard

- Seeded with 3 sample meetings on first launch
- Session cards with colored left-accent bars (varies by status)
- Animated status chips (pulsing dot for active states)
- Active recording banner at the top when recording is in progress
- **"Capture"** pill FAB when idle; **Stop + Recording** pill FABs when recording
- Stop from dashboard FAB navigates directly to meeting details

### 7. Recording Screen

- Large animated record button with glow ring
- Live timer with gradient accent bar
- Status chip with pulsing recording indicator
- Chunk info card (active chunk number, total count)
- Live transcript preview (updates as chunks are transcribed)
- Warning / error banners
- Pause / Resume / Stop controls

### 8. Summary Screen

- Title card with session metadata
- Summary paragraph card
- Action Items card (numbered badges) — hidden if empty after completion
- Key Points card (gradient bullet dots) — hidden if empty after completion
- Full transcript card with per-chunk dividers
- Auto-triggers summary generation on first visit (guarded against duplicates)
- Loading, error, and retry states
- Persisted — survives app relaunch

---

## Theme — TwinMind-Inspired Light UI

| Token           | Hex       | Usage                    |
|-----------------|-----------|--------------------------|
| Background      | `#F7F7F3` | Warm off-white background|
| Surface         | `#FFFFFF` | White card surfaces      |
| Elevated Card   | `#EAF2F2` | Teal-tinted card fill    |
| Primary         | `#0D7377` | Deep teal accent         |
| Secondary       | `#E07C3A` | Warm orange accent       |
| Text Primary    | `#1B3A4B` | Dark navy text           |
| Text Secondary  | `#6B8299` | Muted blue-gray text     |
| Warning         | `#E07C3A` | Orange warning banners   |
| Error / Red     | `#D94444` | Error states, stop       |
| Recording Red   | `#D94444` | Recording indicators     |
| Card Border     | `#D0D8DD` | Light gray borders       |
| Gradient Start  | `#0D7377` | Teal gradient start      |
| Gradient End    | `#149DA0` | Lighter teal gradient end|

---

## Room Entities

| Entity                      | Primary Key   | Key Fields                                       |
|-----------------------------|---------------|--------------------------------------------------|
| `RecordingSessionEntity`    | `sessionId`   | title, startedAt, endedAt, durationMs, status     |
| `AudioChunkEntity`          | `chunkId`     | sessionId, chunkIndex, filePath, transcriptionState |
| `TranscriptSegmentEntity`   | `id`          | sessionId, chunkId, chunkIndex, text              |
| `SummaryEntity`             | `sessionId`   | title, summary, actionItemsJson, keyPointsJson, status |

---

## Permissions

| Permission                           | Purpose                                |
|--------------------------------------|----------------------------------------|
| `RECORD_AUDIO`                       | Microphone access                      |
| `FOREGROUND_SERVICE`                 | Background recording                   |
| `FOREGROUND_SERVICE_MICROPHONE`      | Mic-type foreground service (API 34+)  |
| `POST_NOTIFICATIONS`                | Recording notification (API 33+)       |
| `READ_PHONE_STATE`                  | Phone call detection (bonus strategy)  |
| `INTERNET`                           | Gemini API calls                       |

---

## API Key Safety

The Gemini API key is stored in `local.properties` (git-ignored) and injected via `BuildConfig.GEMINI_API_KEY`. It is **never committed to version control**.

Duplicate API calls are prevented by:
- `enqueueUniqueWork` with `KEEP` policy for both transcription and summary workers
- Early-exit checks inside workers if chunk/summary is already `COMPLETED`
- `generationTriggered` guard in `SummaryViewModel` to prevent re-triggers on recomposition

---

## Known Limitations

- Recording requires a physical device (emulator mic input is unreliable)
- Very long recordings (> 1 hour) may take time for all chunks to transcribe sequentially
- Gemini API rate limits may cause transient failures (handled by WorkManager retry)
- Audio files are stored in app-internal storage and cleared on app uninstall

---



