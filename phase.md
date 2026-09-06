# Gait Functional Decline Radar — Post-Testing Improvements

## 1. Full Light Mode and Dark Mode Support

### Problem
The current app's color system appears to be designed primarily for light mode. When the device switches to dark mode, colors, backgrounds, cards, text, borders, and other UI elements should adapt appropriately.

### Required Changes
Update the application's entire color system so that **all screens support both Light and Dark themes**.

This should be implemented centrally through the Jetpack Compose Material theme rather than manually defining separate colors inside individual screens.

### Requirements
- Create/complete a dedicated light color scheme.
- Create/complete a dedicated dark color scheme.
- Ensure every screen uses theme colors rather than hardcoded colors.
- Update:
    - Backgrounds
    - Surface/card colors
    - Primary and secondary colors
    - Text colors
    - Button colors
    - Outlined button/border colors
    - Icons
    - Charts/graphs
    - Progress indicators
    - Status indicators
    - Navigation elements
    - Dialogs
    - Error/warning/success states
- Maintain sufficient contrast and readability in both themes.
- Ensure the app follows the user's Android system Light/Dark mode preference automatically.
- Avoid hardcoded colors that become unreadable when the theme changes.

### Acceptance Criteria
- Every screen is visually usable in Light Mode.
- Every screen is visually usable in Dark Mode.
- No white cards/backgrounds remain unintentionally visible in Dark Mode.
- No dark text becomes unreadable against dark surfaces.
- Buttons, icons, charts, and status indicators remain clearly visible in both modes.
- Switching the device between Light and Dark mode updates the app correctly.

---

# 2. Settings Screen Crash

### Problem
The application currently crashes immediately when the user opens the **Settings** screen.

This is a blocking functional issue because the Settings screen cannot currently be accessed.

### Required Changes
Investigate and fix the root cause of the Settings screen crash.

### Investigation Requirements
- Capture the Android/Logcat stack trace when opening Settings.
- Identify the exact exception and source location.
- Fix the underlying implementation rather than simply hiding or bypassing the Settings screen.
- Verify that all Settings dependencies, state, navigation arguments, and Compose components are initialized correctly.
- Verify that Settings still works after configuration changes and app restarts.

### Additional Settings Requirement
The existing Settings controls should eventually persist their state rather than existing only as temporary UI state.

Settings that should be persisted include:

- Larger text
- Reduced motion
- Voice instructions
- Haptic feedback
- Reminders
- Automatic data deletion

Use the appropriate persistent storage mechanism already present in the project rather than introducing unnecessary new infrastructure.

### Acceptance Criteria
- Settings opens without crashing.
- All Settings controls render correctly.
- Controls can be changed without crashing.
- Settings remain consistent after leaving and reopening the screen.
- Settings remain consistent after restarting the application.
- No regression is introduced to other screens.

---

# 3. Expand the About & Help Section

## Problem
The current **About & Help** section does not provide enough information about how the assessment actually works.

For a health-oriented application, users should be able to understand what the app is measuring, how the assessment works, what the result means, and what the limitations are.

## Required Changes

Expand the About & Help section with a detailed explanation of the assessment process.

Use expandable/collapsible sections (for example, dropdown/accordion-style components) so that the information does not overwhelm the main screen.

### Suggested Sections

#### 3.1 What Is the Gait Assessment?
Explain:
- What gait/mobility is.
- Why gait patterns can provide information about functional mobility.
- What the application is designed to assess.
- What the application does and does not claim to diagnose.

#### 3.2 How Does the Assessment Work?
Provide a detailed step-by-step explanation:

1. The user starts an assessment.
2. The phone's motion sensors collect movement data.
3. Accelerometer and gyroscope measurements are captured.
4. The collected sensor data is processed on the device.
5. The data is prepared into the format required by the machine-learning model.
6. The trained TensorFlow Lite model analyzes the movement pattern.
7. The application calculates the model-derived mobility score.
8. The result is compared with the user's personal baseline.
9. The application presents the current assessment and any meaningful deviation from the baseline.

#### 3.3 What Sensors Are Used?
Explain:
- Accelerometer
- Gyroscope
- What type of movement information each sensor contributes.
- That the assessment does not require camera/video recording.

#### 3.4 What Is the Machine-Learning Model?
Explain in user-friendly language:
- That the app uses a trained machine-learning model.
- That the model analyzes patterns in motion-sensor data.
- That inference happens on-device.
- What the model's output represents.
- The distinction between a model-derived score and a personalized baseline.

Avoid exposing unnecessary technical/debug information to normal users.

#### 3.5 What Is My Baseline?
Explain that the baseline represents the user's own typical mobility pattern based on previous valid assessments.

Explain why comparing a person against their own historical pattern can be useful instead of relying only on a population-level reference.

#### 3.6 How Should I Interpret My Result?
Explain:
- Current mobility/model score.
- Baseline comparison.
- Whether the current result is broadly consistent with the user's historical pattern.
- What a deviation means.
- That a single unusual assessment should not automatically be interpreted as functional decline.
- That repeated changes are more meaningful than one isolated result.

