# Functional Decline Radar: Developer Integration & System Guide
### Android Client (Kotlin / Compose) & Backend / Pipeline Architecture

This document serves as the complete technical integration guide for **Frontend (UI/UX)** and **Backend (Sensor Service, Room DB, ML Pipeline)** developers implementing the **Functional Decline Radar** on Android.

---

## 1. System Architecture & End-to-End Pipeline

The system is designed as an **offline-first, parallel architecture** running entirely on the Android device to protect user privacy and prevent thermal throttling.

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Android SensorManager (50 Hz)                     │
│               Accelerometer (X, Y, Z) + Gyroscope (X, Y, Z)            │
│                       Pocket Carry — 30 Seconds                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      Data Quality Gatekeeper                           │
│     Validates walking duration (>=10s), orientation (pocket <=35°),     │
│        signal quality index (>=0.5), stationary intervals (<=2)        │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
             [Session Invalid]                [Session Valid]
                    │                                │
                    ▼                                ├───────────────────────────────┐
          Discard Session                            ▼                               ▼
       (Zero ML computation)         ┌───────────────────────────────┐ ┌───────────────────────────┐
                                     │    Deterministic Heuristics   │ │    1D TCN TFLite Model     │
                                     │  - Cadence (steps/min)        │ │  - Input: [1, 1500, 6]    │
                                     │  - Step Symmetry Index        │ │  - INT8 Quantized (25KB)  │
                                     │  - Estimated Speed (m/s)      │ │  - Output: P(irregular)   │
                                     └──────────────┬────────────────┘ └─────────────┬─────────────┘
                                                    │                                │
                                                    │                                ▼
                                                    │                  ┌───────────────────────────┐
                                                    │                  │     Score Translation     │
                                                    │                  │   round((1 - P) * 100)    │
                                                    │                  └─────────────┬─────────────┘
                                                    │                                │
                                                    └────────────────┬───────────────┘
                                                                     │
                                                                     ▼
                                                    ┌──────────────────────────────────────────────┐
                                                    │         Longitudinal Deviation Engine        │
                                                    │  - Baseline calibration (sessions 1..4)      │
                                                    │  - Severe drop check: score < (mu - 2*sigma) │
                                                    │  - Persistence counter: 3 consecutive drops  │
                                                    └────────────────┬─────────────────────────────┘
                                                                     │
                                                                     ▼
                                                    ┌──────────────────────────────────────────────┐
                                                    │             Room DB Persistence              │
                                                    │  Saves complete enriched session entity      │
                                                    └────────────────┬─────────────────────────────┘
                                                                     │
                                                                     ▼
                                                    ┌──────────────────────────────────────────────┐
                                                    │                 Android UI                   │
                                                    │   🟢 Stable  |  🟡 Change  |  🔴 Persistent   │
                                                    └──────────────────────────────────────────────┘
