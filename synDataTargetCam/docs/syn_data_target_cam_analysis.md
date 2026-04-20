# Synthetic Data for Target Camera — Analysis & Requirements Skeleton

**Project:** synDataTargetCam  
**Goal:** Synthetic data generation pipeline that converts open-source phone video into training data representative of a **glasses camera** input.  
**Document type:** Brainstorm / analysis — requirements-to-be-confirmed  
**Revision:** 0.1 · **Date:** 2026-04-19

---

## 0. Caveat — ranking is task-conditional

The parameter-impact ranking below assumes:

- **Model input:** post-ISP RGB (RAW branch documented separately; see §6)
- **Spatial input resolution:** 320 × 320 or 640 × 640 (on-device budget)
- **Task family:** detection / fine-structure recognition (small objects, fine texture, geometry — e.g. QR-like, barcodes, labels, hands, small UI elements)

For **scene classification** or **global tagging** tasks, this ranking compresses significantly — most optical knobs matter less. Confirm the primary task before finalising tier assignments.

---

## 1. Camera parameter impact — tiered ranking

### Tier A — High impact · must simulate for any honest ablation

| # | Parameter | Why it matters | Literature anchor |
|---|---|---|---|
| 1 | **Effective angular resolution / ground sample density (GSD)** | FoV + resolution combined. A wider-FoV glasses camera at 640 × 640 has fewer pixels per degree than a narrow-FoV phone at 1080p. For small-object / fine-texture tasks this is typically the single largest domain gap. | COCO small-object tier (Lin et al.); super-resolution / resize-robustness surveys |
| 2 | **Field of view (FoV) + radial distortion** | Wider FoV → more radial distortion, different edge statistics, different scene composition. Cross-FoV gap is consistently among the top-2 factors in FishEye / omnidirectional detection literature. | FishEye detection benchmarks; Pascal/COCO cropping studies |
| 3 | **Motion blur + rolling shutter** | Head-mounted cameras see far greater angular velocity (yaw/pitch head turns) than a handheld phone. Rolling-shutter skew is real and frequent. Blur-unaware training consistently degrades detection on egocentric inputs. | BAD-NeRF; motion-aware detection; EventVis blur studies |
| 4 | **Intrinsics — focal length & principal point** | Glasses cameras are often asymmetrically placed on the frame; principal point is offset. Models that absorbed an implicit focal prior from phone training misplace objects in geometry-heavy tasks (QR corners, plane estimation). | Calibration-aware detection; cross-camera adaptation papers |

### Tier B — Medium impact · simulate if budget permits · include in ablation

| # | Parameter | Why it matters |
|---|---|---|
| 5 | **Focus model (fixed-focus / EDOF on glasses vs autofocus on phone)** | Glasses ship fixed-focus; near-field objects (hands, handheld product) can be genuinely defocused. Phone footage is almost always sharp. Impact scales with near-field scene frequency. |
| 6 | **ISP tone / color pipeline** | Tone curve, local contrast, sharpening, denoise, WB, saturation — a glasses ISP tuned for power and latency looks quite different from a heavy computational-photography phone ISP. CycleISP / invertible ISP papers show real but recoverable gaps. |
| 7 | **Exposure / HDR behaviour** | Phone HDR compresses highlights aggressively; glasses often do not. Scenes with glare, windows, or screens (glassy labels, screen QR, sunlit shelves) look very different. |
| 8 | **Extrinsics / mount geometry** | Glasses cameras are not at the eye — typically offset by a few cm, often angled slightly downward. Scene composition differs from a hand-held phone (hands dominate near field; subject is typically centred-low). Impacts egocentric task layouts. |

### Tier C — Smaller / task-conditional impact

| # | Parameter | Notes |
|---|---|---|
| 9 | **Sensor noise model** (shot + read noise) | Matters mainly in low light; more important on the RAW branch. |
| 10 | **Vignetting / lens shading** | Usually ISP-corrected; residual impact on edge-of-frame detections. |
| 11 | **Chromatic aberration** | Can matter for thin structures (text, barcodes) near corners. |
| 12 | **Compression artefacts** | Internet video is H.264/H.265 variable bitrate; adds blocking, hides noise. Mostly a nuisance. |
| 13 | **Bit depth / colour space** | Most relevant on RAW branch; post-ISP 8-bit sRGB largely hides it. |
| 14 | **Frame rate / exposure-time coupling** | Only relevant for temporal pipelines (tracking, stabilisation). |

---

## 2. Source video strategy — two tracks

### 2.1 Internet phone video (arbitrary, open-source)

| Aspect | Details |
|---|---|
| **Purpose** | Scene and subject diversity; long-tail content; negative examples |
| **Intrinsics known?** | Mostly no — estimate from EXIF where present; assume class-average phone profile otherwise |
| **ISP known?** | Mixed devices and apps; variable compression |
| **Main strength** | Very large scale; wide range of lighting, backgrounds, subjects |
| **Main drawback** | Weak camera-side labels; estimated intrinsics add warp noise |

### 2.2 Reference phone capture — e.g. Samsung S25+ (controlled, to-be-collected)

