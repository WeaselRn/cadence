# PRD: Functional Decline Radar ML Pipeline

### Android + 1D TCN

## 1. Product Positioning & Core Definitions

This system is a **Personal Functional Mobility Radar**. It is a disease-agnostic, privacy-preserving tool designed for the early identification of persistent changes in human movement.

It is strictly **not** a disease detector, Parkinson's diagnostic tool, or medical assessment system.

### Functional Decline

**Functional Decline** is defined as a persistent deterioration in measurable mobility characteristics relative to an individual's established personal baseline.

Relevant characteristics include:

* Walking speed
* Cadence
* Stride regularity
* Gait symmetry
* Acceleration variability
* Temporal stability

### Mobility Stability Score

The **Mobility Stability Score** is a model-derived relative index representing the consistency of observed mobility with expected movement patterns.

It is **not**:

* A percentage of health
* A probability of disease
* A diagnostic score
* A medical assessment

---

## 2. System Architecture Overview

The system runs entirely offline on Android as a background sensor process.

To ensure defensibility and clinical explainability, the analysis is decoupled into a parallel architecture rather than forcing a single neural network to process everything.

### Architecture Components

#### 2.1 Sensor Telemetry

The Android `SensorManager` continuously samples **6 axes of movement** from:

* Accelerometer: X, Y, Z
* Gyroscope: X, Y, Z

The device is assumed to be positioned in the user's pocket during tracking.

#### 2.2 Data Quality Gatekeeper

A preprocessing layer validates incoming sessions and discards invalid sessions before ML execution.

#### 2.3 Deterministic Signal Processing — Heuristics

A rule-based mathematical layer calculates explainable physical metrics from raw acceleration and gyroscope signals.

Example metrics include:

* Cadence
* Step symmetry
* Estimated walking speed
* Stride regularity
* Acceleration variability

#### 2.4 Temporal Convolutional Network — TCN

A quantized `.tflite` model evaluates complex, non-linear temporal representations and produces a pattern-based movement score.

#### 2.5 Deviation Engine

A longitudinal tracking system cross-references:

* Deterministic metrics
* TCN movement score
* Individual baseline

The engine determines whether observed changes represent a persistent functional deviation.

---

## 3. Data Ingestion & Preprocessing Schema

The data ingestion pipeline relies on time-series signal processing from internal device hardware.

### Target Tensor Shape

```text
(Batch, Sequence_Length, Features)
```

### Sequence Length

A fixed analysis window is used.

**Example:**

```text
1500 frames = 30 seconds
```

assuming a 50 Hz sampling rate.

### Features

Each frame contains 6 sensor features:

| Feature Group | Axes    |
| ------------- | ------- |
| Accelerometer | X, Y, Z |
| Gyroscope     | X, Y, Z |

Therefore:

```text
Features = 6
```

### Normalization

The pipeline applies standard scaling:

```text
x_normalized = (x - μ) / σ
```

where:

* `μ` = mean of the relevant sensor axis
* `σ` = standard deviation of the relevant sensor axis

This helps account for differences in sensor calibration and signal magnitude across hardware.

---

## 4. Data Quality & Session Validity Layer

The Data Quality Gatekeeper prevents unreliable or incomparable sessions from entering the ML inference pipeline.

This is necessary to reduce false positives caused by confounding factors such as:

* Climbing stairs
* Carrying heavy objects
* Dropping the phone
* Phone being placed in a bag
* Insufficient walking duration
* Excessive sensor noise
* Unstable device orientation

### Confidence Checks

The system evaluates:

1. Signal quality
2. Continuous walking duration
3. Stationary intervals
4. Device orientation
5. Sensor consistency

### Rejection Rule

If a session fails the data quality check, the session is discarded and **no ML inference is performed**.

For example:

```text
Valid walking duration = 7 seconds
Required duration = 30 seconds
Result = INVALID
Action = Discard session
```

---

## 5. TCN Model Architecture

The 1D Temporal Convolutional Network handles non-linear temporal pattern recognition and is optimized for edge-device execution.

### Input

```text
(Sequence_Length, 6)
```

Example:

```text
(1500, 6)
```

### TCN Architecture

The model consists of three TCN blocks with increasing dilation rates.

```text
Input
  │
  ▼
TCN Block 1
Causal Conv1D
Dilation = 1
  │
  ▼
TCN Block 2
Causal Conv1D
Dilation = 2
  │
  ▼
TCN Block 3
Causal Conv1D
Dilation = 4
  │
  ▼
Global Average Pooling 1D
  │
  ▼
Output Layer
Single Neuron
Sigmoid Activation
```

### Dilation Strategy

The increasing dilation rates expand the receptive field without requiring memory-heavy recurrent states.

```text
d = 1 → local temporal patterns
d = 2 → medium-range temporal patterns
d = 4 → longer-range temporal patterns
```

### Output

The model produces a single sigmoid probability representing the model's estimated probability that the input window belongs to the irregular/atypical pattern class.

