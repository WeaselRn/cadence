Gait Functional Decline Radar — Codebase Improvement PRD

Document type: Implementation / codebase improvement PRD
Scope: Existing Android app only
Purpose: Finalize the current app for polished hackathon/demo/release readiness without changing the trained ML model contract or expanding the product scope.

1. Objective

This document defines the concrete codebase changes required to take the current Android implementation from a functional prototype into a polished, reliable, production-style hackathon build.

This is not a replacement product PRD. It intentionally avoids redefining the product, ML research scope, or feature roadmap. It focuses on:

UI/UX cleanup

gesture/navigation behavior

removal of prototype/debug artifacts

completing currently incomplete actions

correcting implementation inconsistencies

improving sensor robustness

improving baseline/deviation behavior

improving accessibility and mobile layout

improving release-readiness

Non-goals

Do not use this work to:

retrain or redesign the TCN

replace the existing model

introduce cloud processing

add camera/video processing

add authentication

add a clinician dashboard

add unrelated features

over-engineer the hackathon MVP

2. Priority Legend

Priority

Meaning

P0

Must fix before final demo/release build

P1

Strongly recommended before final demo

P2

Polish / optional if time remains

3. P0 — Remove Developer / Prototype UI From Production

3.1 Remove ML Model Analysis (Debug) from the user-facing Results screen

Current issue

AssessmentSummaryScreen currently renders a card titled "ML Model Analysis (Debug)" containing:

Model Version

Input Tensor Shape

Inference Time

Raw p_irregular

The current implementation explicitly exposes the raw model probability and internal tensor information to users.

This is development/debug information, not product UI.

Required change

Remove the entire debug card from the normal production UI.

Do not display:

raw p_irregular

tensor shape

internal model execution details

internal timing diagnostics

developer terminology

Release-safe rule

The normal Results screen should expose only:

Mobility Stability Score

supported physical movement metrics

longitudinal state

appropriate interpretation text

non-diagnostic disclaimer

Optional developer mode

A developer-only debug surface may remain behind an explicit build flag such as:

BuildConfig.DEBUG

but it must never appear in release builds.

Acceptance criteria

Release APK contains no visible ML Model Analysis (Debug) card.

Release APK contains no visible p_irregular value.

Searching the production UI source for Raw p_irregular returns no user-facing occurrence.

Debug information, if retained, is strictly debug-build gated.

4. P0 — Navigation and Back Gesture Redesign

4.1 Remove visible text Back buttons

The current app repeatedly renders large OutlinedButton("Back") controls at the top of screens including onboarding, assessment, History, Settings, Privacy/Data, About, and Edit Profile.

Examples are present in:

HowItWorksScreen

PrivacyIntroScreen

PersonalInformationScreen

SensorExplanationScreen

AssessmentIntroScreen

AssessmentReadinessScreen

HistoryScreen

SettingsScreen

PrivacyDataScreen

AboutScreen

EditProfileScreen

Required change

Remove the redundant large text-based Back buttons from normal mobile screens.

The navigation model should rely on:

Android system back navigation / edge swipe gesture.

Navigation Compose back stack behavior.

Explicit in-content cancellation only where the action is meaningfully different from navigation (for example, Stop assessment).

Do not replace every Back button with another large custom button.

Important safety rule

Do not remove navigation functionality until the Android back gesture has been verified on a real device.

The physical-device behavior must be tested for:

left-edge swipe back

right-edge swipe back where supported/configured

Android back button

predictive-back animation where supported

nested navigation flows

Acceptance criteria

No generic large Back text button remains on standard secondary screens.

Android system back gesture returns to the previous logical screen.

Navigation does not unexpectedly exit the application from secondary screens.

Assessment cancellation remains a distinct explicit safety/action control.

4.2 Do not implement fake per-screen swipe-back gestures

Do not add ad-hoc pointerInput swipe listeners to every screen just to simulate navigation.

Use the platform navigation stack and Android back gesture behavior instead.

Custom horizontal swipe gestures should be reserved for actual paged content, especially onboarding.

