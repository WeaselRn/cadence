# Business / Product Requirements Document (BRD/PRD)
# Gait Functional Decline Radar — Android Mobile App

**Document version:** 2.0  
**Status:** Hackathon MVP specification  
**Updated against:** `Functional Decline Radar ML Pipeline (Android + 1D TCN)`  
**Primary platform:** Android  
**Product principle:** Privacy-first, local-first, disease-agnostic mobility monitoring

---

## 1. Executive Summary

Gait Functional Decline Radar is an Android application that helps a user track persistent changes in everyday mobility relative to their own established baseline.

The application does **not** diagnose disease, identify Parkinson's disease, or provide a medical assessment. It captures short walking sessions using the phone's built-in **accelerometer and gyroscope**, processes the data locally, and presents a longitudinal Mobility Stability Score and explainable physical movement metrics.

The core experience is:

> **Walk with phone in pocket → collect IMU data → validate session → analyze locally → store result → compare with personal baseline**

There is **no camera recording, no video processing, and no MediaPipe dependency** in the updated MVP.

---

# 2. Product Positioning

## 2.1 Product category

**Personal functional mobility monitoring / longitudinal gait assessment**

## 2.2 One-line description

A privacy-first Android app that uses a phone's built-in motion sensors to track changes in walking patterns over time against the user's own personal baseline.

## 2.3 Core value proposition

> **Compare you with you.**

The product is designed to turn repeatable walking sessions into a longitudinal mobility signal rather than treating one measurement as a diagnosis.

## 2.4 What the product is not

The app must not describe itself as:

- a disease detector,
- a Parkinson's detector,
- a diagnostic tool,
- a medical assessment,
- a probability of disease,
- a percentage of health,
- a replacement for a physician or physiotherapist.

Use neutral language such as:

- movement,
- mobility,
- walking pattern,
- assessment,
- Mobility Stability Score,
- change from your personal baseline,
- persistent change,
- consider discussing persistent changes with a healthcare professional.

---

# 3. Core Product Requirements

## 3.1 Primary goals

The MVP must:

1. Allow a user to create a simple local profile.
2. Explain the product and privacy model clearly.
3. Collect walking data from the phone's accelerometer and gyroscope.
4. Require the phone to be carried in the user's pocket during collection.
5. Collect a fixed-length walking window suitable for the ML pipeline.
6. Reject poor-quality or non-comparable sessions before ML inference.
7. Run the finalized ML pipeline locally on Android.
8. Store approved derived results in Room.
9. Establish a personal baseline from valid completed sessions.
10. Compare later results against that baseline.
11. Detect substantial and persistent changes using the configured longitudinal rules.
12. Present results without diagnostic language.
13. Keep raw sensor data and inference on-device.

## 3.2 Secondary goals

If time remains:

- assessment reminders,
- accessibility improvements,
- data export,
- clinician/research report export,
- richer longitudinal charts.

## 3.3 MVP non-goals

Do not prioritize:

- camera recording,
- video storage,
- MediaPipe Pose,
- live pose estimation,
- cloud processing,
- cloud storage of raw movement data,
- wearable hardware,
- external sensors,
- disease classification,
- chatbot functionality,
- appointment booking,
- complex authentication,
- full clinician web application.

---

# 4. Target Users

## 4.1 Primary user

A single individual who wants to observe changes in their mobility over time.

## 4.2 Secondary users

- Older adults interested in mobility tracking.
- People undergoing or completing rehabilitation.
- Caregivers helping someone maintain a mobility history.

## 4.3 Future users

Clinicians or physiotherapists may eventually receive an exported report, but clinician workflows are outside the core MVP.

---

# 5. Updated Technical Architecture

## 5.1 High-level architecture

```text
                    USER
                      │
                      ▼
              ┌───────────────┐
              │  Android App  │
              └───────┬───────┘
                      │
              Start walking session
                      │
                      ▼
             Android SensorManager
                      │
              ┌───────┴────────┐
              │ Accelerometer  │
              │  + Gyroscope   │
              └───────┬────────┘
                      │
                6-axis IMU data
                      │
                      ▼
              Data Quality Gate
                      │
          ┌───────────┴───────────┐
          │                       │
        Invalid                  Valid
          │                       │
       Discard             Deterministic metrics
                                  │
                                  ▼
                         Fixed 30-sec window
                                  │
                                  ▼
                         Normalization
                                  │
                                  ▼
                           Quantized TFLite
                             1D TCN model
                                  │
                                  ▼
                       Mobility Stability Score
                                  │
                                  ▼
                         Deviation Engine
                                  │
                                  ▼
                             Room DB
                                  │
                                  ▼
                         Longitudinal UI
```

