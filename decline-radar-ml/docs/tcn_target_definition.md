# TCN Training Target & Dataset Definition

## Target
Binary classification: does this 30-second (1500-frame) IMU window represent
a "regular / typical steady-state gait" pattern?

- Label 1 = regular gait
- Label 0 = perturbed / atypical gait

This is disease-agnostic by construction — the model never sees or predicts
any diagnosis. It only discriminates "typical walking dynamics" vs
"atypical walking dynamics," matching PRD section 5's sigmoid output
("raw movement pattern probability").

## Score translation (app-facing)
mobility_stability_score = round((1 - p_irregular) * 100)

This gives the continuous 0-100 score used by the app / deviation engine,
while the underlying TCN itself is trained as a binary classifier.

## Dataset — Phase A (synthetic, no real data yet)
Generated in pairs per sample:

- **Label 1 (regular):** steady cadence (90-120 steps/min), low
  stride-to-stride variability, symmetric left/right steps, consistent
  speed, realistic sensor noise only.
- **Label 0 (perturbed):** same base generator, with one or more of the
  following injected, severity randomized per sample:
  1. Cadence drift (random-walk instability in step frequency)
  2. Asymmetry (uneven amplitude/timing between left/right step halves)
  3. Jitter (elevated high-frequency noise, tremor-like)
  4. Freezing episodes (brief near-zero-motion segments spliced into
     an otherwise walking session)

## Dataset — Phase B (once real data is available)
Real sessions are added ONLY as additional label-1 ("regular") examples.
We cannot assume any real user's session represents "perturbed/atypical"
gait without a diagnosis, which we don't have and PRD forbids using.
This sim-to-real anchoring lets the model learn real sensor noise
characteristics while the "irregular" class stays synthetic.

## Impact on existing code
- Data Quality Gatekeeper (src/data_quality/gatekeeper.py): NO CHANGES.
  It only judges session validity, independent of gait-quality labels.
- Synthetic generator (upcoming, Phase 4): must emit (raw_session, label)
  pairs per this scheme.
- TCN training (upcoming, Phase 5): trains with binary cross-entropy
  against these labels.