```

---

## 2. Backend & Sensor Ingestion Layer

### 2.1 Sensor Requirements
The Android background tracking service collects from two hardware sensors via `SensorManager`:
- **Accelerometer** (`Sensor.TYPE_ACCELEROMETER`): 3 axes ($x, y, z$) in $\text{m/s}^2$ (includes gravity $\approx 9.8$).
- **Gyroscope** (`Sensor.TYPE_GYROSCOPE`): 3 axes ($x, y, z$) in $\text{rad/s}$.

### 2.2 Sampling Specifications
- **Frequency:** $50\text{ Hz}$ ($20,000\ \mu\text{s}$ period via `SensorManager.SENSOR_DELAY_GAME`).
- **Session Duration:** $30\text{ seconds}$ of walking.
- **Fixed Window Size:** $1,500\text{ frames}$ ($30\text{ s} \times 50\text{ Hz}$).
- **Buffer Shape:** `Float[1500][6]`, ordered as:
  `[accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z]`

---

## 3. Data Quality Gatekeeper Contract

The Gatekeeper acts as a strict guardrail before ML execution. If a session fails validation, **no ML inference is run**, saving battery and avoiding corrupted baselines.

### 3.1 Quality Criteria
| Check | Threshold | Reason for Rejection |
| :--- | :--- | :--- |
| **Duration** | $< 10.0\text{ seconds}$ | `walking_duration_too_short` |
| **Orientation** | Mean gravity angle $> 35.0^\circ$ from vertical $Z$ | `orientation_not_pocket_like` (bag, desk, handheld) |
| **Stillness** | $> 2$ stationary windows ($1.0\text{s}$ with std $< 0.15$) | `too_many_stationary_intervals` |
| **Signal Quality** | Signal Quality Index $< 0.5$ | `signal_quality_too_low` |

### 3.2 Gatekeeper Output Schema
```json
{
  "is_valid_session": true,
  "walking_duration_seconds": 30.0,
  "signal_quality_index": 0.96,
  "stationary_intervals": 0,
  "orientation_angle_degrees": 12.4,
  "rejection_reasons": []
}
```

---

## 4. TFLite Model Execution & Score Translation

### 4.1 Model Specifications
- **File:** `gait_tcn_quantized.tflite`
- **Size:** **$25.2\text{ KB}$** (INT8 post-training quantization).
- **Delegate:** Android NNAPI delegate or XNNPACK.
- **Input Tensor:** `Float32[1, 1500, 6]` (Shape: `[Batch, Time, Features]`).
- **Output Tensor:** `Float32[1, 1]`.

### 4.2 Channel Normalization (Z-Score)
Before passing the `[1500, 6]` buffer to the TFLite model, each channel must be normalized:
$$x_{\text{norm}}[t, c] = \frac{x[t, c] - \mu_c}{\sigma_c}$$

The exact parameters (from `normalization_stats.npz`) are:
| Channel Index | Axis | Mean ($\mu$) | Standard Deviation ($\sigma$) |
| :---: | :---: | :---: | :---: |
| 0 | `accel_x` | `1.723782` | `3.841080` |
| 1 | `accel_y` | `-0.319766` | `1.082653` |
| 2 | `accel_z` | `8.714895` | `4.420521` |
| 3 | `gyro_x` | `0.000155` | `0.577367` |
| 4 | `gyro_y` | `-0.000272` | `0.585530` |
| 5 | `gyro_z` | `-0.000454` | `0.555821` |

### 4.3 Score Translation Layer
The model outputs raw scalar $p_{\text{irregular}} \in [0.0, 1.0]$. The application translates this to:
$$\text{mobility\_stability\_score} = \text{round}((1.0 - p_{\text{irregular}}) \times 100)$$

| TCN Probability Output ($p_{\text{irregular}}$) | Mobility Stability Score | Clinical Meaning |
| :---: | :---: | :--- |
| `0.00` | **100** | Highly stable, regular walking dynamics |
| `0.10` | **90** | Normal steady-state gait |
| `0.50` | **50** | Ambiguous / moderate irregularity |
| `1.00` | **0** | Severe temporal disruption (freezing, halting) |

---

## 5. Deterministic Heuristics Module

Computed in parallel with the TCN from the raw accelerometer data (primarily vertical $Z$-axis):
- **`cadence_steps_per_min`**: Total steps taken per minute across both feet (normal range: 90–120 spm).
- **`symmetry_index`**: Ratio between alternating step peak amplitudes ($\min(\mu_{\text{even}}, \mu_{\text{odd}}) / \max(\mu_{\text{even}}, \mu_{\text{odd}})$). $1.0$ is perfect symmetry; $< 0.85$ indicates noticeable asymmetry.
- **`estimated_speed_m_s`**: Forward walking velocity derived from cadence:
  $$\text{speed} = \frac{\text{cadence}}{60} \times 0.66\text{ m/stride}$$

---

## 6. Longitudinal Tracking & Deviation Engine

The engine maintains a serializable state across sessions without requiring persistent background processes.

### 6.1 Calibration Phase (Sessions 1 to 4)
- **Minimum Baseline Sessions:** 4 completed, valid sessions.
- During calibration: `baseline_established = false`.
- The engine buffers `mobility_stability_score`, `cadence`, `symmetry_index`, and `estimated_speed`.
- Upon reaching 4 sessions, it computes personal baseline statistics:
  $$\mu = \text{mean}(\text{scores}), \quad \sigma = \text{std}(\text{scores})$$
  and sets `baseline_established = true`.

### 6.2 Monitoring Phase (Session 5+)
For each subsequent valid session:
1. **Deviation Condition:**
   $$\text{is\_deviation} = \left(\text{mobility\_stability\_score} < \mu - 2\sigma\right)$$
2. **Consecutive Counter:**
   - If `is_deviation == true`: `consecutive_deviation_count += 1`
   - If `is_deviation == false`: `consecutive_deviation_count = 0` (Recovery resets counter)
3. **Persistent Alert Flag:**
   $$\text{persistent\_change\_flagged} = \left(\text{consecutive\_deviation_count} \ge 3\right)$$

---

## 7. Room DB Persistence Schema (JSON Contract)

The exact entity schema saved to the local Android Room database (PRD §11):

```json
{
  "session_id": "sess_20260905_090000_b2x4",
  "timestamp": "2026-09-05T09:00:00Z",
  "data_quality": {
    "is_valid_session": true,
    "walking_duration_seconds": 32.5,
    "signal_quality_index": 0.96,
    "stationary_intervals": 0
  },
  "device_telemetry": {
    "sensor_type": "IMU_ACCEL_GYRO",
    "sample_rate_hz": 50,
    "acceleration_delegate": "NNAPI"
  },
  "heuristic_metrics": {
    "cadence_steps_per_min": 102,
    "symmetry_index": 0.95,
    "estimated_speed_m_s": 1.10
  },
  "inference_results": {
    "mobility_stability_score": 88
  },
  "deviation_engine": {
    "baseline_established": true,
    "is_current_session_substantial_deviation": false,
    "consecutive_deviation_count": 0,
    "persistent_change_flagged": false
  }
}
```

---

## 8. Frontend UI / UX Guidelines

The UI maps the pipeline state into three human-centered visual tiers:

```
                  ┌─────────────────────────────────┐
                  │      Is Baseline Established?   │
                  └──────────────┬──────────────────┘
                                 │
                 ┌───────────────┴───────────────┐
                 │ NO                            │ YES
                 ▼                               ▼
      [Calibrating Badge]          ┌───────────────────────────┐
     "Walk X of 4 complete"        │ persistent_change_flagged │
                                   └─────────────┬─────────────┘
                                                 │
                                 ┌───────────────┴───────────────┐
                                 │ YES                           │ NO
                                 ▼                               ▼
                           🔴 RED STATE            ┌───────────────────────────┐
                        (Persistent Change)        │ is_current_session_...    │
                                                   └─────────────┬─────────────┘
                                                                 │
                                                 ┌───────────────┴───────────────┐
                                                 │ YES                           │ NO
                                                 ▼                               ▼
                                           🟡 AMBER STATE                  🟢 GREEN STATE
                                          (Change Detected)                   (Stable)
