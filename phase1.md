PHASE 1 — COMPLETE FIRST-TIME ONBOARDING EXPERIENCE
We have completed Phase 0 and verified that the Android project builds and runs successfully on a Pixel 9 Android emulator.
Now implement ONLY Phase 1.
Do not implement Phase 2 or any later functionality.
The objective is to create a complete, polished first-time onboarding experience for Gait Functional Decline Radar.

1. PHASE 1 GOAL
   Implement this first-time user flow:
   Launch
   ↓
   Welcome
   ↓
   How It Works
   ↓
   Privacy & Local Processing
   ↓
   Personal Information
   ↓
   Sensor / Data Explanation
   ↓
   Home
   The onboarding must be functional and testable on a Pixel 9 Android emulator.

2. IMPORTANT SCOPE RULE
   Do NOT implement:
   accelerometer collection
   gyroscope collection
   SensorManager runtime collection
   sensor readiness checking
   50 Hz sampling
   30-second assessment
   1500-frame window
   data quality gate
   cadence calculation
   symmetry calculation
   walking speed calculation
   TFLite
   TCN
   ML inference
   Mobility Stability Score
   baseline calculation
   deviation detection
   persistent-change detection
   assessment history
   charts
   real assessment flow
   camera
   CameraX
   MediaPipe
   video
   cloud backend
   authentication
   Those belong to later phases.
   Phase 1 is onboarding only.

3. W01 — WELCOME SCREEN
   Create a polished Welcome screen.
   Headline:
   "Understand your mobility over time."
   Supporting text:
   "Your everyday walking contains patterns related to movement and stability. This app helps you track changes over time using motion sensors already inside your phone."
   Primary CTA:
   "Get Started"
   Secondary action:
   "How it works"
   The screen should feel calm, trustworthy, modern, and accessible.
   Do not use disease-related language.

4. W02 — HOW IT WORKS
   Create a 3–4 page onboarding carousel.
   Slide 1:
   Title:
   "Your phone can sense movement"
   Body:
   "Your phone's built-in motion sensors can capture patterns from the way you walk."
   Slide 2:
   Title:
   "Repeat assessments create a baseline"
   Body:
   "Repeated walks help establish your usual movement pattern."
   Slide 3:
   Title:
   "Compare you with you"
   Body:
   "Future assessments are compared with your own history."
   Slide 4:
   Title:
   "Designed for privacy"
   Body:
   "Movement data and analysis are designed to remain on your device."
   Provide appropriate:
   Next
   Back
   progress indicator
   final Continue/Get Started action
   The user should be able to navigate backward and forward cleanly.

5. W03 — PRIVACY & LOCAL PROCESSING
   Create a dedicated privacy explanation screen.
   Title:
   "Your movement data is sensitive."
   Body:
   "The assessment uses your phone's motion sensors and is designed to be processed locally on your device. Raw sensor data and model inference remain on the device in the MVP."
   Explain that the application is designed around:
   local processing
   on-device analysis
   no camera
   no video
   no cloud processing of raw movement data
   Explain that stored information may include:
   profile information
   assessment timestamps
   assessment metadata
   derived movement metrics
   Mobility Stability Scores
   baseline statistics
   temporary raw sensor data where required for processing
   Do not claim "100% private" or make unsupported absolute privacy claims.
   Use a clear Continue action.

6. O01 — PERSONAL INFORMATION
   Create a profile form.
   Required:
   First name
   Last name
   Date of birth OR age range
   Height
   Optional:
   Preferred name
   Relevant mobility context
   Do not ask for an emergency contact.
   Create sensible input fields and validation.
   Required fields must not allow invalid/empty submission.
   Height should be numeric and validated reasonably.
   Do not overcomplicate validation.

7. PROFILE PERSISTENCE
   For Phase 1, implement the simplest appropriate local persistence mechanism for onboarding/profile state using the existing project foundation.
   The required behavior is:
   User completes onboarding
   ↓
   Profile/onboarding completion is stored locally
   ↓
   App is restarted
   ↓
   Onboarding is skipped
   ↓
   Home is displayed
   Use a simple flag such as:
   hasCompletedOnboarding
   Do NOT implement the complete Room product database in this phase.
   Do NOT create final Assessment, GaitAnalysis, UserBaseline, or Settings entities.
   Those belong to later phases.