## 5.2 Important architecture correction

The previous app specification used a camera/video pipeline. That is no longer valid.

The updated app must **remove**:

- CameraX,
- camera permission,
- camera preview,
- recording controls,
- MP4 creation,
- temporary video storage,
- MediaPipe Pose Landmarker,
- 33 keypoint extraction,
- video-to-pose processing,
- 36-feature pose tensor,
- any live TCN analysis of camera frames.

The app's ML input is the phone's sensor telemetry.

---

# 6. Sensor Data Contract

## 6.1 Sensor source

Use Android `SensorManager` with:

- Accelerometer: X, Y, Z
- Gyroscope: X, Y, Z

Total:

> **6 features per frame**

## 6.2 Sampling

Target sampling frequency:

> **50 Hz**

The app should request the appropriate Android sensor sampling rate and record the actual timestamps/frequency needed to validate the session.

## 6.3 Phone placement

The user should:

> **Place the phone securely in a trouser/pants pocket and walk naturally.**

The app should not require the phone to be held in the hand.

The assessment instructions should warn against:

- carrying the phone in a bag,
- holding it loosely in the hand,
- placing it on an unstable surface,
- intentionally changing walking style.

## 6.4 Input tensor

The ML adapter receives:

```text
(Batch, Sequence_Length, Features)
```

For the MVP:

```text
Sequence_Length = 1500 frames
Features = 6
```

At 50 Hz, 1500 frames represent approximately:

> **30 seconds**

of walking data.

## 6.5 Normalization

The ML pipeline applies standard scaling:

> zero mean, unit variance across sensor axes.

The app must not independently apply a conflicting normalization scheme.

---

# 7. Session Collection

## 7.1 Assessment concept

An assessment is a short standardized walking session during which the phone collects accelerometer and gyroscope data.

The user should not need to interact with the phone while walking.

## 7.2 Recommended user flow

```text
Home
  ↓
Assess
  ↓
Assessment introduction
  ↓
Pocket / environment instructions
  ↓
Sensor readiness check
  ↓
Start walking
  ↓
~30 seconds of sensor collection
  ↓
Session complete
  ↓
Data quality validation
  ↓
Local processing
  ↓
Results
```

## 7.3 Collection UX

During collection show:

- session title,
- progress/timer,
- short walking instruction,
- simple indication that data is being collected,
- stop/cancel control.

Example:

```text
        Mobility Assessment

       Keep your phone in
          your pocket.

           Walk naturally

             00:18
          ━━━━━━━━━━

       ● Collecting movement data

             [ Stop ]
```

The UI must not display live TCN scores.

## 7.4 Safety

Before starting:

> Make sure your walking area is clear.

> Walk only if you feel safe and comfortable.

> Stop the assessment if you feel unstable or unsafe.

The application must never encourage a user to continue walking when they feel unsafe.

---

# 8. Data Quality Gatekeeper

## 8.1 Purpose

The Data Quality Gatekeeper prevents invalid or incomparable sessions from reaching the ML model.

This layer is separate from the TCN's gait-quality classification.

## 8.2 Quality checks

The app/pipeline should evaluate, where technically available:

- sufficient continuous walking duration,
- signal quality,
- stationary intervals,
- sensor availability,
- expected phone orientation/placement characteristics,
- whether the captured session is usable for the fixed analysis window.

## 8.3 Rejection behavior

If the session fails validation:

```text
Assessment could not be analyzed reliably.

Possible reasons:
• Not enough continuous walking
• Too much stationary time
• Poor sensor signal
• Phone placement was unsuitable

[ Try again ]
[ Save session ]
```

No ML inference should be run on a rejected session.

## 8.4 Important distinction

The quality gate answers:

> **"Is this session valid enough to analyze?"**

The TCN answers:

> **"Does this valid movement window resemble regular or atypical walking dynamics?"**

These must not be conflated.

---

# 9. Deterministic Physical Metrics

The application/ML adapter should calculate explainable physical metrics from the sensor signal.

The ML PRD explicitly identifies:

- cadence,
- step symmetry,
- estimated speed.

## 9.1 Cadence

