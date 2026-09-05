PHASE 4 — DETERMINISTIC IMU DATA QUALITY GATE
Implement ONLY Phase 4 after Phase 3 is complete.
PRIMARY OBJECTIVE
Validate whether an ImuSession is reliable enough to enter ML preprocessing. The gate is local, deterministic, explainable, testable, and independent of TensorFlow Lite.
VALIDATION CONTRACT
ImuSession → ImuQualityGate.evaluate() → QualityResult → PASS or RETRY.
REQUIRED CHECKS
Check duration, minimum samples for both sensors, timestamp integrity, finite values, observed sampling stability, and basic signal sanity. Sampling is approximately 50 Hz; do not require exactly 50 Hz.
DURATION
Target approximately 30 seconds. Use centralized thresholds; very short sessions such as 5–10 seconds must fail. Do not require exactly 30.000 seconds.
SIX-AXIS COMPLETENESS
Both accelerometer and gyroscope streams are required. Reject sessions that cannot support ax, ay, az, gx, gy, gz.
IMPORTANT ML GUARDRAIL
If QualityResult.isValid is false, STOP. Do not calculate gait metrics, preprocess for ML, run TFLite, calculate Mobility Stability Score, or save a completed assessment.
OUTPUT
QualityResult exposes validity, failure reason, duration, sensor counts, observed rates, timestamp integrity, finite-value status, and signal sanity status. Keep thresholds centralized.
VERIFICATION
Unit-test valid, short, missing-sensor, low-sample, bad-timestamp, NaN/Infinity, constant/broken-signal, and realistic sampling-variation cases. Test valid/invalid flow on Pixel 9.