8. O02 — SENSOR / DATA EXPLANATION
   Create a screen explaining how movement will eventually be collected.
   Title:
   "Your phone measures movement."
   Body:
   "The assessment uses your phone's accelerometer and gyroscope to capture movement while you walk."
   Additional instruction:
   "Keep the phone securely in your pocket and walk naturally."
   Visually explain:
   Accelerometer
   X / Y / Z
   Gyroscope
   X / Y / Z
   IMPORTANT:
   This is explanatory UI only.
   Do NOT access SensorManager.
   Do NOT collect sensor data.
   Do NOT request or display live sensor values.
   Do NOT perform sensor readiness checks.
   Sensor implementation belongs to Phase 3.

9. NAVIGATION
   Update the existing navigation system so that:
   New user:
   Launch
   → Welcome
   → How It Works
   → Privacy
   → Personal Information
   → Sensor Explanation
   → Home
   Returning user:
   Launch
   → Home
   Do not allow normal back navigation from Home to reopen onboarding after onboarding has been completed.
   Use the existing navigation architecture from Phase 0.

10. ONBOARDING STATE
    The application must know whether onboarding has been completed.
    Use a local persistent state.
    Expected logic:
    if onboarding has not been completed:
    show Welcome
    if onboarding has been completed:
    show Home
    When the user successfully finishes the final onboarding step:
    set hasCompletedOnboarding = true
    then navigate to Home.
    Do not mark onboarding complete before the required profile and sensor explanation steps have been completed.

11. UI / DESIGN
    Use the design system created in Phase 0.
    Visual direction:
    "Modern healthcare + calm technology"
    Prioritize:
    clear hierarchy
    readable typography
    soft surfaces
    restrained rounded components
    simple illustrations
    generous spacing
    accessible tap targets
    trustworthy appearance
    Avoid:
    excessive neon
    excessive 3D
    hospital-management appearance
    disease imagery
    frightening warnings
    fake numerical precision
    The app should feel trustworthy before futuristic.

12. ACCESSIBILITY
    Implement basic accessibility correctly:
    readable typography
    sufficient contrast
    large tap targets
    meaningful content descriptions
    screen-reader-friendly labels
    do not communicate important information through color alone
    Use Compose accessibility semantics where appropriate.

13. PACKAGE STRUCTURE
    Add onboarding code under the existing architecture.
    A reasonable structure is:
    feature/
    └── onboarding/
    ├── WelcomeScreen.kt
    ├── HowItWorksScreen.kt
    ├── PrivacyIntroScreen.kt
    ├── PersonalInformationScreen.kt
    ├── SensorExplanationScreen.kt
    ├── OnboardingViewModel.kt
    ├── OnboardingUiState.kt
    └── components/
    Adapt the exact structure if necessary to maintain clean architecture.
    Do not create unnecessary complexity.

14. CODE QUALITY
    Follow the existing Phase 0 architecture.
    Keep:
    UI
    ↓
    ViewModel
    ↓
    Use Case
    ↓
    Repository
    where appropriate.
    Do not put persistent-state/business logic directly into composables.
    Do not put future sensor/ML logic into onboarding.
    Do not modify working Phase 0 architecture unnecessarily.
    Preserve all existing Phase 0 functionality.

15. PIXEL 9 VERIFICATION
    Before declaring Phase 1 complete:
    Build successfully.
    Install on a Pixel 9 Android emulator.
    Fresh-install/clear application data to simulate a new user.
    Launch the application.
    Verify Welcome appears.
    Navigate through How It Works.
    Verify Privacy screen.
    Complete Personal Information.
    Verify validation.
    Continue to Sensor Explanation.
    Complete onboarding.
    Verify Home appears.
    Force-close the application.
    Relaunch it.
    Verify onboarding is skipped and Home opens.
    Verify Android back navigation does not reopen onboarding.
    Verify there are no crashes.
    Verify there are no compilation errors.

16. PHASE 1 DEFINITION OF DONE
    Phase 1 is complete only when:
    Welcome works.
    How It Works works.
    Privacy explanation works.
    Personal Information works.
    Input validation works.
    Sensor/Data explanation works.
    Onboarding state persists locally.
    New users reach Home.
    Returning users skip onboarding.
    Navigation works correctly.
    Pixel 9 emulator works.
    No sensor collection exists.
    No ML exists.
    No fake Mobility Stability Score exists.
    No assessment functionality exists.
    No camera/video functionality exists.
    Existing Phase 0 functionality remains intact.

17. FINAL REPORT
    After implementation, report:
    Files created/modified.
    Dependencies added, if any.
    Navigation changes.
    Persistence implementation.
    Screens implemented.
    Validation behavior.
    Pixel 9 testing results.
    Build result.
    Any known issues.
    STOP after Phase 1.
    Do not automatically implement Phase 2.