5. P0 — Convert the How-It-Works Flow Into a Real Swipeable Pager

Current issue

HowItWorksScreen is named like a carousel but currently changes pages using buttons. The code stores an integer page and renders a Next button; it does not use a real HorizontalPager / pager state.

Current behavior is therefore:

Tap Next → page changes

rather than:

Swipe left/right → page changes

Required change

Replace the current integer-only carousel implementation with a real Compose pager, such as HorizontalPager with PagerState.

Required behavior:

swipe left → next slide

swipe right → previous slide

page indicator updates automatically

final slide exposes Continue

tapping Continue advances/finishes as appropriate

system back gesture still exits the How-It-Works flow

UX requirements

Use a conventional onboarding experience:

[ visual/content ]

Title
Description

● ○ ○ ○

Swipe to continue

The page indicator should make the number of slides obvious without requiring a visible Back button.

Acceptance criteria

User can move through all onboarding slides by horizontal swipe.

Swipe direction and page transitions feel natural on a physical Android phone.

No horizontal swipe conflicts with vertical content scrolling.

Pager state survives recomposition correctly.

The final page has a clear Continue/Get Started action.

6. P0 — Fix History Interaction and Add Assessment Detail Flow

Current issue

HistoryScreen renders AssessmentHistoryCard, but the card is not currently a navigation target.

There is also no dedicated AssessmentDetail route in the current navigation model.

Required change

Create:

Screen.AssessmentDetail

with an assessment identifier argument.

Make each history card clickable.

Flow:

History
↓
Tap assessment
↓
Assessment Detail

Assessment Detail must show

date/time

duration

Mobility Stability Score

cadence

estimated speed, when available

symmetry, when available

baseline/longitudinal state

model version where useful

deletion action

Do not show:

raw p_irregular

tensor shape

internal sensor telemetry

debug diagnostics

Acceptance criteria

Every stored assessment can be opened from History.

The selected assessment ID is passed correctly through navigation.

Detail view displays the selected record, not the latest record by accident.

Delete from detail updates History immediately.

7. P0 — Implement Actual Data Deletion

Current issue

The repository/DAO layer has deletion support, but the Privacy/Data UI currently contains controls whose callbacks are placeholders.

Required implementation

Implement three clearly separated actions as appropriate to the current data model:

A. Delete selected assessment

Delete only the selected assessment.

B. Delete all assessment data

Delete every stored assessment from Room.

C. Delete all local application data

Clear the local profile/preferences in DataStore and assessment database state.

After full deletion, the app should return to a clean first-run state where appropriate.

Safety requirements

Use confirmation dialogs for destructive actions.

Example flow:

Delete all assessment data?

This will permanently remove your stored assessment history from this device.

[Cancel] [Delete]

Acceptance criteria

No delete button is a no-op.

Destructive actions require confirmation.

Room is actually cleared.

DataStore profile/onboarding state is actually cleared for full local-data deletion.

UI refreshes without requiring an application restart.

No deleted record reappears after process restart.

8. P0 — Remove Prototype-Like Placeholder Actions

Audit the entire codebase for text/comments and callbacks indicating unfinished functionality, including patterns such as:

Future action

TODO

not implemented

placeholder

Debug

fake/sample score text

temporary hardcoded values that look like real user results

buttons that exist but do nothing

Any UI element shown to a normal user must either:

work correctly, or

not be present.

Do not leave dead controls in the release build.

9. P0 — Correct Baseline / Deviation Logic

Current issue

The current code uses a deviation threshold based on approximately -1.5σ and also a fixed 10-point fallback.

The intended finalized implementation should use the configured deviation rule consistently.

Required change

Centralize the deviation settings in one configuration object.

Example:

object BaselineConfig {
const val minimumBaselineWalks = 3
const val deviationZThreshold = -2.0f
const val persistentDeviationCount = 3
}

Do not scatter these values across UI, ViewModels, and repositories.

Required behavior

Baseline not established
↓
Building Baseline

Baseline established
↓
Current score < baseline mean - configured threshold
↓
Change Detected
↓
If deviation persists for N consecutive valid sessions
↓
Persistent Change

