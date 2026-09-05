"""
Trains the 1D TCN on combined synthetic + real (UCI HAR anchored) data.

Usage: adjust UCI_HAR_ROOT below (or set to None to train on synthetic only),
then run:
    python scripts/train_tcn.py

Outputs:
    data/models/gait_tcn.keras           -- trained Keras model
    data/models/normalization_stats.npz  -- channel_mean, channel_std
"""

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import numpy as np
import tensorflow as tf
from src.model.dataset_builder import build_training_bundle
from src.model.tcn import build_tcn, compile_model

# Candidate paths for UCI HAR Dataset (env var prioritized, followed by local data directory)
CANDIDATE_UCI_PATHS = [
    os.environ.get("UCI_HAR_ROOT", ""),
    os.path.join(os.path.dirname(__file__), "..", "data", "UCI HAR Dataset"),
    os.path.join(os.path.dirname(__file__), "..", "data", "UCI_HAR_Dataset"),
]
if os.path.exists(r"A:\UCI HAR Dataset"):
    CANDIDATE_UCI_PATHS.append(r"A:\UCI HAR Dataset")


def resolve_uci_har_root() -> str:
    for path in CANDIDATE_UCI_PATHS:
        if path and os.path.exists(path) and os.path.exists(os.path.join(path, "activity_labels.txt")):
            return path
    return None


N_SYNTHETIC = 3000
OVERSAMPLE_FACTOR = 12
VAL_SPLIT = 0.1
EPOCHS = 40
BATCH_SIZE = 32
SEED = 42

MODEL_OUT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "gait_tcn.keras")
CONTRACT_OUT_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "model_contract.json")
NORM_STATS_PATH = os.path.join(os.path.dirname(__file__), "..", "data", "models", "normalization_stats.npz")


def main():
    uci_root = resolve_uci_har_root()
    if uci_root is None:
        print("INFO: UCI HAR Dataset not found -- training on synthetic data only.")
    else:
        print(f"Found UCI HAR Dataset at: {uci_root}")

    print("Building dataset bundle...")
    bundle = build_training_bundle(
        n_synthetic=N_SYNTHETIC,
        uci_har_root=uci_root,
        oversample_factor=OVERSAMPLE_FACTOR,
        val_split=VAL_SPLIT,
        seed=SEED,
    )

    print(f"X_train: {bundle.X_train.shape}, label balance (1=perturbed): {np.bincount(bundle.y_train)}")
    print(f"X_val:   {bundle.X_val.shape}, label balance: {np.bincount(bundle.y_val)}")
    print(f"Real anchors -- train (raw, pre-oversample): {bundle.n_real_train_raw}, val: {bundle.n_real_val}")

    model = compile_model(build_tcn())

    callbacks = [
        tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=6, restore_best_weights=True),
        tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=3),
    ]

    history = model.fit(
        bundle.X_train, bundle.y_train,
        validation_data=(bundle.X_val, bundle.y_val),
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        callbacks=callbacks,
        verbose=2,
    )

    print("\n--- Overall validation metrics ---")
    overall_loss, overall_acc, overall_auc = model.evaluate(bundle.X_val, bundle.y_val, verbose=0)
    print(f"loss={overall_loss:.4f} acc={overall_acc:.4f} auc={overall_auc:.4f}")

    if bundle.n_real_val > 0:
        # Real-val samples are the LAST n_real_val rows of X_val/y_val
        # (see dataset_builder.py: synthetic val pieces are concatenated first).
        real_start = len(bundle.X_val) - bundle.n_real_val
        X_real_val = bundle.X_val[real_start:]
        y_real_val = bundle.y_val[real_start:]
        print(f"\n--- Real-only validation metrics (n={bundle.n_real_val}, held-out UCI HAR subjects) ---")
        real_loss, real_acc, real_auc = model.evaluate(X_real_val, y_real_val, verbose=0)
        print(f"loss={real_loss:.4f} acc={real_acc:.4f} auc={real_auc:.4f}")
        print("NOTE: all real-val labels are 0 (regular) after the label flip, so 'accuracy' here")
        print("      just measures how often the model correctly says 'not perturbed' on real data --")
        print("      it says nothing about catching real perturbed cases, since we have none labeled.")
    else:
        print("\nNo real validation samples available -- generalization to real data is UNTESTED.")

    os.makedirs(os.path.dirname(MODEL_OUT_PATH), exist_ok=True)
    model.save(MODEL_OUT_PATH)
    np.savez(NORM_STATS_PATH, channel_mean=bundle.channel_mean, channel_std=bundle.channel_std)
    
    # Export human-readable and git-trackable model contract
    import json
    channel_names = ["accel_x", "accel_y", "accel_z", "gyro_x", "gyro_y", "gyro_z"]
    channel_units = ["m/s^2", "m/s^2", "m/s^2", "rad/s", "rad/s", "rad/s"]
    contract = {
        "model_name": "gait_tcn",
        "version": "1.0.0",
        "architecture": "1D Temporal Convolutional Network (3 causal blocks, dilations 1, 2, 4)",
        "parameters": {
            "total_params": int(model.count_params()),
            "quantization_type": "INT8"
        },
        "input": {
            "tensor_shape": [1, 1500, 6],
            "dtype": "float32",
            "sample_rate_hz": 50.0,
            "window_duration_seconds": 30.0,
            "coordinate_system": "Android Sensor Coordinate System (pocket carry)",
            "channels": [
                {
                    "index": i,
                    "name": channel_names[i],
                    "unit": channel_units[i],
                    "mean": float(bundle.channel_mean[i]),
                    "std": float(bundle.channel_std[i])
                }
                for i in range(6)
            ],
            "channel_mean": [float(m) for m in bundle.channel_mean],
            "channel_std": [float(s) for s in bundle.channel_std]
        },
        "output": {
            "tensor_shape": [1, 1],
            "dtype": "float32",
            "name": "p_irregular",
            "description": "Continuous probability in [0.0, 1.0] representing likelihood of atypical/perturbed gait dynamics"
        },
        "score_translation": {
            "formula": "mobility_stability_score = round((1.0 - p_irregular) * 100)",
            "range": [0, 100],
            "output_dtype": "int"
        }
    }
    with open(CONTRACT_OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(contract, f, indent=2)

    print(f"\nSaved model to {MODEL_OUT_PATH}")
    print(f"Saved normalization stats to {NORM_STATS_PATH}")
    print(f"Exported model contract to {CONTRACT_OUT_PATH}")


if __name__ == "__main__":
    main()