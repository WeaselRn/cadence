"""
Converts the trained Keras model to TFLite in two forms:

1. gait_tcn_quantized.tflite -- full integer (int8) quantization of
   weights + activations via a representative dataset (PRD section 2:
   "a quantized .tflite model"). Input/output tensors are kept float32
   (not full int8 I/O) so the Android side can feed normalized floats
   directly without manually re-scaling to int8 -- simpler integration,
   still gets most of the size/speed benefit of quantization.

2. gait_tcn_float32.tflite -- plain float32 TFLite conversion, no
   quantization. This is the fallback mentioned during planning: if
   quantization debugging costs too much time, this file runs on the
   same Android TFLite Interpreter API with no calibration step needed.
   Larger and slightly slower, but functionally equivalent.

Also runs a sanity check: compares each converted model's output against
the original Keras model on a batch of held-out-style samples, to catch
quantization silently destroying accuracy (a common failure mode).
"""

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

import numpy as np
import tensorflow as tf
from synthetic.generator import generate_dataset, GeneratorConfig

MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "gait_tcn.keras")
NORM_STATS_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "normalization_stats.npz")
QUANTIZED_OUT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "gait_tcn_quantized.tflite")
FLOAT32_OUT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "gait_tcn_float32.tflite")

N_CALIBRATION_SAMPLES = 200
N_CHECK_SAMPLES = 50


def make_representative_dataset_fn(channel_mean, channel_std, sequence_length):
    cfg = GeneratorConfig(duration_seconds=sequence_length / 50.0)  # assumes 50Hz, matches training

    def representative_dataset():
        X, _, _ = generate_dataset(N_CALIBRATION_SAMPLES, cfg=cfg, seed=999)
        X_norm = ((X - channel_mean) / channel_std).astype(np.float32)
        for i in range(len(X_norm)):
            yield [X_norm[i:i+1]]

    return representative_dataset


def convert_float32(model) -> bytes:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    return converter.convert()


def convert_quantized(model, channel_mean, channel_std, sequence_length) -> bytes:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = make_representative_dataset_fn(
        channel_mean, channel_std, sequence_length
    )
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
        tf.lite.OpsSet.TFLITE_BUILTINS,  # fallback for any op int8 can't cover
    ]
    # Input/output left as float32 (default) -- see module docstring.
    return converter.convert()


def run_interpreter(tflite_bytes: bytes, X: np.ndarray) -> np.ndarray:
    interpreter = tf.lite.Interpreter(model_content=tflite_bytes)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    outputs = np.zeros((len(X), 1), dtype=np.float32)
    for i in range(len(X)):
        interpreter.set_tensor(input_details["index"], X[i:i+1].astype(input_details["dtype"]))
        interpreter.invoke()
        outputs[i] = interpreter.get_tensor(output_details["index"])
    return outputs


def main():
    model = tf.keras.models.load_model(MODEL_PATH)
    stats = np.load(NORM_STATS_PATH)
    channel_mean, channel_std = stats["channel_mean"], stats["channel_std"]
    sequence_length = model.input_shape[1]

    print("Converting to float32 TFLite (fallback)...")
    float32_bytes = convert_float32(model)
    with open(FLOAT32_OUT_PATH, "wb") as f:
        f.write(float32_bytes)

    print("Converting to quantized (int8) TFLite...")
    quantized_bytes = convert_quantized(model, channel_mean, channel_std, sequence_length)
    with open(QUANTIZED_OUT_PATH, "wb") as f:
        f.write(quantized_bytes)

    float32_size_kb = len(float32_bytes) / 1024
    quantized_size_kb = len(quantized_bytes) / 1024
    print(f"\nfloat32 size:   {float32_size_kb:.1f} KB")
    print(f"quantized size: {quantized_size_kb:.1f} KB  ({quantized_size_kb/float32_size_kb*100:.0f}% of float32)")

    # --- Sanity check: outputs should closely match the original Keras model ---
    cfg = GeneratorConfig(duration_seconds=sequence_length / 50.0)
    X_check, _, _ = generate_dataset(N_CHECK_SAMPLES, cfg=cfg, seed=2024)
    X_check_norm = ((X_check - channel_mean) / channel_std).astype(np.float32)

    keras_out = model.predict(X_check_norm, verbose=0)
    float32_out = run_interpreter(float32_bytes, X_check_norm)
    quantized_out = run_interpreter(quantized_bytes, X_check_norm)

    float32_max_diff = np.max(np.abs(keras_out - float32_out))
    quantized_max_diff = np.max(np.abs(keras_out - quantized_out))
    quantized_mean_diff = np.mean(np.abs(keras_out - quantized_out))

    # Decision-level agreement matters more than raw value diff: a large diff
    # far from the 0.5 threshold is harmless; a small diff near 0.5 can flip
    # the classification. Check both.
    keras_decision = (keras_out >= 0.5).astype(int)
    quantized_decision = (quantized_out >= 0.5).astype(int)
    n_flipped = int(np.sum(keras_decision != quantized_decision))
    flipped_pct = n_flipped / len(keras_out) * 100

    print(f"\n--- Output agreement vs. original Keras model (n={N_CHECK_SAMPLES}) ---")
    print(f"float32 TFLite   max diff: {float32_max_diff:.6f}  (should be ~0)")
    print(f"quantized TFLite max diff: {quantized_max_diff:.4f}  mean diff: {quantized_mean_diff:.4f}")
    print(f"quantized TFLite classification flips: {n_flipped}/{len(keras_out)} ({flipped_pct:.1f}%)")

    if n_flipped > 0:
        print("\nWARNING: quantization flipped the classification on at least one sample. "
              "This is the metric that actually matters for the deviation engine -- "
              "raw output diff alone was not a sufficient check.")
    elif quantized_max_diff > 0.15:
        print("\nWARNING: quantized model diverges notably from the original -- "
              "consider more calibration samples or check for outlier activations.")
    else:
        print("\nQuantized model output stays close to the original and no decisions flipped -- looks safe to use.")


if __name__ == "__main__":
    main()