Display as:

> **Steps/min**

Example:

> 102 steps/min

## 9.2 Symmetry

Display as:

> **Symmetry**

The exact display scale should match the finalized heuristic implementation. Do not fabricate a percentage interpretation if the implementation does not define one.

## 9.3 Estimated walking speed

Display as:

> **Estimated walking speed**

Example:

> 1.10 m/s

The UI should label this as an estimate rather than an exact measured walking speed.

## 9.4 Do not invent unsupported metrics

The updated MVP must not require:

- stride length,
- step length,
- joint angles,
- pose landmarks,
- confidence intervals,
- disease probability,
- clinical severity.

If a future validated implementation adds a metric, it can be added to the adapter contract and UI later.

---

# 10. ML Integration Contract

## 10.1 Input

The Android ML adapter receives the completed sensor session:

```text
1500 × 6 IMU frames
```

representing approximately 30 seconds at the target 50 Hz sampling rate.

## 10.2 Model

The model is:

- 1D Temporal Convolutional Network,
- three causal convolution blocks,
- dilation rates 1, 2, 4,
- global average pooling,
- single sigmoid output,
- quantized `.tflite`,
- suitable for Android edge inference.

## 10.3 Model target

Binary classification:

```text
Label 1 = Regular gait
Label 0 = Perturbed / atypical gait
```

The model must remain disease-agnostic.

## 10.4 Score translation

The raw model probability is not shown to users.

The frontend-facing score is:

```text
Mobility Stability Score
= round((1 - P_irregular) × 100)
```

Therefore:

- higher score = movement pattern more consistent with regular movement,
- lower score = movement pattern less consistent with regular movement.

The score is **not**:

- a health percentage,
- a disease probability,
- a clinical severity grade.

## 10.5 Frontend result object

The app should consume a structured hybrid result such as:

```json
{
  "assessmentId": "A-001",
  "modelVersion": "0.1.0",
  "metrics": {
    "cadence": 105,
    "walkingSpeed": 1.12,
    "symmetry": 0.94,
    "mobilityStabilityScore": 85
  },
  "processingStatus": "completed"
}
```

Optional physical metrics may be absent. The app must never fabricate missing values.

---

# 11. Longitudinal Baseline & Deviation Engine

## 11.1 Baseline principle

The app compares the user against their own historical movement rather than relying only on population thresholds.

## 11.2 Baseline establishment

A baseline requires multiple valid, high-quality completed walks.

Recommended MVP:

> **At least 3 valid walks**

A practical baseline can use approximately:

> **3–5 valid walks**

during the initial calibration period.

The app should show calibration progress rather than presenting an uncalibrated user with a misleading "decline" interpretation.

Example:

```text
Building your personal baseline

2 of 3 baseline walks completed

Complete 1 more valid assessment
to begin personal comparisons.
```

## 11.3 Baseline statistics

For the Mobility Stability Score, persist:

- mean `μ`,
- standard deviation `σ`,
- sample count,
- baseline start,
- baseline end.

## 11.4 Current-session deviation

A session is considered substantially below baseline when:

```text
score < μ − 2σ
```

The threshold should be configurable in the deviation engine rather than hardcoded throughout the UI.

## 11.5 Persistent change

The system should track consecutive substantial deviations.

Default MVP behavior may use:

> **3 consecutive deviations**

but the implementation must keep the consecutive-session limit configurable.

## 11.6 Important interpretation

A single low score should not be presented as a persistent decline.

Use three UI states:

### Stable

Current mobility is consistent with the established baseline.

### Change Detected

Recent mobility differs from the usual pattern, but the change has not persisted.

### Persistent Change

The configured consecutive-deviation limit has been reached.

Persistent change should prompt:

> Consider discussing persistent changes in your mobility with a healthcare professional.

Do not imply a diagnosis.

---

# 12. Updated Screen Inventory

