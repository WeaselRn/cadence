The current GitHub ingest already contains app/src/main/assets/models/gait_tcn_quantized.tflite and app/src/main/assets/models/model_contract.json, and app/build.gradle.kts already includes TensorFlow Lite 2.17.0. Phase 6 must therefore treat artifact ingestion as an already-completed prerequisite and focus on runtime inspection, exact preprocessing, TFLite adapter implementation, inference, output semantics, and tests. Do not create a duplicate model file or add a second TFLite integration path.
GITHUB INGEST NOTE — MODEL ARTIFACT ALREADY PRESENT
PHASE 6 — TRAINED TFLITE TCN INGESTION, PREPROCESSING & INFERENCE
Implement ONLY Phase 6 after Phases 3–5 are complete. This is the critical model-integration phase.
PRIMARY OBJECTIVE
Ingest the trained TFLite model from the ML Git repository, convert the real mobile ImuSession into the exact model contract, execute local inference, and return a structured prediction. Do not retrain or modify the trained model in Android.
MODEL ARTIFACT INGEST
Use the committed gait_tcn_quantized.tflite artifact from the ML repository/Git branch. Copy/import that exact artifact into app/src/main/assets/models/ (or the project-equivalent model assets directory). Do not replace it with a newly generated model. Keep model_contract.json/version information available to the integration code.
MODEL CONTRACT — SOURCE OF TRUTH
Model gait_tcn, version 1.0.0. TCN with causal blocks/dilations 1, 2, 4. Input Float32 [1,1500,6]. Output Float32 [1,1]. Target 50 Hz over 30 seconds. Channels: 0 accel_x, 1 accel_y, 2 accel_z, 3 gyro_x, 4 gyro_y, 5 gyro_z.
QUANTIZATION NOTE
The artifact is internally quantized/INT8, while its Android input/output tensors are Float32. Feed normalized Float32 values. Do not manually int8-quantize unless the inspected TFLite metadata explicitly requires it.
REQUIRED RUNTIME INSPECTION
At first integration inspect the actual TFLite input/output tensor shapes and datatypes. Fail safely if they do not match the committed contract. Never guess tensor indices or output semantics.
INPUT PIPELINE
Use the real mobile sensor session, not Phase 5 metrics: ImuSession → timestamp-aware synchronization/resampling → exactly 1500 time steps → [ax, ay, az, gx, gy, gz] → per-channel normalization → Float32 [1,1500,6] → TFLite.
RESAMPLING
Construct a deterministic 50 Hz time grid using retained timestamps and interpolation/nearest-valid samples. Do not simply truncate/pad by array index and do not invent long missing segments. Fail preprocessing if a reliable fixed window cannot be formed.
NORMALIZATION — MUST MATCH TRAINING
Use the exact training statistics from model_contract.json. accel_x μ=1.7237821340560913 σ=3.8410803079605103; accel_y μ=-0.3197662681341171 σ=1.0826526403427124; accel_z μ=8.71489520072937 σ=4.420521068572998; gyro_x μ=0.0001550798441166679 σ=0.5773674726486206; gyro_y μ=-0.00027176467701792717 σ=0.5855300307273865; gyro_z μ=-0.00045357465520501137 σ=0.5558213353157043. Formula: x_norm=(x-μ)/σ. Never recompute these statistics from the current user session.
CRITICAL DISTINCTION
Do not gravity-remove, magnitude-transform, rotate, or otherwise alter the six channels for the ML path unless the supplied model contract explicitly requires it. Phase 5 transformations are for physical metrics only.
ML ABSTRACTION
Keep TFLite behind MlInferenceAdapter/GaitMlModel. UI, ViewModel, repositories, Room, and baseline code must not import TFLite classes.
INFERENCE
Run preprocessing and inference on a background dispatcher. Reuse buffers where practical. Return MlPrediction with raw model output, model version, input shape, and inference time. Release Interpreter resources correctly.
OUTPUT SEMANTICS — DO NOT GET THIS BACKWARDS
The trained sigmoid output is p_irregular: probability of perturbed/atypical gait. Training flips the generator target, so model output 1 means perturbed and 0 means regular. The frontend score is therefore not the raw probability.
SCORE TRANSLATION
Mobility Stability Score = round((1.0 - p_irregular) × 100), clamped to 0–100. Higher means the movement pattern is more consistent with regular movement. Never label it disease probability, disease severity, health percentage, or diagnosis.
ERROR HANDLING
Handle missing/malformed model, unsupported tensor shape/datatype, invalid input size, preprocessing failure, inference exception, and invalid/non-finite output as structured failures. Never crash or show stack traces.
DEVELOPMENT DIAGNOSTICS
During development provide a debug-only ML Analysis screen showing model version, input shape 1500 × 6, inference time, and raw p_irregular. Do not present it as medical interpretation.
UNIT TESTS
Test channel order, timestamp-aware resampling, exact 1500 × 6 output, exact normalization constants, determinism, model loading, tensor metadata, finite output, and score translation. Add a golden/reference prediction test if the ML repository provides one.
VERIFICATION
On Pixel 9: real collection → quality PASS → 1500 × 6 → normalization → committed TFLite → inference → p_irregular → Mobility Stability Score. Verify failed quality sessions never call the model and inference never runs on the UI thread.