#### 3.7 What Can Affect an Assessment?
Explain factors such as:
- Phone placement
- Walking conditions
- Walking speed
- Surface/environment
- Interruptions during the assessment
- Sensor noise
- Temporary changes in movement

#### 3.8 Privacy
Explain:
- What sensor data is collected.
- Where processing occurs.
- Whether data leaves the device.
- How assessment history is stored.
- How users can delete their data.

### UI Requirements
- Use expandable sections.
- Keep the main About & Help page concise.
- Allow users to expand only the information they want.
- Use clear headings and short paragraphs.
- Maintain accessibility in both Light and Dark modes.
- Ensure expanded content is scrollable and readable.

---

# 4. Make the Home Screen More Informative

## Problem
The current Home/Main screen does not provide enough useful information.

At present, the main experience can feel too empty because it primarily communicates a generic status such as:

> Stable mobility pattern

The home screen should provide a more meaningful summary of the user's current mobility status and recent assessment history.

## Required Changes

Transform the Home screen into a useful **mobility overview/dashboard**.

### Suggested Home Screen Structure

#### Current Status
Display:
- Current mobility score.
- Current assessment status.
- Comparison against the user's baseline.
- A clear interpretation of whether the current result is consistent with the user's normal pattern.

Example concept:

**Current Mobility**
`82 / 100`

**Compared with your baseline**
`−4 points`

**Status**
`Within your usual range`

---

### Recent Assessment

Show the latest assessment with:

- Date/time
- Model-derived score
- Baseline score
- Difference from baseline
- Assessment quality/status

---

### Trend

If sufficient historical assessments exist, show a simple trend visualization.

For example:
- Recent mobility scores
- Baseline/reference level
- Direction of change

The visualization should remain simple and understandable rather than becoming an overly complex medical dashboard.

---

### Assessment Count

Show useful context such as:

- Number of completed assessments
- Number of assessments contributing to the baseline
- Whether the baseline is established

For example:

`5 assessments`

`Baseline established`

---

### First-Time User State

If the user has not completed enough assessments to establish a baseline, the Home screen should explain that clearly.

For example:

**Build your baseline**

Complete additional assessments to establish your personal mobility pattern.

Do not show misleading baseline comparisons before a valid baseline exists.

---

# 5. Improve Assessment History

## Problem
The current History section does not provide enough useful information about each recorded assessment.

The user should be able to understand what happened during each assessment and how the result relates both to the machine-learning model and their own historical baseline.

## Required Changes

Each assessment in History should expose meaningful result information.

### Assessment History Card

Each card should show at minimum:

- Assessment date/time
- Current mobility score
- Baseline comparison
- Change from baseline
- Overall interpretation/status

Example:

**September 6, 2026 — 10:42 AM**

**Mobility Score:** 82/100

**Personal Baseline:** 86/100

**Difference:** −4 points

**Status:** Within usual range

---

# 6. Add Assessment Detail Screen

Selecting an assessment from History should open a dedicated **Assessment Detail** screen.

The detail screen should provide a complete explanation of that assessment.

## Suggested Structure

### A. Assessment Overview

Display:

- Date and time
- Overall score
- Assessment validity/quality
- Overall interpretation

---

### B. Machine-Learning Model Result

Clearly distinguish the model result from the personalized baseline.

For example:

**Machine-Learning Model**

`Mobility Score: 82/100`

Explain that this is the score produced from the current assessment by the trained model.

If the model produces an underlying probability such as `p_irregular`, that raw technical value should **not** normally be exposed to the end user.

---

### C. Personal Baseline Comparison

Display:

**Your Baseline**

`86/100`

**Current Assessment**

`82/100`

**Difference**

`−4 points`

Then provide an understandable interpretation.

For example:

> Your current result is 4 points below your personal baseline and remains within your expected range.

---

### D. Historical Context

Where enough historical data exists, show:

- Recent assessment scores
- Personal baseline
- Current score
- Trend/direction
- Number of assessments used to establish the baseline

This allows the user to understand whether the current result is an isolated variation or part of a repeated pattern.

---

### E. Interpretation

The application should distinguish between:

1. **Consistent with baseline**
2. **Moderate deviation**
3. **Persistent deviation**
4. **Insufficient data for comparison**

The wording should avoid implying a medical diagnosis.

---

# 7. Clearly Separate Model Comparison From Baseline Comparison

This is an important product requirement.

Every assessment should conceptually have **two different comparisons**:

### Comparison 1 — Machine-Learning Model

> How does this assessment score according to the trained ML model?

This produces the model-derived mobility score.

### Comparison 2 — Personal Baseline

> How does this assessment compare with this user's normal historical mobility pattern?

This produces the personalized interpretation.

These should never be presented as if they are the same measurement.

### Example

```text
CURRENT ASSESSMENT

Mobility Score
82 / 100

────────────────────────

MODEL RESULT

The trained model assigned this
assessment a mobility score of 82/100.

────────────────────────

YOUR BASELINE

Personal baseline: 86/100

Difference: −4 points

Status:
Within your usual range