| ID | Screen | Required |
|---|---|---|
| W01 | Welcome / Hero | Yes |
| W02 | How It Works | Yes |
| W03 | Privacy & Local Processing | Yes |
| O01 | Personal Information | Yes |
| O02 | Sensor / Data Explanation | Yes |
| O03 | Notification Permission, if needed | Optional |
| H01 | Home — First-Time | Yes |
| H02 | Home — Returning User | Yes |
| A01 | Assessment Introduction | Yes |
| A02 | Pocket & Walking Instructions | Yes |
| A03 | Sensor Readiness Check | Yes |
| A04 | Live Sensor Collection | Yes |
| A05 | Session Complete | Yes |
| A06 | Data Quality / Validation | Yes |
| A07 | Local Processing | Yes |
| A08 | Results | Yes |
| HI01 | History | Yes |
| HI02 | Assessment Detail | Yes |
| P01 | Profile | Yes |
| P02 | Edit Profile | Yes |
| P03 | Privacy & Data | Yes |
| P04 | Settings | Yes |
| P05 | About / Help | Recommended |
| P06 | Delete Local Data | Yes |
| E01 | Sensor Unavailable / Permission | Yes |
| E02 | Invalid Session | Yes |
| E03 | Processing Error | Yes |

### Removed from previous version

The following screens are no longer part of the MVP:

- Camera Permission
- Camera Positioning
- Camera Preview
- Live Video Recording
- Recording Complete / MP4 confirmation
- Video-specific processing states

---

# 13. Welcome Experience

## W01 — Welcome

Headline:

> **Understand your mobility over time.**

Supporting text:

> Your everyday walking contains patterns related to movement and stability. This app helps you track changes over time using motion sensors already inside your phone.

CTA:

> **Get Started**

Secondary:

> **How it works**

---

# 14. How It Works

Use 3–4 simple slides.

### Slide 1 — Your phone can sense movement

> Your phone's built-in motion sensors can capture patterns from the way you walk.

### Slide 2 — Repeat assessments create a baseline

> Repeated walks help establish your usual movement pattern.

### Slide 3 — Compare you with you

> Future assessments are compared with your own history.

### Slide 4 — Designed for privacy

> Movement data and analysis are designed to remain on your device.

Do not mention camera or video.

---

# 15. Onboarding

## O01 — Personal Information

Required:

- first name,
- last name,
- date of birth or age range,
- height.

Optional:

- preferred name,
- relevant mobility context.

Do not require an emergency contact.

## O02 — Sensor / Data Explanation

Before collecting data, explain:

> The assessment uses your phone's accelerometer and gyroscope to capture movement while you walk.

Explain:

> Keep the phone securely in your pocket and walk naturally.

The app should not request camera permission.

---

# 16. Home

## H01 — First-Time Empty State

Example:

> **Good morning, {Name}**

Main card:

> Start your first mobility assessment

Supporting text:

> Your first few assessments help establish your personal baseline.

CTA:

> **Start assessment**

Do not display fake scores or graphs.

## H02 — Returning User

Show:

- latest assessment date,
- processing status,
- latest Mobility Stability Score if available,
- baseline/calibration status,
- current longitudinal state if established.

Example:

```text
Your mobility

84
Mobility Stability Score

Stable

Compared with your personal baseline
```

If calibration is incomplete:

```text
Building your baseline
2 of 3 walks completed
```

---

# 17. Assessment Flow

## A01 — Assessment Introduction

Headline:

> Ready for your mobility assessment?

Instructions:

1. Put your phone securely in your pocket.
2. Find a clear area where you can walk safely.
3. Walk naturally.
4. Keep your phone in the same pocket during the assessment.
5. Do not intentionally change your pace.

CTA:

> **Get ready**

## A02 — Pocket & Walking Instructions

Show a simple illustration of:

```text
      PHONE
        ↓
   ┌─────────┐
   │ POCKET  │
   └─────────┘
        ↓
      WALK
    → → → → →
```

Instructions:

- phone securely in pocket,
- avoid bags,
- avoid holding the phone,
- clear walking area,
- normal walking pace.

CTA:

> **Check sensors**

## A03 — Sensor Readiness Check

Verify that:

- accelerometer is available,
- gyroscope is available,
- sensors are producing data,
- sampling can proceed,
- the phone is reasonably positioned for the session where detectable.

Example:

```text
✓ Accelerometer ready
✓ Gyroscope ready
✓ Movement signal detected

You're ready to walk.

[ Start ]
```

No ML inference occurs here.

---

# 18. Live Sensor Collection

## A04

This replaces the old camera-recording experience.

Required UI:

- assessment title,
- approximately 30-second timer/progress,
- concise instruction,
- collection status,
- stop/cancel control.

Example:

```text
       Mobility Assessment

       Keep your phone
          in your pocket.

         Walk naturally

            00:21
        ━━━━━━━━━━━━

      ● Collecting movement

            [ Stop ]
```

