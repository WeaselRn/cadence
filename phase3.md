PHASE 5 — PHYSICAL GAIT METRICS ENGINE
Implement ONLY Phase 5 after Phase 4 is complete.
PRIMARY OBJECTIVE
Extract deterministic physical gait metrics from a quality-passed ImuSession. This engine is separate from the ML model input path.
PIPELINE
ImuSession → Quality PASS → GaitMetricsEngine → GaitMetricsResult.
METRICS
Calculate available signal-derived metrics such as step count, cadence, mean step interval, step-time variability, movement regularity, acceleration variability, gyroscope variability, and valid PRD-defined symmetry or estimated walking speed when supported.
ORIENTATION ROBUSTNESS
Where practical, use magnitude-based representations for physical gait features rather than assuming a fixed phone axis. Keep pocket placement consistent.
NO ML INPUT COUPLING
Do NOT feed GaitMetricsResult into the TCN. The model input is the raw six-axis sequence after Phase 6 resampling and normalization.
NO FABRICATION
If a metric cannot be calculated reliably, return unavailable/null. Never insert placeholder values or zeros.
PERFORMANCE
Run computation off the Compose main thread using the existing coroutine architecture.
VERIFICATION
Test normal, slower, faster walking and insufficient events. Verify finite outputs, plausible cadence, no UI freeze, and no execution for failed quality sessions.
