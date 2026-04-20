# QR Code Pairing — Requirements

**Feature:** QR-initiated BLE device pairing  
**Devices:** Smart glasses (camera) ↔ Companion mobile app  
**Document type:** Product / engineering requirements  
**Revision:** 0.1 · **Date:** 2026-04-19

---

## 1. Purpose

Enable a **frictionless, first-time BLE pairing** experience between smart glasses and a companion mobile app by having the **glasses camera scan a QR code** displayed on the **companion app screen**. The QR code carries enough information for the glasses to identify and connect to the correct peer device without requiring the user to manually select from a BLE scan list or enter a PIN.

---

## 2. Pairing flow

```mermaid
sequenceDiagram
  participant G as Glasses
  participant C as Companion App (Phone)

  C->>C: Generate & display QR code\n(encodes BLE MAC + pairing token)
  G->>G: Glasses camera scans QR from phone screen
  G->>G: Decode MAC address (+ optional token)
  G->>C: Initiate BLE connection to decoded MAC
  C->>G: BLE pairing handshake (OS-level)
  G->>C: Pairing confirmed
  C->>C: Persist bonding; proceed to onboarding
```

---

## 3. Scope

### 3.1 In scope

- **QR generation** in the companion app (iOS and/or Android) encoding the pairing payload.
- **QR scanning** via the **glasses on-device camera** and software/ML decode pipeline.
- **BLE connection initiation** from glasses to companion app using the decoded MAC address.
- **Pairing success / failure UX** on both glasses and companion app.
- **Security baseline** appropriate for a medium-threat consumer pairing flow (see §7).

### 3.2 Out of scope (explicit)

| Item | Rationale |
|---|---|
| PIN / passkey entry via glasses | Requires input HID; not in this flow |
| QR scanning by the phone camera | Role is reversed in this design; out of scope for v1 |
| Wi-Fi / network credential delivery via QR | Separate feature track |
| Full BLE stack implementation | Handled by OS BLE APIs; only the pairing trigger is in scope |
| Cryptographic key exchange beyond BLE OOB | Out of scope for medium-security tier; noted as stretch goal |

---

## 4. QR code payload specification

> **Status: to be confirmed** — the exact payload schema must be agreed between glasses firmware and companion app teams before implementation.

### 4.1 Minimum viable payload (v1)

| Field | Type | Notes |
|---|---|---|
| `ble_mac` | string · 17 chars · `XX:XX:XX:XX:XX:XX` | BLE MAC of the **companion app's host device** (the phone) |
| `token` | string · 8–16 hex chars (optional) | Short-lived random token for replay protection; expires after `T` seconds (see §7) |