The phone collects:

```text
Accelerometer X/Y/Z
Gyroscope X/Y/Z
```

at the target sampling rate.

The TCN does not run during collection.

---

# 19. Session Complete

## A05

After collection:

> **Assessment captured**

Show:

- duration,
- date/time,
- sensor data captured,
- next processing step.

CTA:

> **Analyze locally**

Secondary:

> **Discard**

If discarded, the temporary raw session data must be deleted.

---

# 20. Data Validation

## A06

Before ML inference:

```text
Checking assessment quality...

✓ Sensor data captured
✓ Walking duration checked
✓ Signal quality checked
✓ Session comparability checked
```

If valid:

> Assessment is ready for local analysis.

If invalid:

> We couldn't get a reliable assessment from this session.

Provide actionable guidance and:

> **Try again**

No score should be shown for an invalid session.

---

# 21. Local Processing

## A07

Show explicit local processing:

```text
Analyzing on your device

✓ Movement data prepared
✓ Session validated
⟳ Calculating movement metrics
○ Running local model
○ Saving assessment
```

Do not imply upload or cloud processing.

The pipeline is:

```text
6-axis IMU
   ↓
quality validation
   ↓
physical metrics
   ↓
1500-frame window
   ↓
normalization
   ↓
quantized TFLite TCN
   ↓
Mobility Stability Score
   ↓
baseline/deviation engine
   ↓
Room
```

---

# 22. Results

## A08

The results screen is the primary frontend output of the ML integration.

### Primary result

Display:

> **Mobility Stability Score**

Example:

```text
              85

      Mobility Stability Score

        Consistent with
        your usual pattern
```

Do not write:

- "85% healthy",
- "15% chance of disease",
- "Parkinson's probability",
- "severity 15%".

## 22.1 Physical metrics

Where available:

```text
Cadence
105 steps/min

Estimated walking speed
1.12 m/s

Symmetry
0.94
```

Only show values actually returned by the adapter.

## 22.2 Longitudinal state

### Stable

> Your current mobility is consistent with your established baseline.

### Change Detected

> Your recent mobility differs from your usual pattern. One assessment alone does not establish a persistent change.

### Persistent Change

> Your mobility has shown a persistent change from your usual pattern. Consider discussing this change with a healthcare professional.

---

# 23. History

## HI01

Display chronological assessments.

Example:

```text
Sep 5
Mobility Stability Score   85
Stable

Sep 2
Mobility Stability Score   88
Stable

Aug 29
Mobility Stability Score   82
Change detected
```

For users still establishing a baseline:

```text
Sep 5
Assessment completed
Baseline walk 2 of 3
```

## 23.1 Charts

Once enough data exists, show:

- Mobility Stability Score over time,
- personal baseline mean,
- warning/deviation threshold where appropriate.

Avoid charts that imply clinical severity.

---

# 24. Assessment Detail

## HI02

Show:

- date/time,
- duration,
- processing status,
- Mobility Stability Score,
- cadence,
- estimated speed,
- symmetry,
- baseline status,
- deviation state,
- model version where useful.

Do not expose:

- raw TCN probability,
- internal tensor,
- sensor telemetry implementation details,
- unsupported confidence intervals.

Actions:

- Delete assessment
- Back

Sharing/export can remain future functionality.

---

# 25. Profile

## P01

Show:

- name,
- age/date of birth,
- height,
- optional profile information,
- number of completed assessments,
- baseline status.

Example:

```text
Assessments stored: 6
Baseline: Established
Cloud sync: Off
```

---

# 26. Privacy & Data

## P03

Privacy is a core product feature.

Suggested copy:

> **Your movement data is sensitive.**
>
> The assessment uses your phone's motion sensors and is designed to be processed locally on your device. Raw sensor data and model inference remain on the device in the MVP.

Stored data may include:

- profile information,
- assessment timestamps,
- assessment metadata,
- derived movement metrics,
- Mobility Stability Scores,
- baseline statistics,
- raw sensor data temporarily retained for processing where required.

Controls:

- Delete all assessment data
- Delete all local app data
- Auto-delete raw session data after successful processing

Avoid absolute claims such as "100% private" unless verified technically.

---

# 27. Raw Sensor Data Lifecycle

Recommended lifecycle:

```text
Start assessment
      ↓
Collect temporary IMU session
      ↓
Validate
      ↓
Calculate physical metrics
      ↓
Run TCN
      ↓
Store approved derived result
      ↓
Delete temporary raw session data
```

