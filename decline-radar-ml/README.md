# Functional Decline Radar: ML Pipeline

> **Privacy-preserving, disease-agnostic personal mobility change detection for Android**

The **Functional Decline Radar** is an edge-based machine learning and signal-processing pipeline. It continuously monitors human movement dynamics from 6-axis smartphone inertial sensors (accelerometer and gyroscope) to detect persistent functional decline relative to an individual's personal baseline.

---

## Key Highlights

- **100% On-Device & Offline:** Runs entirely on Android as a background process; no raw motion data or telemetry ever leaves the device.
- **Disease-Agnostic & Non-Diagnostic:** Detects longitudinal changes in functional movement patterns (cadence, symmetry, temporal regularity) without assigning clinical diagnoses or disease labels.
- **Ultra-Lightweight Edge Model:** 1D Temporal Convolutional Network (TCN) quantized to INT8 ($25.2\text{ KB}$, ~11.7K parameters), running via Android NNAPI or XNNPACK.
- **Parallel Architecture:** Combines deterministic biomechanical signal processing (cadence, step symmetry, estimated speed) with deep temporal pattern recognition.
- **Robust Longitudinal Deviation Engine:** Tracks user baseline ($\mu, \sigma$) and enforces a 3-consecutive-session persistence requirement before flagging persistent changes.

---

## Pipeline Architecture

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

## Directory Structure

```
decline-radar-ml/
├── data/
│   └── models/
│       ├── model_contract.json       # Committed JSON model contract (Z-score stats, shapes, metadata)
│       ├── gait_tcn.keras            # Trained Keras model (gitignored binary)
│       ├── gait_tcn_quantized.tflite # INT8 quantized edge model (gitignored binary)
│       └── normalization_stats.npz   # NumPy normalization stats (gitignored binary)
├── docs/
│   ├── PRD.md                        # Master Product Requirements Document
│   ├── DEVELOPER_INTEGRATION_GUIDE.md# End-to-end guide for Android frontend & backend engineers
│   └── tcn_target_definition.md      # Training target, labeling rationale, and score mapping
├── scripts/
│   ├── train_tcn.py                  # Trains TCN on synthetic + real UCI HAR data & exports contract
│   ├── convert_to_tflite.py          # Quantizes model to INT8 TFLite with parity verification
│   ├── test_false_negatives.py       # Comprehensive FN/FP evaluation on real and synthetic gait
│   └── check_uci_har.py              # Quick sanity check for UCI HAR data extraction
├── src/
│   ├── data_quality/                 # Gatekeeper: session validity, stillness, pocket orientation
│   ├── heuristics/                   # Deterministic metrics: cadence, symmetry index, speed
│   ├── model/                        # 1D TCN architecture and dataset builder
│   ├── deviation_engine/             # Baseline establishment & consecutive deviation tracking
│   ├── real_data/                    # UCI HAR dataset loader and overlap-stitching
│   └── synthetic/                    # Synthetic IMU generator with controlled perturbations
├── tests/                            # 19 automated pytest unit and integration tests
├── requirements.txt                  # Core Python dependencies
└── README.md                         # This file
```

---

## Quickstart & Setup

### 1. Prerequisites
- Python 3.10, 3.11, or 3.12
- Virtual environment tool (`venv`)

### 2. Installation
```bash
# Clone the repository and enter directory
cd decline-radar-ml

# Create and activate virtual environment
python -m venv .venv
# On Windows:
.venv\Scripts\activate
# On Linux / macOS:
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### 3. Run Automated Tests
```bash
pytest -v
```
All 19 unit tests across the Gatekeeper, Heuristics, TCN, Deviation Engine, and Dataset Loader should pass.

---

## Training & Model Export

### 1. Training the TCN
The training pipeline combines synthetic IMU walking sessions with real human walking anchors from the [UCI HAR Dataset](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones) (optional, if downloaded):

```bash
# Set path to UCI HAR dataset (optional, falls back to synthetic-only if omitted):
set UCI_HAR_ROOT=C:\path\to\UCI HAR Dataset    # Windows
export UCI_HAR_ROOT=/path/to/UCI\ HAR\ Dataset  # Linux/macOS

# Run training
python scripts/train_tcn.py
```
Outputs:
- `data/models/gait_tcn.keras`
- `data/models/model_contract.json` (committed contract with channel means/stds)
- `data/models/normalization_stats.npz`

### 2. Exporting & Quantizing to TFLite
```bash
python scripts/convert_to_tflite.py
```
Performs INT8 post-training quantization using representative data calibration, checks output divergence against the original Keras model, and exports:
- `data/models/gait_tcn_quantized.tflite` (~25.2 KB)
- `data/models/gait_tcn_float32.tflite` (~54.8 KB fallback)

### 3. Running False Negative / False Positive Evaluation
```bash
python scripts/test_false_negatives.py
```
Evaluates the model on:
1. Real UCI HAR regular walking windows (Ground Truth: Regular $\to$ 0% False Positives).
2. Clinically relevant gait perturbations (Freezing of Gait, Falls $\to$ 0% False Negatives).
3. Synthetic benchmark across all 4 perturbation types (Cadence Drift, Asymmetry, Jitter, Freezing).

---

## Model Contract Reference

The model contract is permanently committed in **[data/models/model_contract.json](data/models/model_contract.json)** so Android and edge developers have immediate access to input/output specifications without needing binary `.npz` files:

- **Input Shape:** `Float32[1, 1500, 6]` ($30\text{s} \times 50\text{ Hz}$, `[accel_x..z, gyro_x..z]`)
- **Output Shape:** `Float32[1, 1]` ($p_{\text{irregular}} \in [0.0, 1.0]$)
- **Score Translation:** $\text{mobility\_stability\_score} = \text{round}((1.0 - p_{\text{irregular}}) \times 100)$
- **Per-Channel Z-Score Normalization:**
  - `accel_x`: $\mu = 1.7238$, $\sigma = 3.8411$
  - `accel_y`: $\mu = -0.3198$, $\sigma = 1.0827$
  - `accel_z`: $\mu = 8.7149$, $\sigma = 4.4205$
  - `gyro_x`: $\mu = 0.0002$, $\sigma = 0.5774$
  - `gyro_y`: $\mu = -0.0003$, $\sigma = 0.5855$
  - `gyro_z`: $\mu = -0.0005$, $\sigma = 0.5558$

---

## Documentation Links

- **[Master PRD](docs/PRD.md)**: Product positioning, non-diagnostic constraints, architecture specifications, and Room DB schema.
- **[Developer Integration Guide](docs/DEVELOPER_INTEGRATION_GUIDE.md)**: Comprehensive guide for Android frontend (Compose) and backend (SensorService, Room DB) developers, including complete Kotlin inference snippets.
- **[TCN Target Definition](docs/tcn_target_definition.md)**: Details on the binary classification target, synthetic generator dynamics, and sim-to-real anchoring.
