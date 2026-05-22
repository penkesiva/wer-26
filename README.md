# GolfCues — Android MVP

GolfCues detects probable golf shots using on-device microphone audio and motion context during an active **Golf Mode** session.

## Features

- Start/stop Golf Mode with a foreground listening service
- Continuous audio processing via `AudioRecord` (16 kHz mono PCM)
- Motion gating (idle/near-idle) to reduce false positives
- Live timeline with confidence and motion state
- User feedback: Correct, Not a shot, Missed shot
- Session history stored locally with Room
- Configurable sensitivity, cooldown, and motion gating

## Stack

- Kotlin + Jetpack Compose + MVVM
- Room (local persistence)
- ForegroundService (microphone type)
- TensorFlow Lite (with `FakeAudioClassifier` fallback until model is added)
- SensorManager (accelerometer, gyroscope, step detector)

## Requirements

- Android SDK 26+
- Android Studio Ladybug or newer (recommended)
- JDK 17

## Build & Run

1. Open `/Users/sivapenke/Projects/GolfCues` in Android Studio
2. Sync Gradle
3. Run on a physical device (microphone + motion sensors required)

Or from the command line:

```bash
cd /Users/sivapenke/Projects/GolfCues
./gradlew assembleDebug
```

Install the APK:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

- `RECORD_AUDIO` — detect impact sounds during Golf Mode
- `POST_NOTIFICATIONS` — persistent Golf Mode notification (Android 13+)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MICROPHONE` — background listening
- `ACTIVITY_RECOGNITION` — motion context (optional enhancement)

## TFLite Model

Add a trained model at:

`app/src/main/assets/golf_audio_classifier.tflite`

Without it, the app uses `FakeAudioClassifier`, which simulates detections from audio energy spikes (useful for UI and pipeline testing).

## Project Structure

```
com.golfcues.app
├── data/          Room entities, DAOs, repositories
├── domain/        Audio, motion, detection logic
├── ml/            Feature extraction + TFLite classifier
├── service/       Foreground service + notifications
└── ui/            Compose screens + ViewModels
```

## MVP Flow

1. Tap **Start Golf Mode** on the home screen
2. Grant microphone (and notification) permissions
3. Foreground service begins listening
4. Probable shots appear in the live timeline when audio confidence + motion gate pass
5. Tap **Stop Golf Mode** to end and save the session
6. Review past sessions in **Session History**

## Privacy

Audio is processed on-device. Raw audio is not stored unless **Save audio snippets** is enabled in Settings.