Recommended default:

> **Delete temporary raw sensor data after successful processing.**

If processing fails, temporarily retain enough data to permit retry, subject to a defined cleanup rule.

---

# 28. Settings

## P04

Recommended MVP settings:

### Assessment

- assessment duration, only if multiple validated durations are implemented,
- assessment reminder.

### Accessibility

- larger text,
- reduced motion,
- high contrast,
- voice instructions,
- haptic feedback.

### Notifications

- reminders on/off,
- reminder frequency,
- preferred reminder time.

### Privacy

- auto-delete temporary sensor data,
- link to Privacy & Data.

Do not add unnecessary account or personalization settings.

---

# 29. Notifications

Notifications are optional.

Example:

> **Time for your mobility assessment.**

Avoid medical-alert language.

Do not send persistent warnings from a single abnormal-looking session.

---

# 30. Error States

## E01 — Sensors unavailable

> This device does not provide the sensors needed for the assessment.

Action:

> **Return home**

## E02 — Invalid session

> We couldn't get a reliable assessment from this session.

Possible causes:

- insufficient walking,
- excessive stationary periods,
- poor signal,
- unsuitable phone placement.

Action:

> **Try again**

## E03 — Processing error

> Your assessment was captured, but local analysis could not be completed.

Actions:

- Retry
- Save assessment for later
- Return home

Do not silently delete the captured session before the user has a recovery path.

## E04 — Storage error

> The assessment could not be saved on this device.

Provide retry and data-management guidance.

---

# 31. Local Data Model

## User

```text
user_id
first_name
last_name
preferred_name
date_of_birth / age
height
created_at
updated_at
```

## Assessment

```text
assessment_id
user_id
created_at
duration_seconds
recording_status       // rename to collection_status
processing_status
analysis_status
sensor_sample_count
sample_rate_hz
device_info
app_version
```

## GaitAnalysis

```text
assessment_id
cadence_steps_per_min
symmetry_index
estimated_speed_m_s
mobility_stability_score
model_version
inference_timestamp
```

Do not require stride length unless a validated implementation is added later.

## UserBaseline

```text
baseline_id
user_id
metric_name
mean
standard_deviation
sample_count
baseline_start_at
baseline_end_at
created_at
updated_at
```

For the primary Mobility Stability Score, persist:

```text
baseline_mean_mu
baseline_std_sigma
deviation_threshold_2sigma
z_score
is_current_session_deviation
consecutive_deviation_count
persistent_deviation_flag
```

These are longitudinal application values, not neural-network outputs.

## Settings

```text
user_id
notifications_enabled
reminder_frequency
reminder_time
auto_delete_raw_sensor_data
reduced_motion
large_text
voice_guidance
```

---

# 32. Technical Stack

## Android

**Kotlin + Jetpack Compose**

## Sensors

**Android SensorManager**

Used for:

- accelerometer,
- gyroscope,
- timestamped sensor samples.

## Local database

**Room**

Used for:

- profile,
- assessments,
- derived analysis,
- baseline,
- settings.

## ML runtime

**TensorFlow Lite**

The ML PRD specifies a quantized `.tflite` TCN and NNAPI delegation where available.

## Asynchronous processing

Use:

- Kotlin Coroutines,
- Flow,
- WorkManager where background processing is appropriate.

## Dependency injection

**Hilt** is recommended.

## Navigation

**Jetpack Navigation Compose**

## Architecture

Recommended:

```text
UI
 ↓
ViewModel
 ↓
Use Cases
 ↓
Repositories
 ↓
Sensor / Local Data / ML Adapter
```

---

# 33. Suggested Project Structure

```text
app/
├── core/
│   ├── database/
│   ├── sensors/
│   ├── storage/
│   ├── permissions/
│   ├── design/
│   └── utilities/
│
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── assessment/
│   ├── results/
│   ├── history/
│   ├── profile/
│   ├── privacy/
│   └── settings/
│
├── ml/
│   └── MlInferenceAdapter
│
└── navigation/
```

The ML adapter should expose a stable interface so the UI and repositories do not depend on TCN implementation details.

---

# 34. ML Adapter Responsibility

The adapter owns the orchestration between the Android application and the ML pipeline.

Conceptually:

