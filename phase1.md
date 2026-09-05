PHASE 0 — Project Foundation & Architecture
Build Phase 0 of the Android application:
Gait Functional Decline Radar
This is the FOUNDATION phase only.
The goal is to create a clean, scalable, compilable Android project that will support the later onboarding, sensor collection, gait analysis, TFLite inference, Room persistence, baseline calculation, history, and longitudinal monitoring features.
IMPORTANT SCOPE RULE
Implement ONLY Phase 0.
Do NOT implement features belonging to later phases.
Do not attempt to build the complete application from the PRD in this phase.
Do not implement actual gait assessment, sensor collection, gait metrics, ML inference, baseline calculations, history functionality, or polished onboarding.
The project must be runnable after this phase.

1. Product Context
   The application is a privacy-first, local-first Android application for longitudinal personal mobility monitoring.
   The core future workflow is:
   Walk
   → collect accelerometer + gyroscope data
   → validate session
   → analyze locally
   → store result
   → compare with personal baseline
   The application is NOT a disease detector, diagnostic tool, Parkinson's detector, medical assessment, or disease probability calculator.
   The updated product uses phone motion sensors rather than camera/video.

2. Technology Stack
   Set up the project using:
   Kotlin
   Jetpack Compose
   Material 3
   Navigation Compose
   ViewModel
   Kotlin Coroutines
   Kotlin Flow
   Hilt
   Room
   Android SensorManager
   TensorFlow Lite dependency/integration foundation as appropriate
   Use current stable versions compatible with the project.
   Do not introduce unnecessary dependencies.

3. Architecture
   Use a clean layered architecture following:
   UI
   ↓
   ViewModel
   ↓
   Use Cases
   ↓
   Repositories
   ↓
   Sensor / Local Data / ML Adapter
   The architecture must keep the UI independent from implementation details of sensors, Room, and TFLite.

4. Project Structure
   Create a structure equivalent to:
   app/
   ├── core/
   │ ├── database/
   │ ├── sensors/
   │ ├── storage/
   │ ├── permissions/
   │ ├── design/
   │ └── utilities/
   │
   ├── feature/
   │ ├── onboarding/
   │ ├── home/
   │ ├── assessment/
   │ ├── results/
   │ ├── history/
   │ ├── profile/
   │ ├── privacy/
   │ └── settings/
   │
   ├── ml/
   │ └── MlInferenceAdapter
   │
   └── navigation/
   Use appropriate Kotlin package naming.
   Create only the foundation required for this phase.
   Do not fully implement future feature packages.

5. Application Setup
   Create:
   MainActivity
   Application class if required by Hilt
   Compose theme
   Material 3 configuration
   Navigation host
   Initial destination
   The app must launch successfully.
   For the initial screen, create a simple foundation/placeholder screen that clearly indicates that the application is running.
   Example content:
   "Gait Functional Decline Radar"
   "Project Foundation"
   "App is running"
   Do not create final onboarding or assessment UI yet.

6. Navigation Foundation
   Create the navigation infrastructure required for future screens.
   Define destinations for the future architecture if useful, such as:
   Onboarding
   Home
   Assessment
   Results
   History
   Profile
   Privacy
   Settings
   However, only the initial placeholder destination needs to be functional in Phase 0.
   Do not build the actual screens for these future destinations.

7. Hilt
   Configure Hilt correctly.
   Create the application-level Hilt setup.
   Create only the dependency-injection foundation required at this stage.
   Do not create the complete dependency graph for future sensors, ML, and database functionality yet unless it is necessary for compilation.

8. Room Foundation
   Configure Room so that the project is ready for later local persistence.
   Do NOT implement the complete product database yet.
   Do NOT create the final User, Assessment, GaitAnalysis, UserBaseline, and Settings entities yet.
   Those will be implemented in a later phase.
   If a minimal placeholder is technically required for Room configuration, keep it minimal and clearly isolated.

9. Sensor Layer Foundation
   Create the sensor-layer architecture for:
   Android SensorManager
   ├── Accelerometer
   └── Gyroscope
   The final application will collect:
   Accelerometer X/Y/Z
   Gyroscope X/Y/Z
   at a target sampling rate of approximately 50 Hz.
   However:
   DO NOT IMPLEMENT SENSOR COLLECTION IN PHASE 0.
   Do not start a sensor listener.
   Do not collect data.
   Do not display accelerometer values.
   Do not display gyroscope values.
   Only establish the package/interface structure needed for Phase 3.

