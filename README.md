# Gait Functional Decline Radar (Cadence Radar)

[![Android CI](https://img.shields.io/badge/Android-Kotlin-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TFLite-1D%20TCN-orange.svg)](https://www.tensorflow.org/lite)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Gait Functional Decline Radar (Cadence Radar)** is an advanced, local-first Android application designed to observe and track changes in walking dynamics (gait) over time using your phone's built-in motion sensors. Built with modern Android architecture and an on-device TensorFlow Lite machine-learning model, the app establishes a personal mobility baseline and monitors longitudinal movement consistency without uploading sensitive health data to the cloud.

---

## 🌟 Key Features

- **30-Second Walking Assessment**: Captures high-frequency 6-axis IMU data (accelerometer and gyroscope) at 50 Hz while the phone is securely carried in your pocket.
- **On-Device Machine Learning**: Executes inference locally on your device via a quantized 1D Temporal Convolutional Network (TCN) packaged in TensorFlow Lite.
- **Personal Baseline Engine**: Automatically establishes your baseline mobility pattern from your first 3 valid walking assessments and compares future walks against your own history rather than generalized population averages.
- **Dynamic Longitudinal Tracking**: Implements deterministic consecutive deviation rules (`STABLE`, `CHANGE_DETECTED`, `PERSISTENT_CHANGE`) by analyzing assessment history backward in time.
- **Robust Quality Gate**: Validates sensor continuity, sampling rate stability (35–65 Hz), duration (20–45s), and finite values before allowing ML processing.
- **Local-First Privacy**: No camera/video recording, no cloud upload of raw movement sensor data, and complete user control over data deletion via Room and DataStore.
- **Modern UI & Theming**: Built entirely with Jetpack Compose and Material 3, supporting seamless Light and Dark mode adaptation.

---

## 🛠️ Tech Stack

- **Language**: [Kotlin 2.0+](https://kotlinlang.org)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/)
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/guide/navigation/navigation-getting-started)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) & [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Concurrency**: [Kotlin Coroutines & Flows](https://kotlinlang.org/docs/coroutines-overview.html)
- **Machine Learning**: [TensorFlow Lite (2.17.0)](https://www.tensorflow.org/lite)
- **Testing**: [JUnit 4](https://junit.org/junit4/)

---

## 🏛️ Architecture & Project Structure

The project follows a clean feature-based package structure within a single Android application module (`:app`):

```text
com.cadence.gaitradar/
├── core/
│   ├── baseline/        # Personal baseline engine & longitudinal rules
│   ├── database/        # Room database, DAO, entities & repositories
│   ├── design/          # Material 3 theme, colors, typography & shapes
│   ├── di/              # Hilt modules (DataStore, Database, etc.)
│   ├── metrics/         # Physical gait features (cadence, speed, variability)
│   ├── permissions/     # Sensor & hardware permission managers
│   ├── quality/         # Deterministic IMU quality gate & validation
│   ├── sensors/         # Sensor collection & timestamp alignment collector
│   └── storage/         # Preferences & settings repositories
├── feature/
│   ├── about/           # Expandable accordion About & Help section
│   ├── assessment/      # Intro, readiness, active capture, summary & ViewModel
│   ├── history/         # Assessment history list & detailed record view
│   ├── home/            # Mobility overview dashboard & status cards
│   ├── onboarding/      # Welcome, swipeable HorizontalPager & personal info
│   ├── privacy/         # Local data control & deletion dialogs
│   ├── profile/         # User profile management
│   └── settings/        # Persistent settings toggles
├── ml/
│   ├── ImuPreprocessor.kt       # 50 Hz resampling & Z-score normalization
│   ├── MlInferenceAdapter.kt    # ML inference contract & prediction model
│   └── TfliteMlInferenceAdapter.kt # TFLite interpreter & model execution
└── navigation/
    ├── CadenceNavHost.kt        # Central NavHost & routing graph
    └── Screen.kt                # Type-safe navigation screen routes
```

---

## 🧠 Machine Learning Model Contract

The app integrates a quantized 1D Temporal Convolutional Network (TCN) located at `app/src/main/assets/models/gait_tcn_quantized.tflite`.

- **Input Tensor**: `[1, 1500, 6]` (Float32)
  - 1500 timesteps corresponding to a 30-second window sampled at 50 Hz.
  - 6 channels: `accel_x`, `accel_y`, `accel_z`, `gyro_x`, `gyro_y`, `gyro_z`.
- **Normalization**: Per-channel Z-score normalization adhering strictly to `model_contract.json` training statistics.
- **Output Tensor**: `[1, 1]` (Float32 probability `p_irregular` in `[0.0, 1.0]`).
- **Score Translation**:
  $$\text{mobility\_stability\_score} = \text{round}((1.0 - p_{\text{irregular}}) \times 100)$$

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer.
- **JDK**: Java 17 or newer.
- **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0+).
- **Physical Device or Emulator**: Android device running API 26+. (Note: Motion sensor assessments require a physical device for accurate accelerometer and gyroscope telemetry).

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/cadence-gait-radar.git
   cd cadence
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select **Open**, then choose the `cadence` project folder.
3. **Sync Gradle**:
   - Allow Android Studio to sync Gradle and download dependencies.
4. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. **Build and Deploy**:
   - Connect an Android device with USB Debugging enabled.
   - Click **Run 'app'** in Android Studio or build via CLI:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🤝 Contributing

Contributions to improve mobile accessibility, test coverage, sensor robustness, or UI polish are welcome!

1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git origin push feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