```

### 8.1 The 3 Visual States
| State | Condition | Visual Indicator | UI Messaging |
| :--- | :--- | :--- | :--- |
| **🟢 Stable** | `is_deviation == false` | Green hero badge, trend line | "Your mobility stability is aligned with your personal baseline." Shows Score, Cadence, Symmetry, Speed. |
| **🟡 Change Detected** | `is_deviation == true` AND count $< 3$ | Amber banner / notice | "A temporary variation in your movement was detected. Please verify your phone is positioned in your pocket during tracking." |
| **🔴 Persistent Change** | `persistent_change_flagged == true` (3 consecutive drops) | Red sticky alert | "Persistent changes in your mobility pattern have been detected over recent walks. Consider discussing these changes with a healthcare professional." |

### 8.2 Mandatory UI Rules (§14)
1. **Rule 2 — Strict Disease-Agnostic Vocabulary:** NEVER display words like *"Parkinson's"*, *"Fall Risk Probability"*, *"Alzheimer's"*, or *"Disease Detection"*.
2. **Rule 3 — Relative Scoring Only:** NEVER display the Mobility Stability Score as a *"Health %"* or *"Clinical Grade"*. It is an index of movement consistency against personal baseline.
3. **Rule 8 — Explainability:** Always present deterministic metrics (Cadence, Symmetry, Speed) alongside the score so users understand the physical context of the reading.

---

## 9. Kotlin Reference Integration Snippet

```kotlin
// Example Android inference routine
class DeclineRadarInference(
    private val tfliteInterpreter: Interpreter,
    private val channelMean: FloatArray, // Length 6
    private val channelStd: FloatArray   // Length 6
) {
    fun runSessionInference(rawImuWindow: Array<FloatArray>): Int {
        require(rawImuWindow.size == 1500 && rawImuWindow[0].size == 6)

        // 1. Z-Score Normalization: [1, 1500, 6]
        val inputBuffer = Array(1) { Array(1500) { FloatArray(6) } }
        for (t in 0 until 1500) {
            for (c in 0 until 6) {
                inputBuffer[0][t][c] = (rawImuWindow[t][c] - channelMean[c]) / channelStd[c]
            }
        }

        // 2. Execute TFLite Model
        val outputBuffer = Array(1) { FloatArray(1) }
        tfliteInterpreter.run(inputBuffer, outputBuffer)
        val pIrregular = outputBuffer[0][0]

        // 3. Score Translation Layer
        return Math.round((1.0f - pIrregular) * 100.0f).coerceIn(0, 100)
    }
}
```

---

## 10. Privacy & Offline Guarantees
- **No Network Permissions Required for ML Pipeline:** Inference occurs via on-device TFLite.
- **Zero Raw Data Egress:** Raw sensor frames never leave the device.
- **Retention:** Raw sensor arrays are processed in RAM and discarded; only the enriched Room DB entity record is persisted.
