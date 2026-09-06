import json
import os
import sys

import numpy as np
import tensorflow as tf

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from src.synthetic.generator import generate_dataset


PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MODEL_DIR = os.path.join(PROJECT_ROOT, "data", "models")
CONTRACT_PATH = os.path.join(MODEL_DIR, "model_contract.json")
FLOAT_MODEL_PATH = os.path.join(MODEL_DIR, "gait_tcn_float32.tflite")
QUANTIZED_MODEL_PATH = os.path.join(MODEL_DIR, "gait_tcn_quantized.tflite")
KERAS_MODEL_PATH = os.path.join(MODEL_DIR, "gait_tcn.keras")
CHANNEL_NAMES = (
    "accel_x",
    "accel_y",
    "accel_z",
    "gyro_x",
    "gyro_y",
    "gyro_z",
)


def _load_contract():
    with open(CONTRACT_PATH) as contract_file:
        return json.load(contract_file)


def _tflite_predict(model_path, samples):
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    input_index = interpreter.get_input_details()[0]["index"]
    output_index = interpreter.get_output_details()[0]["index"]

    predictions = []
    for sample in samples.astype(np.float32):
        interpreter.set_tensor(input_index, sample[np.newaxis, ...])
        interpreter.invoke()
        predictions.append(float(interpreter.get_tensor(output_index)[0, 0]))
    return np.asarray(predictions)


def _normalize(samples):
    contract = _load_contract()
    mean = np.asarray(contract["input"]["channel_mean"])
    std = np.asarray(contract["input"]["channel_std"])
    return ((samples - mean) / std).astype(np.float32)


def _score(probabilities):
    return np.rint((1.0 - probabilities) * 100.0).astype(int)


def _summary(values):
    quantiles = np.quantile(values, [0.0, 0.05, 0.5, 0.95, 1.0])
    return (
        f"mean={values.mean():.6f}, "
        f"min/p05/p50/p95/max="
        f"{quantiles[0]:.6f}/{quantiles[1]:.6f}/{quantiles[2]:.6f}/"
        f"{quantiles[3]:.6f}/{quantiles[4]:.6f}"
    )


def _rotate(samples, rotation):
    rotated = samples.copy()
    rotated[:, :, :3] = samples[:, :, :3] @ rotation.T
    rotated[:, :, 3:] = samples[:, :, 3:] @ rotation.T
    return rotated


def test_regular_synthetic_channel_and_prediction_distributions():
    samples, _, _ = generate_dataset(1_000, seed=20260906, balance=1.0)

    for index, name in enumerate(CHANNEL_NAMES):
        channel = samples[:, :, index]
        print(f"regular synthetic {name}: {_summary(channel)}")

    normalized = _normalize(samples)
    float_predictions = _tflite_predict(FLOAT_MODEL_PATH, normalized)
    quantized_predictions = _tflite_predict(QUANTIZED_MODEL_PATH, normalized)
    float_scores = _score(float_predictions)
    quantized_scores = _score(quantized_predictions)

    print(f"float32 p_irregular: {_summary(float_predictions)}")
    print(f"float32 score: {_summary(float_scores)}")
    print(f"quantized p_irregular: {_summary(quantized_predictions)}")
    print(f"quantized score: {_summary(quantized_scores)}")
    print(f"float32/quantized absolute difference: {_summary(np.abs(float_predictions - quantized_predictions))}")

    if os.path.exists(KERAS_MODEL_PATH):
        keras_model = tf.keras.models.load_model(KERAS_MODEL_PATH)
        keras_predictions = keras_model.predict(normalized, verbose=0).reshape(-1)
        print(f"Keras p_irregular: {_summary(keras_predictions)}")
        print(f"Keras/float32 absolute difference: {_summary(np.abs(keras_predictions - float_predictions))}")
        print(f"Keras/quantized absolute difference: {_summary(np.abs(keras_predictions - quantized_predictions))}")
    else:
        print("Keras diagnostic unavailable: data/models/gait_tcn.keras is not tracked.")

    assert float_scores.mean() >= 95
    assert quantized_scores.mean() >= 90
    assert not np.any((float_predictions >= 0.5) != (quantized_predictions >= 0.5))


def test_regular_synthetic_orientation_diagnostic():
    samples, _, _ = generate_dataset(200, seed=77, balance=1.0)
    rotations = {
        "z_up": np.eye(3),
        "x_up": np.array([[0, 0, 1], [0, 1, 0], [-1, 0, 0]]),
        "y_up": np.array([[1, 0, 0], [0, 0, 1], [0, -1, 0]]),
        "x_down": np.array([[0, 0, -1], [0, 1, 0], [1, 0, 0]]),
        "y_down": np.array([[1, 0, 0], [0, 0, -1], [0, 1, 0]]),
        "z_down": np.array([[1, 0, 0], [0, -1, 0], [0, 0, -1]]),
    }

    mean_scores = {}
    for name, rotation in rotations.items():
        normalized = _normalize(_rotate(samples, rotation))
        float_predictions = _tflite_predict(FLOAT_MODEL_PATH, normalized)
        quantized_predictions = _tflite_predict(QUANTIZED_MODEL_PATH, normalized)
        float_scores = _score(float_predictions)
        quantized_scores = _score(quantized_predictions)
        mean_scores[name] = (float_scores.mean(), quantized_scores.mean())
        print(
            f"{name}: float32 p_irregular {_summary(float_predictions)}, "
            f"score mean={float_scores.mean():.2f}; "
            f"quantized p_irregular {_summary(quantized_predictions)}, "
            f"score mean={quantized_scores.mean():.2f}"
        )

    assert mean_scores["z_up"][0] >= 95
    assert mean_scores["z_up"][1] >= 90
    assert mean_scores["y_up"][0] <= 10
    assert mean_scores["y_up"][1] <= 10