Acceptance criteria

One source of truth for thresholds.

Threshold is not hardcoded in UI.

Consecutive deviation behavior is deterministic and tested.

A recovery/stable session resets the consecutive deviation count.

10. P0 — Ensure Invalid Sessions Never Produce or Save a Score

Maintain the strict ordering:

Sensor collection
↓
Quality gate
↓
Valid?
┌───┴───┐
No      Yes
↓        ↓
Reject   Metrics + ML
↓
Result

Required code review

Verify that:

invalid sessions never invoke TFLite inference

invalid sessions do not produce a fabricated score

invalid sessions are not inserted as completed valid assessments

the UI does not display a score for a rejected session

retry starts a clean assessment state

Important

The ML adapter currently returns a fallback-looking 50 score in its exception object. That value must never be interpreted or displayed as a real inference result.

Acceptance criteria

Any inference failure must remain an explicit failure state:

isSuccess = false
score = null / unavailable at UI level

Do not use 50 as a synthetic result.

11. P1 — Improve Sensor Synchronization

Current issue

The collector combines the latest accelerometer and gyroscope values whenever either sensor emits an event. The combined sample therefore may contain values from different timestamps.

Required change

Use timestamped queues/buffers for accelerometer and gyroscope samples.

Pair samples by nearest timestamp within a small tolerance.

Conceptually:

Accel queue ─┐
├─ timestamp alignment → 6-axis sample
Gyro queue ──┘

Acceptance criteria

Combined IMU samples use temporally aligned accel/gyro measurements.

Samples remain ordered by timestamp.

Large timing gaps are rejected or handled explicitly.

Preprocessing still produces the exact [1, 1500, 6] tensor.

12. P1 — Make Sensor Collection Explicitly Target 50 Hz

The preprocessor already defines a 20 ms target interval for 50 Hz.

The collection layer should explicitly request an appropriate sensor rate rather than relying only on SENSOR_DELAY_GAME.

Required behavior

Request the intended target rate, then validate the actual observed rate.

Do not assume requested frequency equals delivered frequency.

Acceptance criteria

Sensor registration uses an explicit target sampling period where supported.

Actual timestamps remain authoritative.

Quality validation still rejects sessions with unsuitable sampling behavior.

13. P1 — Strengthen the Data Quality Gate

The current gate is useful but should better represent the intended quality checks.

Where technically practical, validate:

continuous walking duration

sensor availability

timestamp continuity

stationary intervals

orientation / pocket-likeness

signal quality

minimum usable sample count

Required UX

When a session fails, show the user a specific, actionable reason.

Examples:

Not enough continuous walking.

The phone moved too little during the assessment.

Phone position was not suitable for this assessment.

Do not expose internal threshold values unless useful to the user.

14. P1 — Make Settings Functional and Persistent

Current issue

Settings switches are currently local remember state, meaning changes are not a durable application setting.

Required change

Create a settings repository backed by DataStore.

Persist at least:

larger text

reduced motion

voice instructions

haptic feedback

assessment reminders, if enabled

auto-delete temporary sensor data

Acceptance criteria

Change a setting.

Leave Settings.

Return to Settings.

Setting remains changed.

Restart the app.

Setting remains changed.

Important

Do not implement settings that do not actually change application behavior.

15. P1 — Improve Mobile UI Layout and Responsiveness

The current UI relies heavily on:

large fixed paddings such as 24.dp / 28.dp

full-width cards

large text blocks

stacked controls

hardcoded vertical spacers

This is acceptable for a prototype but needs refinement for real phones.

Required goals

The layout must work on:

small Android phones

standard phones

taller phones

large font accessibility settings

portrait orientation

Rules

Prefer:

responsive fillMaxWidth() components with sensible max content width

consistent horizontal margins

WindowInsets / safe-area awareness

scrollable content when necessary

bottom actions that remain reachable

fewer excessively tall cards

consistent spacing scale

Avoid:

controls pushed below the fold accidentally

excessive empty vertical space

text cramped against card edges

important CTA buttons requiring long scrolling

decorative content dominating the actual action