**Encoded as:** a plain URI string or compact JSON, kept under **~100 characters** to stay within QR Version 3–4 (low module density → easy scanning from a phone screen at arm's length).

Example URI form:
```
blep://pair?mac=AA:BB:CC:DD:EE:FF&tok=3f9a12c0
```

### 4.2 QR code parameters

| Parameter | Value |
|---|---|
| Symbology | QR Code Model 2 (ISO/IEC 18004) |
| Error correction | **M** (15%) — balances density and screen-scan robustness |
| Version | V3–V5 (target; determined by payload length) |
| Minimum display size | **200 × 200 px** at native screen resolution |
| Quiet zone | 4 modules minimum |
| Foreground / background | Black modules on white — no inverse or coloured QR in v1 |

---

## 5. Glasses-side requirements

| ID | Requirement |
|---|---|
| **P-1** | Glasses SHALL scan and decode the pairing QR in **≤ 3 seconds** from the moment the code is fully in view under normal indoor lighting. |
| **P-2** | Glasses SHALL correctly decode the QR when the phone screen is held at **30–60 cm** from the glasses camera (typical arm-to-face distance). |
| **P-3** | Glasses SHALL handle **screen glare and moiré** artefacts inherent in scanning an LCD/OLED display. |
| **P-4** | After successful decode, glasses SHALL initiate a BLE connection to the decoded MAC within **500 ms**. |
| **P-5** | On decode failure (timeout or repeated error), glasses SHALL surface a **user-visible error** and prompt a retry. |
| **P-6** | The pairing QR scanner SHALL operate **independently** of any other ML inference pipeline (e.g. the TFLM geometry model) and SHALL NOT require that model to be loaded. |

---

## 6. Companion app requirements

| ID | Requirement |
|---|---|
| **A-1** | The companion app SHALL generate and display the pairing QR on the **first-launch / onboarding screen** before BLE is active. |
| **A-2** | The QR SHALL be **regenerated with a fresh token** each time the pairing screen is displayed (not cached from a previous session). |
| **A-3** | The app SHALL display a **countdown or expiry indicator** if a token TTL is used (see §7). |
| **A-4** | The app SHALL transition automatically to the BLE pairing OS prompt upon receiving an incoming connection from the decoded MAC. |
| **A-5** | The app SHALL handle the case where the decoded MAC arrives **after token expiry** and prompt the user to refresh the QR. |
| **A-6** | The app SHALL support both **iOS** and **Android** companion platforms. Exact OS version minima to be confirmed. |

---

## 7. Security requirements (medium-threat baseline)

| ID | Requirement |
|---|---|
| **S-1** | The QR payload SHALL include a **short-lived token** (recommended TTL: **60–120 seconds**) to prevent replay of a captured QR image. |
| **S-2** | The token SHALL be **cryptographically random** (≥ 64 bits of entropy; e.g. `SecureRandom` / `CryptoKit`). |
| **S-3** | The companion app SHALL reject any BLE connection attempt that arrives after the token TTL has expired and require a new QR to be displayed. |
| **S-4** | The pairing QR SHALL NOT encode persistent credentials, passwords, or long-lived session keys. |
| **S-5** | Post-pairing BLE bonding SHALL rely on **OS-level authenticated pairing** (e.g. Bluetooth LE Secure Connections with MITM protection where supported by both devices). |

> **Stretch goal:** BLE Out-of-Band (OOB) pairing using the QR payload as the OOB data blob for stronger MITM protection. This would replace the simple MAC + token scheme but requires firmware BLE stack support — confirm feasibility per SKU.

---

## 8. UX requirements

| ID | Requirement |
|---|---|
| **U-1** | The pairing QR screen on the companion app SHALL include a **plain-language instruction** (e.g. "Hold your glasses up and look at this code"). |
| **U-2** | On successful pairing, both glasses and companion app SHALL show a **confirmation** (visual and/or haptic) within **1 second** of BLE bonding completion. |
| **U-3** | The full pairing flow (QR display → scan → BLE connect → confirmed) SHALL complete in **≤ 15 seconds** under normal conditions. |
| **U-4** | A **"Can't scan?"** fallback path SHALL be available (e.g. manual device selection from a BLE scan list or a support deep-link). |

---

## 9. Open decisions

| # | Question | Needed to finalise |
|---|---|---|
| **Q-1** | **Payload owner:** Does the QR encode the **phone's** BLE MAC (phone advertises, glasses connect) or the **glasses'** MAC (glasses advertise, phone initiates after scan)? Each has different BLE role implications. | Firmware + app teams |
| **Q-2** | **Token TTL:** 60 s vs 120 s vs user-refreshable with no auto-expiry? | Product / security decision |
| **Q-3** | **OOB pairing stretch goal:** Is BLE OOB feasible on the glasses BLE stack and target OS versions? | BLE firmware team |
| **Q-4** | **Fallback UX:** Is the manual BLE scan fallback (U-4) in scope for v1 or a v2 item? | Product |
| **Q-5** | **OS version minima:** What iOS / Android minimum versions must the companion app support? Impacts `CoreBluetooth` / `BluetoothGatt` API choices. | Platform / product |
| **Q-6** | **Multi-device:** Should pairing support connecting glasses to more than one phone (e.g. work + personal), and if so how is the bonding list managed? | Product |

---

## 10. KPI table

| KPI | Definition | Target | Status |
|---|---|---|---|
| **QR decode time** | Time from QR fully in frame → successful decode on glasses | ≤ 3 s (P-1) | TBD |
| **BLE connect time** | Decode complete → BLE connection established | ≤ 2 s | TBD |
| **E2E pairing time** | QR displayed → bonding confirmed | ≤ 15 s (U-3) | TBD |
| **First-attempt success rate** | % of pairing attempts that succeed without retry (lab golden suite) | TBD % | TBD |
| **Token replay rejection rate** | % of expired-token connections correctly rejected | 100% | TBD |

---

## 11. Glossary

| Term | Meaning |
|---|---|
| **BLE** | Bluetooth Low Energy |
| **MAC** | Media Access Control address — hardware identifier for a BLE device |
| **OOB** | Out-of-Band pairing — BLE pairing method where key exchange happens over a channel other than BLE (here: QR code) |
| **TTL** | Time to live — duration after which a token or QR is considered expired |
| **Bonding** | Persistent BLE pairing state stored on both devices so reconnection does not require re-pairing |
| **MITM** | Man-in-the-middle — attack where a third party intercepts the pairing handshake |

---

## References

- Bluetooth Core Specification 5.x — LE Secure Connections, OOB pairing
- ISO/IEC 18004 — QR Code standard
- Apple CoreBluetooth documentation
- Android BluetoothGatt / BluetoothLeScanner API documentation
