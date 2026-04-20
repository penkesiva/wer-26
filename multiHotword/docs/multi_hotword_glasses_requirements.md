# Multi-Hotword Detection for Smart Glasses — Requirements & Tech Feasibility

**Feature:** On-device multi-hotword detection micro model  
**Platform:** Smart glasses (always-on, low-power path)  
**Document type:** Product / engineering requirements + feasibility  
**Revision:** 0.1 · **Date:** 2026-04-19

---

## 1. Purpose

Enable smart glasses to detect **multiple wake words** on-device (for example, brand-specific assistant names or vendor-agent triggers) and hand off to the correct downstream assistant with low latency and low power.

This is separate from QR projects and focuses only on **keyword spotting (KWS)**.

---

## 2. Why this is needed (business context in LLM/agent era)

- Users increasingly interact with multiple AI assistants (phone OS agent, enterprise agent, app-specific agent).
- A single generic wake word limits partner integrations and routing flexibility.
- Multi-vendor ecosystems need a local gate that decides **which agent to wake** before cloud or heavy inference.
- On-device wake-word detection reduces privacy risk and battery drain versus continuous cloud audio streaming.
- Partner strategy: same hardware SKU can support different hotword sets by market, carrier, OEM, or app bundle.

---

## 3. Scope

### 3.1 In scope

- Always-on on-device detection of **N hotwords** (target N to be confirmed).
- Low-power audio front-end + micro model inference on MCU/DSP path.
- Trigger routing to downstream assistant/app based on detected hotword ID.
- False accept / false reject controls per hotword.
- OTA update mechanism for model and hotword configuration.

### 3.2 Out of scope (v1)

| Item | Rationale |
| --- | --- |
| Full ASR / speech recognition | KWS only; avoids compute and latency expansion |
| Natural-language intent parsing | Handled by downstream assistant after wake |
| Speaker verification / biometrics | Separate security feature track |
| Cloud-only wake-word detection as default | Violates always-on power/privacy goals |

---

## 4. Functional requirements

| ID | Requirement |
| --- | --- |
| **H-1** | System SHALL support at least **2 hotwords** in v1, with scalable architecture to N hotwords. |
| **H-2** | Detector SHALL output `{hotword_id, confidence, timestamp}` for each accepted trigger. |
| **H-3** | Hotword decision thresholds SHALL be configurable per hotword via signed config. |
| **H-4** | Detector SHALL expose cooldown/suppression window to prevent repeated triggers from the same utterance. |
| **H-5** | Trigger event SHALL route to the mapped assistant/vendor endpoint in local policy. |
| **H-6** | System SHALL support OTA model/version rollback on regression. |

---

## 5. Non-functional requirements (glasses constraints)

| ID | Requirement |
| --- | --- |
| **N-1** | End-to-end wake latency (speech end to trigger event) target: **<= 300 ms** p95. |
| **N-2** | Always-on power budget target: **TBD mW** on low-power audio path (must be measured per SKU). |
| **N-3** | Runtime memory footprint target (model + buffers + runtime): **TBD KB** with SKU-specific caps. |
| **N-4** | Model artifact size target for micro model: **TBD KB** (typical range 20-200 KB depending on N and robustness goals). |
| **N-5** | Must operate robustly under glasses noise profile: wind, walking vibration coupling, traffic, cafe, office, TV background, near-field speech. |

---

## 6. Target metrics and acceptance criteria

Use per-hotword and aggregate reporting:

| Metric | Definition | Target (initial) |
| --- | --- | --- |
| **False Reject Rate (FRR)** | Missed activations when true hotword spoken | <= 5% (quiet), <= 10% (noisy) |
| **False Accept Rate (FAR)** | Spurious triggers per hour from non-hotword audio | <= 0.1/hour/hotword (quiet), <= 0.3/hour/hotword (real-world mix) |
| **Cross-trigger confusion** | Triggering wrong hotword among supported set | <= 1% |
| **Wake latency** | End of hotword to trigger event | <= 300 ms p95 |
| **Power delta** | Always-on audio + KWS minus audio baseline | TBD mW |

Targets are placeholders until hardware profiling and product tolerance are confirmed.

---

## 7. System architecture options

### Option A: Open-source baseline (recommended for fast start)

- Audio front-end: log-mel / MFCC features on-device.
- Micro model: compact CNN/CRNN/DS-CNN classifier for hotword classes + background class.
- Runtime: TFLite Micro, CMSIS-NN, or vendor DSP/NPU kernels where available.
- Pros: fast prototyping, transparent, no licensing lock-in.
- Cons: requires internal data collection and tuning to match production robustness.

