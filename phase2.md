PHASE 2 — MAIN APPLICATION SHELL
Phase 0 and Phase 1 have been completed and verified on a Pixel 9 Android emulator.
Now implement ONLY Phase 2.
Do not implement Phase 3 or any later functionality.
The purpose of this phase is to transform the completed onboarding experience into the main application shell.

1. PHASE 2 GOAL
   After onboarding is completed, the user should enter a functional Home screen and be able to navigate to:
   Assessment Introduction
   Profile
   Edit Profile
   Privacy & Data
   Settings
   About / Help
   History placeholder
   The app must remain runnable on the Pixel 9 emulator.

2. CRITICAL SCOPE BOUNDARY
   DO NOT IMPLEMENT:
   SensorManager runtime collection
   Accelerometer collection
   Gyroscope collection
   50 Hz sampling
   Sensor readiness checking
   30-second assessment
   1500 × 6 tensor
   Quality gate
   Signal processing
   Cadence
   Symmetry
   Walking speed
   TFLite
   TCN
   ML inference
   Mobility Stability Score
   Baseline calculations
   Deviation calculations
   Persistent-change detection
   Real assessment history
   Fake assessment results
   Camera
   CameraX
   MediaPipe
   Video
   Cloud processing
   Authentication
   Those belong to later phases.

3. HOME SCREEN — FIRST-TIME USER
   Implement H01.
   After onboarding, show:
   "Good morning, {Name}"
   Main card:
   "Start your first mobility assessment"
   Supporting text:
   "Your first few assessments help establish your personal baseline."
   Primary CTA:
   "Start assessment"
   Do NOT display fake Mobility Stability Scores.
   Do NOT display fake graphs.
   Do NOT create fake assessment history.
   If there are no real assessments, clearly show an empty state.

4. ASSESSMENT ENTRY POINT
   The "Start assessment" button must navigate to an Assessment Introduction screen.
   Implement A01 only.
   Title:
   "Ready for your mobility assessment?"
   Instructions:
   Put your phone securely in your pocket.
   Find a clear area where you can walk safely.
   Walk naturally.
   Keep your phone in the same pocket during the assessment.
   Do not intentionally change your pace.
   Safety copy:
   "Make sure your walking area is clear before starting."
   "Walk only if you feel safe and comfortable."
   "Stop the assessment if you feel unstable or unsafe."
   CTA:
   "Get ready"
   At the end of Phase 2, the button may navigate to a clearly marked placeholder for the future sensor assessment.
   DO NOT implement actual sensor functionality.

5. RETURNING USER HOME ARCHITECTURE
   Prepare the Home screen architecture to support future returning-user data.
   The UI should be capable of displaying in future phases:
   latest assessment date
   processing status
   latest Mobility Stability Score
   baseline/calibration status
   longitudinal state
   Create reusable components if appropriate, such as:
   LatestAssessmentCard
   BaselineStatusCard
   MobilityScoreCard
   LongitudinalStateCard
   However, because Phase 2 has no real assessments:
   DO NOT populate these components with invented data.
   Use appropriate empty-state behavior.

6. PROFILE SCREEN
   Implement P01.
   Display the profile information created during onboarding:
   first name
   last name
   preferred name if available
   age/date of birth
   height
   relevant mobility context if available
   completed assessment count
   baseline status
   At Phase 2:
   assessment count should be zero if no real assessment exists.
   Baseline should show:
   "Not established"
   Do not fabricate baseline information.
   Provide:
   "Edit profile"

7. EDIT PROFILE
   Implement P02.
   Allow the user to edit:
   first name
   last name
   preferred name
   age/date of birth
   height
   relevant mobility context
   Validate input.
   Save changes locally.
   After saving:
   Edit Profile
   → Profile
   The Home greeting must use the updated profile name.
   Preserve the existing onboarding persistence architecture.
   Do not introduce unnecessary database complexity in this phase.

8. PRIVACY & DATA
   Implement P03.
   Use the product's privacy-first/local-first messaging.
   Title:
   "Your movement data is sensitive."
   Explain that:
   the assessment uses phone motion sensors
   analysis is designed to run locally on the device
   raw movement data is not intended to be uploaded to the cloud
   the app does not use camera/video in the MVP
   Explain that future stored information may include:
   profile information
   assessment timestamps
   assessment metadata
   derived movement metrics
   Mobility Stability Scores
   baseline statistics
   temporary raw sensor data when needed for processing
   Create a Data Controls section with UI actions for:
   Delete assessment data
   Delete all local data
   Auto-delete temporary raw sensor data
   Because the actual assessment database does not exist yet, do not pretend that assessment data exists.
   Actions must fail safely or show an appropriate empty-state message.
   Do not make unsupported claims such as "100% private."

