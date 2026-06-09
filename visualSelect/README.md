# Visual Select (Android)

Prototype Android app for **Idea 1** — live camera preview, detect **two hands** with **Google MediaPipe Hand Landmarker** (ML Kit / Google AI Edge vision stack), compute the **region between hands**, and **save a PNG crop to the gallery**.

## Branch

`visual-select-android`

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Physical device or emulator with **camera** (physical device recommended)
- `local.properties` with `sdk.dir=...` (Android Studio creates this on import)

## Open in Android Studio

1. **File → Open** → select the `visualSelect/` folder (not the repo root).
2. Let Gradle sync.
3. Run the **app** configuration on a device.

## How it works

1. **CameraX** preview + `ImageAnalysis` (RGBA frames).
2. **Hand Landmarker** (`com.google.mediapipe:tasks-vision`) — up to 2 hands, 21 landmarks each.
3. **BetweenHandsCropper** — bounding boxes from landmarks; crop rectangle spans **inner edges** between left and right hand (with padding).
4. Yellow overlay = crop region; blue = hand boxes.
5. **Save crop** writes `Pictures/VisualSelect/visual_select_<timestamp>.png` via **MediaStore** (no legacy storage permission on API 29+).
6. **Settings** (top-right): autofocus, wide-angle lens, zoom / FOV, chime, crop padding.

## Settings defaults

| Option | Default |
| --- | --- |
| Autofocus | **Off** (fixed focus for stable framing) |
| Wide-angle lens | **On** (widest back camera when available) |
| Zoom | **0.5** (widest FOV on device) |
| Chime on two hands | **On** (once per entry into two-hand state) |
| Crop padding | 24 px |

## Assumptions (confirm if you want changes)

| Topic | Current behavior |
| --- | --- |
| **Hands** | Exactly **2 hands** required to enable Save |
| **Crop** | Axis-aligned box **between** hand bounding boxes (not full union) |
| **Trigger** | Manual **Save crop** button (no auto-save on stability yet) |
| **Camera** | Back camera only |
| **ML stack** | MediaPipe Hand Landmarker (Google’s current hand API; same family as ML Kit vision tasks) |

## Model

Bundled at `app/src/main/assets/hand_landmarker.task` (~7.5 MB). Re-download:

```bash
curl -L -o app/src/main/assets/hand_landmarker.task \
  https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task
```

## Project layout

```
visualSelect/
  app/src/main/java/com/visualselect/app/
    MainActivity.kt          # CameraX + Compose UI
    HandLandmarkerHelper.kt  # MediaPipe live stream
    BetweenHandsCropper.kt   # Region-between-hands math
    GallerySaver.kt          # MediaStore PNG save
```

## Related docs

- Idea 1 architecture: `../tmp/idea1.md`