### Option B: Commercial wake-word SDK

- Use a vendor KWS stack with pre-optimized runtime and tuning tools.
- Pros: faster path to quality and certification in many cases.
- Cons: recurring licensing cost, partner lock-in, limited model transparency.

### Option C: Hybrid

- Start with open-source baseline for architecture ownership and bring commercial fallback for schedule risk.

---

## 8. Open-source technical feasibility

Feasible on modern glasses MCU/DSP/low-power cores if pipeline is designed for streaming inference:

- Sliding-window inference (for example 20-40 ms frames with 0.5-1.0 s context).
- Quantized int8 micro model.
- Fixed-size ring buffers, no unbounded allocations.
- Optional DSP acceleration for feature extraction and conv kernels.

Candidate open-source components:

- **TensorFlow Lite Micro** for runtime.
- **CMSIS-DSP / CMSIS-NN** for feature + kernel acceleration on Arm Cortex-M.
- Public KWS reference models (DS-CNN style) as starting points.

Key risk is usually **dataset mismatch**, not raw compute feasibility.

---

## 9. Custom solution feasibility and what is needed

Custom multi-hotword model is feasible, but quality depends on dataset and labeling discipline.

### 9.1 Minimum dataset needs (v1 guideline)

Per hotword:

- **Positive utterances:** at least 3k-10k clips/hotword from diverse speakers.
- **Speaker diversity:** age, accent, gender, region, speaking rate.
- **Channel diversity:** glasses mic(s), phone replay, far/near distance.
- **Noise conditions:** indoor/outdoor, transport, crowd, TV/music, wind.
- **Hard negatives:** phonetically similar words and frequent conversational phrases.

Global:

- Large background/negative corpus (hours scale) to control FAR.
- Device-specific recordings from actual glasses microphones are mandatory.
- Train/val/test splits by speaker/session/environment with strict leakage control.

### 9.2 Tooling and pipeline requirements

- Data collection app/tooling with consent and metadata.
- Labeling pipeline with QA (spot checks, confusion mining).
- Training + quantization + on-target benchmarking pipeline.
- Continuous evaluation dashboard (FAR/FRR/latency/power by condition).
- OTA rollout/canary + rollback policy.

---

## 10. Integration requirements (multi-vendor routing)

| ID | Requirement |
| --- | --- |
| **I-1** | Each hotword SHALL map to a local `assistant_id` routing table entry. |
| **I-2** | Routing config SHALL be updateable OTA without model retraining where possible. |
| **I-3** | If multiple assistants are disabled by policy/region, detector SHALL honor dynamic allow-list. |
| **I-4** | Trigger event schema SHALL be stable across vendors (`hotword_id`, confidence, locale, device_state). |
| **I-5** | Privacy policy SHALL define retained telemetry fields (no raw always-on audio upload by default). |

---

## 11. Open decisions

| # | Decision | Owner |
| --- | --- | --- |
| **D-1** | How many hotwords in v1 and v2 (N=2,3,5...)? | Product |
| **D-2** | Open-source only vs commercial SDK vs hybrid? | Product + Eng |
| **D-3** | Required locales/languages at launch? | Product |
| **D-4** | FAR/FRR operating point priority by market segment? | Product + UX |
| **D-5** | On-device only policy vs optional cloud fallback trigger confirmation? | Privacy + Product |
| **D-6** | OTA cadence and rollback thresholds for wake model updates? | Platform |

---

## 12. Immediate next steps

1. Lock **D-1** (hotword count) and **D-3** (locales) to size dataset and model.
2. Run a 2-4 week feasibility spike:
   - open-source DS-CNN baseline,
   - on-target latency/power profiling,
   - initial FAR/FRR on small in-house dataset.
3. Decide build-vs-buy at the end of spike using objective scorecard (quality, power, schedule, cost, lock-in).
4. Freeze v1 acceptance metrics and convert this document to revision 1.0.

---

## 13. Glossary

| Term | Meaning |
| --- | --- |
| **KWS** | Keyword spotting (wake-word detection). |
| **FAR** | False accepts per hour. |
| **FRR** | False reject rate (missed true wake words). |
| **DS-CNN** | Depthwise separable CNN, common low-power KWS architecture. |
| **Always-on path** | Low-power audio pipeline that runs continuously. |