9. SETTINGS
   Implement P04.
   Create sections for:
   Assessment
   Provide the future structure for assessment-related settings.
   Do not implement sensor settings yet.
   Accessibility
   Include UI/state for:
   Larger text
   Reduced motion
   Voice instructions
   Haptic feedback
   Notifications
   Include UI/state for:
   Assessment reminders
   Do not implement actual notification scheduling yet.
   Privacy
   Include:
   Auto-delete temporary sensor data
   Privacy & Data navigation
   Use clean Compose state management.

10. ABOUT / HELP
    Implement a simple About / Help screen.
    Include:
    "Gait Functional Decline Radar"
    Version number from the application version.
    Description:
    "This app is designed to help you observe changes in your mobility over time."
    Also state:
    "This is not a diagnostic tool and does not replace professional medical advice."
    Include sections for:
    How assessments work
    Privacy
    Help / feedback placeholder
    Do not implement external support infrastructure.

11. HISTORY PLACEHOLDER
    Prepare the navigation destination for History.
    Do not create fake assessments.
    If the user opens History with zero assessments, display:
    "No assessments yet."
    Supporting text:
    "Complete your first mobility assessment to begin building your personal history."
    This screen will be fully implemented in a later phase.

12. MAIN NAVIGATION
    Create a clean main-app navigation structure.
    The final product will eventually contain:
    Home
    Assessment
    History
    Profile
    Settings
    Use appropriate Navigation Compose architecture.
    A reasonable bottom navigation structure is:
    Home
    Assess
    History
    Profile and Settings can be accessed through the Home/profile/menu structure.
    If you choose another navigation structure, keep it consistent with the existing Phase 0/1 architecture.
    Do not create navigation loops.

13. STATE MANAGEMENT
    Maintain clean separation:
    UI
    ↓
    ViewModel
    ↓
    State / Repository
    Do not read persistence directly inside composables.
    Create appropriate UI state models.
    For Home, the state should distinguish at least:
    no assessments
    assessments available in future phases
    Do not implement actual assessment persistence yet.

14. DESIGN
    Use the Phase 0 design system.
    Visual direction:
    "Modern healthcare + calm technology"
    Prioritize:
    readable typography
    generous spacing
    clear hierarchy
    soft surfaces
    restrained rounded components
    accessible controls
    trustworthy appearance
    Avoid:
    excessive neon
    excessive 3D
    disease imagery
    alarming medical language
    fake numerical precision

15. ACCESSIBILITY
    Ensure:
    sufficient contrast
    large tap targets
    meaningful labels
    screen-reader-friendly controls
    important information is not conveyed only by color
    readable text

16. PRESERVE PHASE 0 AND PHASE 1
    Do not rewrite working architecture unnecessarily.
    Existing behavior must continue to work:
    New user:
    Launch
    → Onboarding
    → Home
    Returning user:
    Launch
    → Home
    Do not break onboarding persistence.

17. PIXEL 9 TESTING
    Before declaring Phase 2 complete:
    Test 1
    Fresh install / clear app data.
    Complete onboarding.
    Verify:
    Onboarding
    → Home
    Test 2
    Verify Home shows:
    "Good morning, {Name}"
    and:
    "Start your first mobility assessment"
    Test 3
    Tap Start Assessment.
    Verify Assessment Introduction appears.
    Test 4
    Verify all safety instructions are visible.
    Test 5
    Open Profile.
    Verify onboarding profile information is displayed.
    Test 6
    Edit profile.
    Change the name.
    Save.
    Return Home.
    Verify the updated name appears.
    Test 7
    Open Privacy & Data.
    Verify privacy information.
    Test 8
    Open Settings.
    Verify settings sections.
    Test 9
    Open About / Help.
    Verify the screen.
    Test 10
    Open History.
    Verify the empty state.
    Do not show fake assessments.
    Test 11
    Force-close application.
    Relaunch.
    Verify Home opens directly.
    Test 12
    Perform a clean rebuild.
    Verify:
    Gradle build succeeds
    no compilation errors
    no runtime crashes

18. PHASE 2 DEFINITION OF DONE
    Phase 2 is complete only when:
    Home works.
    First-time empty state works.
    User name is dynamic.
    Assessment introduction works.
    Profile works.
    Edit Profile works.
    Profile persistence works.
    Privacy & Data works.
    Settings works.
    About / Help works.
    History empty state works.
    Main navigation works.
    Pixel 9 emulator works.
    Phase 1 onboarding still works.
    No fake scores exist.
    No fake assessments exist.
    No sensors are collected.
    No ML runs.
    No baseline is calculated.
    No camera/video exists.
    No crashes exist.

19. FINAL REPORT
    After completing Phase 2, report:
    Files created/modified.
    Screens implemented.
    Navigation changes.
    State/persistence changes.
    Settings implementation.
    Profile implementation.
    Pixel 9 testing results.
    Build result.
    Known issues.
    STOP after Phase 2.
    Do not automatically implement Phase 3.