10. ML Adapter Foundation
    Create a clean abstraction for future ML inference.
    Create an interface equivalent to:
    MlInferenceAdapter
    The exact method signatures may be designed appropriately for Kotlin, but the key architectural requirement is:
    The UI and repositories must NOT directly depend on TensorFlow Lite implementation details.
    The future architecture should allow:
    MlInferenceAdapter
    ↓
    TFLite implementation
    Do NOT load a TFLite model.
    Do NOT perform inference.
    Do NOT generate fake ML scores.
    Do NOT display Mobility Stability Score in Phase 0.

11. Design System Foundation
    Create a centralized Compose design system.
    Prepare:
    Theme
    Typography
    Colors
    Shapes
    Basic dimensions if useful
    The eventual visual direction is:
    "Modern healthcare + calm technology"
    Use clean, readable, trustworthy styling.
    Do not spend excessive effort on final visual polish in Phase 0.
    The goal is consistency and a foundation for later phases.

12. Permissions Foundation
    Create the permissions package/foundation needed for future Android permissions.
    Do not request unnecessary permissions.
    In particular:
    DO NOT request camera permission.
    The final application does not use camera/video functionality.
    Do not request notification permissions yet unless absolutely necessary for the foundation.
    Sensor permissions/setup should be handled in the future sensor phase rather than unnecessarily complicating Phase 0.

13. Privacy / Offline Architecture
    The application is local-first.
    Do NOT add:
    Firebase
    cloud database
    backend API
    remote ML API
    authentication
    cloud storage
    sensor-data upload
    The core future workflow must be capable of operating offline.
    Do not implement the complete privacy UI yet.

14. Explicitly Forbidden in Phase 0
    Do NOT implement:
    CameraX
    camera permission
    camera preview
    video recording
    MP4 creation
    MediaPipe
    pose estimation
    pose landmarks
    gait classification
    TFLite inference
    Mobility Stability Score
    cadence calculation
    symmetry calculation
    walking speed calculation
    quality gate
    30-second sensor session
    baseline calculation
    deviation engine
    persistent-change detection
    history
    charts
    onboarding
    profile workflow
    assessment workflow
    results workflow
    cloud services
    authentication
    chatbot
    disease detection
    medical diagnosis
    These belong to later phases.

15. Code Quality Requirements
    Use:
    clear naming
    small focused classes
    interfaces where appropriate
    separation of concerns
    immutable UI state where appropriate
    Kotlin best practices
    Compose best practices
    Avoid:
    giant MainActivity
    business logic inside composables
    hardcoded future ML logic
    unnecessary singleton objects
    tightly coupling UI to SensorManager
    tightly coupling UI to Room
    tightly coupling UI to TensorFlow Lite

16. Pixel 9 Verification
    Before declaring Phase 0 complete:
    Open the project in Android Studio.
    Sync Gradle successfully.
    Build the debug application.
    Start a Pixel 9 Android emulator.
    Install the application.
    Launch the application.
    Verify the initial Compose screen renders.
    Verify there are no runtime crashes.
    Verify there are no compilation errors.
    Verify the project can be rebuilt from a clean build.
    The application must be genuinely runnable on the Pixel 9 emulator.

17. Phase 0 Definition of Done
    Phase 0 is complete only when:
    Gradle sync succeeds.
    Project compiles.
    Debug APK builds.
    App installs on Pixel 9 emulator.
    App launches successfully.
    Compose renders successfully.
    Navigation foundation exists.
    Hilt foundation exists.
    Room foundation exists.
    Sensor architecture foundation exists.
    ML adapter abstraction exists.
    Design system foundation exists.
    No camera/video/MediaPipe functionality exists.
    No fake ML results exist.
    No future feature is partially implemented in a way that creates misleading functionality.

18. Final Response Required From Devin
    After implementation, report:
    Project structure created.
    Files created or modified.
    Dependencies added.
    Architecture decisions made.
    How to run the project in Android Studio.
    Pixel 9 emulator verification status.
    Build status.
    Any known issues.
    Do not proceed to Phase 1 automatically.
    STOP after completing Phase 0.