---

## 6. TCN Training Target & Dataset Definition

The model strictly discriminates between:

* **Typical walking dynamics**
* **Atypical/perturbed walking dynamics**

The model does not predict or classify medical diagnoses.

### Target

Binary classification:

```text
Regular gait       = 1
Perturbed gait     = 0
```

The target represents whether a 30-second IMU window exhibits regular steady-state walking dynamics.

### Label 1 — Regular

Regular samples represent:

* Steady cadence
* Cadence between approximately 90–120 steps/min
* Low stride-to-stride variability
* Symmetric left/right steps
* Consistent speed
* Realistic sensor noise

### Label 0 — Perturbed

Perturbed samples are generated from the regular base generator with one or more randomized perturbations.

Potential perturbations include:

* Cadence drift
* Step asymmetry
* Temporal jitter
* High-frequency noise
* Freezing-like episodes

---

## 7. Dataset Strategy

### Phase A — Synthetic Data

Synthetic training data is generated in pairs for each sample.

Each generated sample contains:

```text
(raw_session, label)
```

Example:

```text
Regular session  → Label 1
Perturbed session → Label 0
```

The synthetic generator controls:

* Cadence
* Step timing
* Symmetry
* Speed
* Noise
* Perturbation severity

This allows controlled generation of different movement patterns and perturbation severities.

### Phase B — Real Data Integration

Real-world sessions are added as additional **Label 1 — Regular** examples to anchor the model to realistic device sensor characteristics.

Real sessions must not automatically be treated as perturbed merely because they differ from synthetic regular walking.

A real session cannot be assigned a perturbed label without an appropriate ground-truth labeling methodology.

### Pipeline Impact

The introduction of real data does not change the data-quality architecture.

```text
Data Quality Gatekeeper
        │
        ▼
Valid Session
        │
        ├───────────────┐
        ▼               ▼
Heuristic Processing   TCN
        │               │
        └───────┬───────┘
                ▼
        Deviation Engine
```

The synthetic generator outputs:

```text
(raw_session, label)
```

The TCN is trained using binary cross-entropy.

---

## 8. TCN Training Objective

The model is trained using binary cross-entropy:

```text
L = -[y log(p) + (1-y) log(1-p)]
```

where:

* `y` = ground-truth label
* `p` = predicted probability

The objective is to distinguish temporal movement patterns rather than identify a disease.

---

## 9. Score Translation

The raw model output is transformed into the application-facing Mobility Stability Score.

The application-facing score is calculated as:

```text
mobility_stability_score = round((1 - p_irregular) * 100)
```

where:

```text
p_irregular = TCN probability of the irregular/atypical class
```

Therefore:

|     TCN Output | Mobility Stability Score |
| -------------: | -----------------------: |
| 0.00 irregular |                      100 |
| 0.10 irregular |                       90 |
| 0.25 irregular |                       75 |
| 0.50 irregular |                       50 |
| 0.75 irregular |                       25 |
| 1.00 irregular |                        0 |

The score should always be interpreted relative to the individual's longitudinal baseline.

---

## 10. Longitudinal Tracking & Deviation Engine

The Deviation Engine determines whether an individual's mobility is persistently changing.

The system moves away from isolated hardcoded triggers and instead uses a configurable ruleset.

### Baseline Establishment

A baseline requires a minimum number of valid, high-quality walking sessions.

Recommended initial configuration:

```text
Minimum baseline sessions = 3–5
```

The baseline stores statistical characteristics such as:

```text
μ = baseline mean
σ = baseline standard deviation
```

for relevant mobility metrics and model outputs.

### Deviation Detection

A session may be considered substantially deviated when it falls outside the configured baseline range.

Example:

```text
Substantial deviation:
current_value < μ - 2σ
```

The exact threshold should remain configurable.

### Persistence Requirement

A single abnormal session should not automatically trigger a persistent-change alert.

The engine evaluates:

1. Magnitude of deviation
2. Session reliability
3. Data quality
4. Number of consecutive deviations
5. Configured persistence threshold

Example:

```text
Deviation threshold = μ - 2σ
Persistence requirement = 3 consecutive valid sessions
```

Only after the persistence requirement is satisfied should the system flag a persistent change.

---

## 11. Data Contracts & Persistence Schema

The Android background pipeline stores the complete enriched session record locally using **SQLite/Room**.

### Example Record

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

### Persistence Requirements

All raw sensor data and derived inference data remain on-device.

The persistence layer must support:

* Session identification
* Timestamping
* Data-quality results
* Device telemetry
* Deterministic metrics
* ML inference results
* Baseline statistics
* Deviation state
* Persistence counters

---

## 12. Frontend Interface Contract

The UI translates complex pipeline outputs into simple, actionable insights.

The interface must avoid diagnostic terminology and should communicate changes relative to the user's own baseline.

### UI States