```text
IMU session
   ↓
quality validation
   ↓
heuristic physical metrics
   ↓
1500 × 6 fixed window
   ↓
standard normalization
   ↓
TFLite TCN
   ↓
raw probability
   ↓
Mobility Stability Score
   ↓
hybrid result object
```

The raw probability remains internal.

The app consumes the final hybrid result.

---

# 35. Performance Requirements

## UI

- smooth navigation,
- responsive interactions,
- no obvious frame drops.

## Sensor collection

- reliable accelerometer/gyroscope sampling,
- correct timestamp handling,
- graceful handling of unavailable sensors.

## ML

- inference must occur on-device,
- use quantized TFLite model,
- use NNAPI where available if supported by the implementation,
- processing should not make the UI appear frozen.

## Battery

Do not run continuous high-frequency sensor collection unnecessarily.

For the hackathon MVP, sensor collection should be initiated for an assessment rather than running permanently in the background.

---

# 36. Offline Requirements

The core workflow must work without internet connectivity:

```text
Assessment
   ↓
Sensor collection
   ↓
Validation
   ↓
ML inference
   ↓
Result
   ↓
Room persistence
```

No cloud service should be required for:

- sensor collection,
- model inference,
- score calculation,
- baseline calculation,
- history viewing.

---

# 37. Security & Privacy Requirements

- Store sensitive records in app-private storage.
- Do not upload raw movement data.
- Do not log sensitive personal information.
- Do not log raw sensor sessions in release builds.
- Delete temporary raw sensor data after successful processing when enabled.
- Avoid plaintext storage of sensitive data where practical.
- Do not silently share assessment data.
- Explicitly explain any future export/share action.

---

# 38. Accessibility

The app should support older adults and users with mobility limitations.

Requirements:

- large tap targets,
- readable typography,
- strong contrast,
- plain language,
- voice instructions,
- reduced motion,
- clear error messages,
- screen-reader support where practical,
- no critical information conveyed through color alone.

During assessment:

> Make sure the walking area is clear before starting.

> Stop if you feel unsafe.

---

# 39. Visual Design Direction

Desired aesthetic:

> **Modern healthcare + calm technology**

Use:

- soft surfaces,
- clear hierarchy,
- restrained rounded components,
- readable typography,
- subtle movement-inspired graphics,
- baseline timelines,
- simple sensor/motion illustrations.

Avoid:

- hospital-management appearance,
- excessive neon,
- excessive 3D,
- intimidating medical warnings,
- disease imagery,
- fake precision.

The app should feel trustworthy before it feels futuristic.

---

# 40. Microcopy Guidelines

Prefer:

> Assessment

over:

> Diagnostic scan

Prefer:

> Movement analysis

over:

> Disease detection

Prefer:

> Change from your personal baseline

over:

> Abnormal result

Prefer:

> Persistent change

over:

> Disease progression

Prefer:

> Consider discussing persistent changes with a healthcare professional

over:

> You may have a neurological disorder

Never use disease names or diagnosis language in user-facing screens.

---

# 41. First-Time User Flow

```text
Launch
  ↓
Welcome
  ↓
How it works
  ↓
Privacy explanation
  ↓
Create profile
  ↓
Explain motion sensors
  ↓
Home
  ↓
Start assessment
  ↓
Pocket instructions
  ↓
Sensor readiness
  ↓
30-sec sensor collection
  ↓
Validation
  ↓
Local processing
  ↓
Results / baseline progress
```

---

# 42. Returning User Flow

```text
Launch
  ↓
Home
  ↓
View latest result / baseline
  ↓
Start assessment
  ↓
Pocket instructions
  ↓
Sensor readiness
  ↓
Sensor collection
  ↓
Validation
  ↓
Local processing
  ↓
Results
  ↓
History
```

---

# 43. Hackathon MVP Scope

## Must build

- Welcome screen.
- 3–4 explanation slides.
- Privacy explanation.
- Local profile setup.
- Motion-sensor explanation.
- Home empty state.
- Returning-user Home.
- Assessment introduction.
- Pocket/walking instructions.
- Sensor readiness check.
- Accelerometer + gyroscope collection.
- Approximately 30-second assessment session.
- Data quality validation.
- Local processing screen.
- ML adapter interface.
- TFLite inference integration.
- Mobility Stability Score.
- Cadence.
- Symmetry.
- Estimated walking speed.
- Baseline establishment.
- Deviation calculation.
- Persistent-change state.
- History.
- Assessment detail.
- Profile.
- Privacy/data controls.
- Settings.
- Delete data.
- Core error states.
- Room database.
- Local raw sensor lifecycle.