16. P1 — Establish a Consistent App Shell

The current screens independently construct their own top-level layout.

Create a reusable screen scaffold pattern for secondary screens.

Conceptual structure:

App content
├── consistent safe-area handling
├── title / section heading
├── content
└── action area where required

Do not recreate slightly different top spacing, padding, background treatment, and card styles in every screen.

Acceptance criteria

Secondary screens feel like one application, not separate prototypes.

Typography hierarchy is consistent.

Horizontal padding is consistent.

CTA heights and shapes are consistent.

Background treatment is consistent.

17. P1 — Refine the Visual Design

The current design is structurally calm but still has a prototype feel because many screens use very similar large gradients and large cards.

Required visual direction

Move toward:

quieter backgrounds

stronger visual hierarchy

one primary focal area per screen

fewer competing cards

restrained use of gradients

consistent corner radii

consistent elevation

clear primary vs secondary actions

Results screen

This is the most important screen.

Make it the strongest screen visually.

Suggested hierarchy:

Mobility Stability Score

        85

Consistent with your usual pattern

[Cadence] [Speed] [Symmetry]

Baseline context

Disclaimer

Do not bury the score under implementation details.

18. P1 — Improve Results Screen UX

The results screen should answer four questions immediately:

What is my result?

What does it mean?

How does it compare with my baseline?

What should I do next?

Stable

Show a calm positive interpretation.

Change Detected

Explain that the recent result differs from the usual pattern and encourage another measurement under normal conditions.

Persistent Change

Clearly distinguish this from a one-off result and use the non-diagnostic healthcare-professional guidance.

Do not

show disease probabilities

show internal probability values

show unexplained numerical precision

use red/green as the only communication mechanism

19. P1 — Improve History Screen UX

Current history cards show date, score, duration, and steps but are visually static.

Required improvements

Each card should communicate:

date/time

score

longitudinal state

key metric summary

tap affordance

The whole card should be the touch target.

Use a subtle chevron or other standard affordance to indicate that the card opens details.

Optional P2 enhancement

Add a simple score-over-time chart once enough data exists.

The chart should show personal context rather than implying clinical severity.

20. P1 — Accessibility Improvements

Implement actual accessibility behavior, not just Settings toggles.

Required

sufficient touch target sizes

readable typography

proper semantic labels

meaningful screen-reader descriptions

content descriptions for decorative/meaningful icons

no critical information conveyed through color alone

dynamic text should not clip at large font sizes

important buttons should remain reachable

Assessment screen

Ensure a screen reader can understand:

time remaining

current status

stop action

success/failure state

21. P1 — Make Email / Contact Behavior Correct

The current source ingest does not expose a verified application support/contact email address, so do not invent one in code.

Required implementation

If the app includes an email/contact action in About/Help or elsewhere:

Define the verified project email in one centralized constant/config location.

Use an Android mailto: intent rather than relying on plain text.

Provide a graceful fallback if no email client is installed.

Ensure the displayed address exactly matches the address used by the intent.

Remove any typo, placeholder, stale address, or malformed email string.

Conceptually:

val supportEmail = "<verified-project-email>"

and:

mailto:<verified-project-email>

Acceptance criteria

Tapping the contact action opens the device email client with the correct recipient.

No malformed address appears in the UI.

Displayed and linked email values are identical.

The code does not contain multiple conflicting hardcoded email addresses.

22. P1 — Add Proper Processing State UX

The existing processing state machine is a good foundation.

Improve the UI so each processing phase is visually understandable without overwhelming the user.

Recommended progression:

Assessment captured
↓
Checking quality
↓
Preparing movement data
↓
Calculating movement metrics
↓
Analyzing locally
↓
Comparing with your baseline
↓
Saving
↓
Complete

Do not expose implementation terminology such as:

tensor

interpreter

NNAPI

raw probability

normalization constants

23. P1 — Improve Error Recovery

Every recoverable failure should have a clean recovery path.

Sensor unavailable

Return to a useful screen with a clear explanation.

Invalid assessment

Offer:

Try again

without creating a false result.

ML processing failure