| UI State                    | System Condition                                                                                     | UX Behavior                                                                                                            |
| --------------------------- | ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **Stable — Green**          | Current mobility is consistent with the established baseline.                                        | Display hero score and standard physical metrics.                                                                      |
| **Change Detected — Amber** | Recent mobility differs from the usual pattern but has not persisted.                                | Display a subtle warning indicator and prompt the user to ensure ideal tracking conditions.                            |
| **Persistent Change — Red** | Mobility stability has dropped below the configured threshold for the required consecutive sessions. | Display a sticky alert suggesting that the user consider discussing persistent changes with a healthcare professional. |

### Stable

The user sees:

* Mobility Stability Score
* Cadence
* Estimated speed
* Symmetry
* Other supported physical metrics

### Change Detected

The system communicates that recent measurements differ from the established baseline.

The UX should avoid implying that the user has a disease or medical condition.

The user may be prompted to:

* Repeat tracking under normal conditions
* Keep the phone in the usual pocket position
* Avoid unusual activities during measurement

### Persistent Change

When the configured persistence criteria are satisfied, the UI displays a persistent alert.

Recommended communication:

> Persistent changes in your mobility pattern have been detected. Consider discussing these changes with a healthcare professional.

---

## 13. Privacy & Offline Execution

The system is designed as an offline-first, privacy-preserving pipeline.

### Strict Offline Execution

All of the following remain on the device:

* Raw sensor data
* Preprocessed sensor data
* ML inference
* Heuristic calculations
* Baseline statistics
* Deviation calculations
* Session history

No raw sensor telemetry should be transmitted to a remote server as part of the core pipeline.

### Local ML Execution

The TCN is deployed as a quantized TensorFlow Lite model:

```text
model.tflite
```

Inference occurs directly on the Android device.

The configured acceleration delegate may include:

```text
NNAPI
```

or another supported on-device delegate.

---

## 14. Cross-PRD Implementation Rules

The following rules are mandatory across the implementation.

### Rule 1 — Strict Offline Execution

All raw sensor data and inference must remain on the device.

### Rule 2 — No Disease Labeling

The user-facing application must not use diagnostic disease terminology.

The UI, alerts, and localized persistence schemas must remain disease-agnostic.

### Rule 3 — Relative Scoring

The Mobility Stability Score represents consistency with expected movement patterns.

It must not be presented as:

* Health percentage
* Disease probability
* Diagnostic confidence
* Medical risk percentage

### Rule 4 — Score Translation

The TCN output is inverted and scaled before being exposed to the application:

```text
mobility_stability_score = round((1 - p_irregular) * 100)
```

### Rule 5 — Quality Before Inference

Invalid sessions must be rejected before ML inference.

```text
Raw Sensor Data
      │
      ▼
Data Quality Gatekeeper
      │
      ├── Invalid → Discard
      │
      └── Valid
           │
           ▼
       ML + Heuristics
```

### Rule 6 — Baseline Before Deviation

Deviation detection should only become active after a sufficient number of valid sessions have established a personal baseline.

### Rule 7 — Persistence Before Alerting

A single unusual session must not automatically produce a persistent-change alert.

The deviation must satisfy the configured persistence criteria.

### Rule 8 — Explainability

Where possible, user-facing insights should be supported by deterministic physical metrics rather than relying exclusively on the neural-network score.

---

## 15. End-to-End Pipeline

The complete system operates as follows:

```text
┌──────────────────────────────┐
│ Android SensorManager       │
│ Accelerometer + Gyroscope   │
│ 6 axes @ fixed sample rate  │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│ Data Quality Gatekeeper     │
│ Signal + Duration + Pose    │
└──────────────┬───────────────┘
               │
         ┌─────┴─────┐
         │           │
         ▼           ▼
┌───────────────┐ ┌───────────────┐
│ Deterministic │ │ TCN Model     │
│ Heuristics    │ │ .tflite       │
│               │ │               │
│ Cadence       │ │ Temporal      │
│ Symmetry      │ │ Pattern       │
│ Speed         │ │ Recognition   │
│ Variability   │ │               │
└───────┬───────┘ └───────┬───────┘
        │                 │
        └────────┬────────┘
                 ▼
┌──────────────────────────────┐
│ Score Translation            │
│ (1 - p_irregular) × 100     │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│ Longitudinal Baseline        │
│ & Deviation Engine           │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│ Room / SQLite Persistence    │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│ Android UI                   │
│                              │
│ Stable                       │
│ Change Detected              │
│ Persistent Change            │
└──────────────────────────────┘
```

---

## 16. Core Design Principle

The system should be understood as a **personal longitudinal movement-change detection system**, not a diagnostic model.

Its primary objective is:

> **Detect persistent changes in an individual's measurable mobility patterns relative to their own established baseline, while keeping computation and sensor data local to the device.**

The combination of:

1. Deterministic physical metrics,
2. Temporal neural-network pattern recognition,
3. Data-quality validation, and
4. Longitudinal baseline comparison

forms the core of the Functional Decline Radar ML Pipeline.