| Aspect | Details |
|---|---|
| **Purpose** | Controlled ablations; reference for warp-to-glasses; ISP-matching anchor |
| **Intrinsics known?** | Yes — from EXIF + one-time checkerboard calibration session |
| **ISP known?** | Yes — fixed, repeatable |
| **Main strength** | Trustworthy warp targets; enables per-parameter isolation |
| **Main drawback** | Labor cost to collect; narrow scene distribution |

> **Recommended blend for v1:** Majority of training volume from internet video (scene statistics). Smaller reference-phone slice for ISP-matching anchors and validated warp-to-glasses transforms. Real glasses eval set as the only acceptance gate.

---

## 3. Ablation plan — per-parameter sensitivity study

Run each step against the **real glasses eval set** (available). Report absolute task metric and delta per added factor.

| Step | Transform applied (cumulative) | Expected dominant effect |
|---|---|---|
| 1 | **Baseline** — phone source, no transform | Measure raw domain gap |
| 2 | + **GSD match** (resize / crop to glasses GSD) | Tier A-1 |
| 3 | + **FoV + intrinsics match** (pinhole re-projection) | Tier A-2, A-4 |
| 4 | + **Motion blur + rolling-shutter simulation** | Tier A-3 |
| 5 | + **Focus model** (EDOF / fixed-focus kernel) | Tier B-5 |
| 6 | + **ISP match** (3D LUT or learned mapping from reference phone → glasses) | Tier B-6 |
| 7 | + **Exposure / HDR** behaviour | Tier B-7 |
| 8 | + **Noise model** (shot + read calibrated to glasses sensor) | Tier C-9 |
| 9 | **All combined** | Full simulation |

**Stretch:** knock-out ablation (All minus one factor) to identify which individual factors hurt most when absent.

---

## 4. Open questions / requirements to confirm

| # | Question | Needed to finalise |
|---|---|---|
| **Q-1** | **Primary task(s):** What is the ML model being trained to do? (Detection, QR, hands, object class list…) | Tier assignments may re-order |
| **Q-2** | **Glasses camera profile:** Approximate FoV, resolution (sensor and inference input), shutter type (rolling / global), focus type, ISP / post-processing level | Defines warp targets for §§1–3 |
| **Q-3** | **RAW vs post-ISP branch:** Is glasses RAW available and practical in the training pipeline, or is post-ISP RGB the only path? | Affects Tier C weighting; noise model depth |
| **Q-4** | **Label strategy for internet video:** Bootstrap labels via a large teacher model? Manual annotation? Weak supervision? | Determines usable video fraction and annotation cost |
| **Q-5** | **Reference-phone capture budget:** How many hours of S25+ footage, how many distinct scenes / conditions? | Determines ISP-matching anchor quality |
| **Q-6** | **Real glasses eval set details:** How many scenes, what conditions, how labelled? | Must be confirmed before ablation metrics are meaningful |
| **Q-7** | **Inference resolution contract:** Is 320 × 320 or 640 × 640 locked, or is it TBD by model/budget trade-off? | Anchors GSD calculations |

---

## 5. Next steps

1. **Confirm Q-1 and Q-2** to finalise tier ranking and define simulation targets.
2. **Collect glasses camera spec sheet** (even partial — FoV and resolution are the minimum).
3. **Draft simulation recipe** (Blender / OpenCV pipeline) based on confirmed intrinsics and ISP profile.
4. **Plan reference-phone capture session** (S25+ checkerboard + representative scenes).
5. **Run ablation study** (§3) on a seed dataset once simulation pipeline is functional.
6. **Update this doc** to revision 1.0 with confirmed requirements after Q-1 through Q-7 are resolved.

---

## 6. Glossary

| Term | Meaning |
|---|---|
| **GSD** | Ground sample density — angular size of one pixel on the sensor; combines FoV and resolution |
| **EDOF** | Extended Depth of Field — fixed-focus optical design that maintains acceptable sharpness over a range without autofocus |
| **ISP** | Image Signal Processor — on-chip pipeline that converts raw sensor data to post-processed RGB |
| **Rolling shutter** | Sensor readout mode that exposes rows sequentially; causes skew artefacts under motion |
| **Ablation study** | Controlled experiment where one factor is added/removed at a time to isolate its contribution |
| **3D LUT** | 3-dimensional look-up table used to map colour from one ISP / camera profile to another |
| **Post-ISP RGB** | Image after the glasses ISP pipeline has applied demosaicing, tone mapping, noise reduction, etc. |
| **RAW** | Unprocessed or minimally processed sensor data (Bayer pattern); preserves full radiometric information |

---

## References

- Lin et al., "Microsoft COCO: Common Objects in Context" — small-object detection tiers  
- Deng et al., "CycleISP: Real Image Restoration via Improved Data Synthesis" — cross-ISP generalisation  
- Invertible ISP / RAW-RGB round-trip papers — ISP matching methodology  
- FishEye / wide-angle detection benchmarks — FoV generalisation  
- BAD-NeRF and motion-aware detection works — blur / rolling-shutter robustness