Offer retry while preserving enough temporary state for retry where appropriate.

Storage failure

Do not silently discard the captured session.

Acceptance criteria

Every error state has:

user-understandable message

actionable next step

no misleading score

no dead-end navigation

24. P1 — Raw Sensor Data Lifecycle Cleanup

The app should ensure temporary raw sensor data does not remain indefinitely.

Required lifecycle

Collect
↓
Validate
↓
Analyze
↓
Save derived result
↓
Delete temporary raw data

When processing fails:

retain only what is needed for retry

apply a defined cleanup path

Acceptance criteria

successful processed sessions do not leave unnecessary raw data behind

discarded sessions clean up temporary data

failed sessions have a deterministic cleanup/retry behavior

25. P1 — Release Configuration Cleanup

Before building the final APK:

Remove or restrict

debug UI

verbose logs

test/sample data

developer-only labels

temporary diagnostics

fake scores

placeholder buttons

Verify

release version name/version code

model asset is packaged correctly

model contract version matches code

no debug-only code leaks into release behavior

Optional but recommended

Enable release shrinking/minification only after confirming the TFLite model and Room/Hilt runtime are preserved.

Do not introduce unnecessary release-build complexity immediately before the hackathon.

26. P1 — Improve ML Adapter Failure Contract

The MlPrediction model currently contains a numeric fallback-looking score even when inference fails.

This is risky because downstream code can accidentally treat a failed inference as a genuine prediction.

Required design

Use a result type that clearly distinguishes success and failure.

Example concept:

sealed interface MlResult {
data class Success(val prediction: MlPrediction) : MlResult
data class Failure(val message: String) : MlResult
}

At minimum, ensure:

isSuccess == false
→ no valid score available

Acceptance criteria

No failed inference can enter the baseline/history path as a genuine mobility score.

27. P1 — Prevent Accidental Baseline Contamination

Baseline statistics must use only valid, successfully analyzed assessments.

Exclude:

invalid sessions

failed inference

incomplete sessions

manually discarded sessions

assessments without a valid score

The baseline engine should not need to infer validity from arbitrary fields.

A repository query or domain filter should provide a clean list of valid completed assessments.

28. P1 — Baseline State Handling

The app should keep these states distinct:

0/3 valid walks
1/3 valid walks
2/3 valid walks
Baseline established

Do not count failed or rejected assessments toward the baseline.

After baseline establishment:

Stable
Change Detected
Persistent Change

The first three calibration sessions should not accidentally create deviation warnings based on an incomplete baseline.

29. P1 — Physical Device QA

The app should be tested on a real Android phone, not only the emulator.

Minimum manual matrix

Navigation

launch

onboarding

swipe onboarding

Android back gesture

Android back button

forward navigation

returning navigation

Assessment

sensors available

sensors unavailable

valid 30-second walk

too-short walk

stationary session

poor phone placement

cancel assessment

processing success

processing failure

Persistence

save assessment

restart app

History reload

Assessment Detail reload

delete one assessment

delete all assessments

full local-data reset

UI

small phone

tall phone

large font

dark/light theme where supported

screen reader

30. P1 — Test the Full Real Pipeline

Perform a real-device smoke test of:

Physical phone
↓
Accelerometer + gyroscope
↓
6-axis sample stream
↓
Quality gate
↓
Resampling
↓
Normalization
↓
TFLite
↓
Mobility Stability Score
↓
Baseline comparison
↓
Room
↓
History
↓
Assessment Detail

Confirm there are no silent substitutions, fake results, or lifecycle-only assumptions.

31. P2 — Add Longitudinal Chart

Once enough assessments exist, add a simple chart showing:

score over time

baseline mean

optional deviation threshold

The chart should:

remain readable on mobile

support large text

provide a textual alternative

avoid language implying clinical severity

This is a high-value hackathon polish item, but not more important than functional deletion, detail navigation, real gestures, or release cleanup.

32. P2 — Add Subtle Motion / Transition Polish

After navigation and functionality are stable, add restrained animations:

pager transitions

result reveal

baseline progress

processing progress

Avoid:

