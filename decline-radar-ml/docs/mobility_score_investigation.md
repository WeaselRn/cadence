# Mobility Stability Score investigation

## Verdict

The low real-device score is most likely caused by an axis/orientation domain
shift, compounded by a narrow synthetic gait distribution. It is not explained
by the Android score formula or by TFLite quantization.

A normal synthetic walk scores 99 on the float32 model and 95 on the quantized
model in the generator's native positive-Z orientation. Rotating the identical
six-axis signal so gravity is positive Y produces mean scores of 5 on both
models; negative Y produces 4 on float32 and 5 on quantized. This reproduces the
reported 2–5 score range without changing gait dynamics.

## Evidence

### Model behavior

Diagnostics used 1,000 deterministic, known-good regular synthetic windows:

| Model | Mean `p_irregular` | `p_irregular` range | Mean score | Score range |
|---|---:|---:|---:|---:|
| Float32 TFLite | 0.008579 | 0.003657–0.015016 | 99.002 | 98–100 |
| Quantized TFLite | 0.050387 | 0.011719–0.148438 | 94.943 | 85–99 |

The two TFLite variants had no regular/irregular decision flips. Their mean
absolute probability difference was 0.041808 and maximum was 0.137878. The
quantized model shifts confidence but does not turn normal in-distribution
walking into a score of 2–5.

On the reconstructed 318-window validation set, float32 accuracy was 99.69%
(one synthetic false negative) and quantized accuracy was 100%. All 18 held-out
UCI HAR WALKING windows scored 100 in both variants. This validates only
synthetic data and clean waist-mounted UCI HAR data, not Android pocket data.

### Orientation stress test

The same regular synthetic windows were rotated as rigid six-axis signals:

| Gravity orientation | Float32 mean score | Quantized mean score |
|---|---:|---:|
| Positive Z (generator native) | 99.0 | 94.7 |
| Positive X (UCI HAR-like) | 100.0 | 100.0 |
| Positive Y | 5.0 | 5.0 |
| Negative X | 11.8 | 11.0 |
| Negative Y | 4.0 | 5.0 |
| Negative Z | 0.0 | 0.0 |

Across all 24 right-handed axis-aligned rotations, 12 produced a quantized mean
score at or below 10. The TCN consumes six independent device axes and has no
rotation-invariant representation or orientation augmentation.

### Six-channel training distribution

The regular synthetic generator has a fixed positive-Z gravity baseline, small
X/Y sinusoidal sway, and independent Gaussian gyro noise:

| Channel | Regular synthetic mean ± std | UCI HAR train mean ± std | Full train/contract mean ± std |
|---|---:|---:|---:|
| `accel_x` | 0.001714 ± 0.234615 | 9.760681 ± 2.256268 | 1.723782 ± 3.841080 |
| `accel_y` | 0.000056 ± 0.234504 | -1.818729 ± 1.868296 | -0.319766 ± 1.082653 |
| `accel_z` | 10.754519 ± 0.472694 | -0.610911 ± 2.078163 | 8.714895 ± 4.420521 |
| `gyro_x` | -0.000208 ± 0.199952 | -0.001677 ± 0.507276 | 0.000155 ± 0.577367 |
| `gyro_y` | 0.000137 ± 0.199961 | 0.000385 ± 0.557652 | -0.000272 ± 0.585530 |
| `gyro_z` | -0.000030 ± 0.200077 | -0.000617 ± 0.336768 | -0.000454 ± 0.555821 |

The exact training bundle contains 2,700 synthetic windows plus 576 jittered
copies of only 48 UCI HAR train anchors. Rebuilding it with seed 42 reproduces
every contract mean and standard deviation exactly. The regular class therefore
learns two dominant mounting orientations: synthetic positive Z and UCI HAR
positive X. It has no positive-Y, inverted, arbitrary-angle, phone-bias, or
realistic pocket-motion coverage. Synthetic gyro channels also lack
gait-correlated angular motion.

### Keras and conversion reproducibility

`scripts/convert_to_tflite.py` compares Keras, float32 TFLite, and quantized
TFLite on identical normalized windows and checks classification flips. Git
history records the quantized model as retrained on the fixed generator with
zero classification flips.

The original `data/models/gait_tcn.keras` and
`data/models/normalization_stats.npz` are excluded by `.gitignore` and are not
present in the repository. Direct Keras inference and an independent
Keras-to-current-TFLite comparison therefore cannot be reproduced from the
tracked artifacts. The float32 and quantized tracked models were compared
directly above; the missing source model is an artifact-reproducibility gap.

### Android contract audit

`ImuPreprocessor.kt` matches the tracked contract for shape `[1, 1500, 6]`,
50 Hz resampling, channel order, units, and all six normalization constants.
`TfliteMlInferenceAdapter.kt` uses float32 I/O and implements the contract score
formula exactly. The Android model asset is byte-identical to the ML project's
quantized model. No preprocessing defect was found.

The integration is nevertheless orientation-sensitive:

- `model_contract.json` says “Android Sensor Coordinate System (pocket carry)”
  but does not define or enforce a canonical phone orientation.
- The ML-only Python gatekeeper assumes gravity near positive Z.
- The Android quality gate performs no orientation check and its valid-session
  unit test uses gravity on positive Y—an orientation that the model scores 5.

## Recommended next changes

1. Export representative raw Android IMU windows, with phone placement and
   orientation labels, and run the added diagnostics before changing the score.
2. Choose one explicit orientation strategy: gravity-align both accelerometer
   and gyroscope before inference, or train with broad 3D rotation augmentation
   and real pocket data. Update the contract and model together.
3. Replace independent synthetic gyro noise with gait-coupled angular motion
   and add device bias, placement motion, cadence/amplitude diversity, sampling
   jitter, and sensor noise calibrated from phones.
4. Add real-phone subject/device/placement splits to validation. Do not use
   oversampled copies of the same 48 UCI windows as evidence of phone-domain
   generalization.
5. Preserve the float32/quantized parity checks, but archive the source Keras
   model, normalization statistics, training manifest, seed, and metrics with
   each released model.

## Files involved

- `src/synthetic/generator.py`
- `src/model/dataset_builder.py`
- `src/model/tcn.py`
- `src/real_data/uci_har_loader.py`
- `src/data_quality/gatekeeper.py`
- `scripts/train_tcn.py`
- `scripts/convert_to_tflite.py`
- `scripts/test_false_negatives.py`
- `data/models/model_contract.json`
- `data/models/gait_tcn_float32.tflite`
- `data/models/gait_tcn_quantized.tflite`
- `tests/test_model_diagnostics.py`
- `../app/src/main/java/com/cadence/gaitradar/ml/ImuPreprocessor.kt`
- `../app/src/main/java/com/cadence/gaitradar/ml/TfliteMlInferenceAdapter.kt`
- `../app/src/main/java/com/cadence/gaitradar/core/quality/ImuQualityGate.kt`
- `../app/src/test/java/com/cadence/gaitradar/core/quality/ImuQualityGateTest.kt`