## Can build if time remains

- voice instructions,
- accessibility settings,
- reminder notifications,
- polished animations,
- richer score charts,
- exportable report,
- clinician dashboard.

## Do not prioritize

- camera/video pipeline,
- MediaPipe,
- cloud processing,
- cloud video storage,
- chatbot,
- social features,
- complex authentication,
- appointment booking,
- wearable integration,
- full website.

---

# 44. Definition of Done

A new user should be able to:

1. Launch the app.
2. Understand what the product does.
3. Understand that it is not a diagnostic tool.
4. Understand the local-processing model.
5. Create a profile.
6. Start a mobility assessment.
7. Put the phone in their pocket.
8. Complete approximately 30 seconds of walking data collection.
9. Pass sensor data through the quality gate.
10. Run the finalized local ML pipeline.
11. Receive a Mobility Stability Score.
12. View available physical movement metrics.
13. Save the result locally.
14. View the result in History.
15. Establish a personal baseline after the required initial valid walks.
16. Calculate deviation relative to that baseline.
17. Track consecutive deviations.
18. Expose Stable / Change Detected / Persistent Change state.
19. Delete stored assessment data.
20. Recover gracefully from sensor, validation, storage, and processing failures.
21. Perform the complete workflow without an internet connection.

---

# 45. Hackathon Demo Narrative

The strongest demo should follow one user's longitudinal journey.

### 1. Open app

> "Understand your mobility over time."

### 2. Explain privacy

> "Your movement analysis is designed to run locally on your device."

### 3. Create profile

Use a realistic demo profile.

### 4. Start assessment

Show the pocket instructions.

### 5. Collect movement

Show the phone collecting accelerometer + gyroscope data for approximately 30 seconds.

### 6. Validate

Show:

> "Checking assessment quality."

### 7. Analyze locally

Show:

> "Analyzing on your device."

### 8. Show result

Display:

- Mobility Stability Score,
- cadence,
- estimated speed,
- symmetry.

### 9. Show longitudinal history

Use preloaded demo assessments to demonstrate:

```text
Baseline established
       ↓
Normal session
       ↓
Change detected
       ↓
Persistent change
```

### 10. Explain the ML

Explain that the app uses:

```text
6-axis phone motion data
       ↓
quality gate
       ↓
physical metrics
       ↓
1D TCN
       ↓
Mobility Stability Score
       ↓
personal baseline
       ↓
longitudinal change detection
```

This demonstrates a complete software product around the model rather than a standalone classifier.

---

# 46. Product Architecture Summary

```text
                         USER
                           │
                           ▼
                    ┌──────────────┐
                    │ ANDROID APP  │
                    └──────┬───────┘
                           │
             ┌─────────────┼─────────────┐
             │             │             │
             ▼             ▼             ▼
          Profile      Assessment      History
                           │
                           ▼
                    SensorManager
                           │
                    Accelerometer
                       + Gyroscope
                           │
                           ▼
                     Quality Gate
                           │
                           ▼
                  Physical Metrics
                           │
                           ▼
                    1500 × 6 IMU
                           │
                           ▼
                      TFLite TCN
                           │
                           ▼
              Mobility Stability Score
                           │
                           ▼
                   Deviation Engine
                           │
                           ▼
                        Room DB
                           │
                           ▼
                  Longitudinal UI
```

---

# 47. Final Product Positioning

## Product category

**Personal functional mobility monitoring**

## Core innovation

The product does not attempt to diagnose a disease from a single gait recording.

Instead, it creates a repeatable, privacy-preserving workflow that turns everyday walking into longitudinal personal movement data.

## Core experience

> **Walk → Collect IMU → Validate → Analyze locally → Store → Compare over time**

## Core differentiator

> **Compare you with you.**

## Core privacy message

> **Your movement data is sensitive. Keep the analysis on your device.**

## Core hackathon pitch

> **Functional Decline Radar turns everyday walking into a longitudinal mobility signal. A smartphone uses its built-in accelerometer and gyroscope to capture a short walking session, validates and analyzes the movement locally with a lightweight TCN, and stores the resulting physical metrics and Mobility Stability Score in a personal history. By comparing future sessions with the user's own baseline, the app can surface persistent changes in mobility without requiring raw movement data to leave the device.**