excessive 3D

flashy transitions

distracting motion during assessment

The app should feel calm and trustworthy.

33. P2 — Reduce UI Duplication

Extract reusable components where multiple screens currently repeat patterns:

standard page container

section header

primary CTA

secondary CTA

metric row

status badge

informational card

destructive-action dialog

This should improve consistency without forcing a large architecture rewrite.

34. P2 — Centralize Product Copy

Avoid scattering critical wording across multiple files.

Centralize reusable copy for:

score interpretation

baseline states

persistent change message

privacy explanation

assessment instructions

safety message

processing messages

This reduces inconsistencies during final demo polishing.

35. Recommended Implementation Order

Implement in this order:

1. Remove debug/prototype UI
2. Fix navigation/back behavior
3. Replace onboarding buttons with real swipe pager
4. Add clickable History → Assessment Detail
5. Implement actual deletion flows
6. Fix ML failure semantics
7. Fix baseline/deviation configuration
8. Harden sensor synchronization
9. Strengthen quality gate
10. Persist Settings
11. Refine mobile layout/UI hierarchy
12. Improve accessibility
13. Fix/centralize email contact behavior
14. Run real-device end-to-end testing
15. Apply final visual polish
16. Build final release APK

Do not start with animations or decorative work before P0 functionality is complete.

36. Final Definition of Done

The codebase is ready for the final hackathon APK when all of the following are true:

Product UI

No debug ML card is visible.

No raw p_irregular is visible.

No fake or placeholder result is visible.

No dead button remains in the normal user flow.

No unnecessary generic Back buttons remain.

Gestures / navigation

How-It-Works supports real horizontal swiping.

Android system back gesture works through the navigation stack.

Assessment stop remains an explicit safety control.

History cards open Assessment Detail.

Assessment

Real sensor data is collected.

Sensor timestamps are handled correctly.

Quality validation happens before ML.

Invalid sessions never create valid scores.

TFLite inference failures never become fake scores.

Results

Score is clearly presented.

Physical metrics are clearly presented.

Baseline comparison is understandable.

Persistent change is clearly differentiated from a single change.

Medical disclaimer remains visible and appropriate.

Persistence

Assessments survive restart.

Detail view reads the correct assessment.

Delete-one works.

Delete-all works.

Full local-data reset works.

Temporary raw data lifecycle is deterministic.

Settings

Settings persist across navigation and app restart.

Any displayed setting actually changes behavior.

Accessibility

large text does not break layouts.

touch targets are usable.

screen-reader labels are meaningful.

color is not the only status signal.

Release

Debug-only UI is excluded from release.

No fake data is present.

Model asset is packaged correctly.

Final APK installs on a physical Android phone.

Full assessment flow works offline.

37. Current Codebase Findings That Motivated This PRD

The following findings were observed directly in the supplied codebase ingest:

HowItWorksScreen uses a page integer plus Next button rather than a real swipe pager.

Multiple screens render OutlinedButton("Back").

AssessmentHistoryCard is currently rendered without a detail-navigation callback.

Screen has no dedicated Assessment Detail route.

Results currently includes an ML Model Analysis (Debug) section with raw p_irregular, tensor shape, model version, and inference time.

Settings values are currently local remember state rather than durable preferences.

Room provides deleteAssessment() and deleteAllAssessments(), but the privacy UI needs to wire destructive actions into those operations.

The baseline engine currently uses a -1.5σ / 10-point deviation condition, which should be centralized and aligned with the finalized configured rule.

Sensor collection currently combines latest accel/gyro values instead of explicitly timestamp-aligning them.

The ML adapter returns a fallback-looking score value in its failure object; downstream code must never treat that as a genuine result.

These are the primary code-level changes required to finish the existing application rather than redesign it.

38. Final Principle

Do not make the app bigger. Make the existing app feel finished.

The target is:

Functional prototype
↓
Reliable pipeline
↓
Polished mobile UX
↓
Correct gestures/navigation
↓
No developer artifacts
↓
Real-device validation
↓
Demo-ready APK

The trained ML model and core on-device architecture remain intact.