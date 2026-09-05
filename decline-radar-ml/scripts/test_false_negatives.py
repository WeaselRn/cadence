"""
Test script for evaluating False Negatives (FN) and False Positives (FP).

Evaluates the TCN model on:
1. Real UCI HAR test walking windows (Ground Truth = Regular / 0):
   Verifies that the model correctly predicts 'False' (Not Perturbed, p <= 0.5).
   Reports True Negatives (TN) and Specificity.
2. Injected gait perturbations on real UCI HAR test windows (Ground Truth = Perturbed / 1):
   Verifies detection of clinically relevant gait interruptions (Freezing of Gait,
   Post-Fall Immobility, and brief impact spikes). Reports False Negatives (FN) and Sensitivity.
3. Held-out synthetic test set (Ground Truth = 50% Regular, 50% Perturbed):
   Comprehensive confusion matrix across all 4 perturbation types (cadence_drift,
   asymmetry, jitter, freezing).
"""

import os
import sys

# Ensure project root is on the Python path
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, PROJECT_ROOT)

import numpy as np
import tensorflow as tf
from src.real_data.uci_har_loader import extract_regular_anchor_windows
from src.synthetic.generator import (
    GeneratorConfig,
    generate_dataset,
    generate_session,
    PERTURBATION_TYPES,
)

# Paths
MODEL_PATH = os.path.join(PROJECT_ROOT, "data", "models", "gait_tcn.keras")
CONTRACT_PATH = os.path.join(PROJECT_ROOT, "data", "models", "model_contract.json")
STATS_PATH = os.path.join(PROJECT_ROOT, "data", "models", "normalization_stats.npz")

# Candidate paths for UCI HAR Dataset (env var prioritized, followed by local data directory)
CANDIDATE_UCI_PATHS = [
    os.environ.get("UCI_HAR_ROOT", ""),
    os.path.join(PROJECT_ROOT, "data", "UCI HAR Dataset"),
    os.path.join(PROJECT_ROOT, "data", "UCI_HAR_Dataset"),
]
if os.path.exists(r"A:\UCI HAR Dataset"):
    CANDIDATE_UCI_PATHS.append(r"A:\UCI HAR Dataset")


def resolve_uci_har_root() -> str:
    for path in CANDIDATE_UCI_PATHS:
        if path and os.path.exists(path) and os.path.exists(os.path.join(path, "activity_labels.txt")):
            return path
    return None


def print_metrics_block(title: str, tn: int, fp: int, tp: int, fn: int):
    total = tn + fp + tp + fn
    accuracy = ((tp + tn) / total) * 100 if total > 0 else 0.0
    sensitivity = (tp / (tp + fn)) * 100 if (tp + fn) > 0 else 0.0
    specificity = (tn / (tn + fp)) * 100 if (tn + fp) > 0 else 0.0
    fn_rate = (fn / (tp + fn)) * 100 if (tp + fn) > 0 else 0.0
    fp_rate = (fp / (tn + fp)) * 100 if (tn + fp) > 0 else 0.0

    print(f"\n{'=' * 65}")
    print(f" {title}")
    print(f"{'=' * 65}")
    print(f"  Total Samples Evaluated : {total}")
    print(f"  True Negatives  (TN)    : {tn:<5} (Correctly predicted Regular / Not Perturbed)")
    print(f"  False Positives (FP)    : {fp:<5} (False Alarms on Regular Gait)")
    print(f"  True Positives  (TP)    : {tp:<5} (Correctly Detected Perturbations)")
    print(f"  False Negatives (FN)    : {fn:<5} (Missed Perturbations)")
    print(f"  -------------------------------------------------------------")
    print(f"  Accuracy                : {accuracy:.2f}%")
    print(f"  Specificity             : {specificity:.2f}% (Predicts Negative correctly)")
    print(f"  Sensitivity (Recall)    : {sensitivity:.2f}% (Catches Perturbations)")
    print(f"  False Negative Rate     : {fn_rate:.2f}% (Miss rate for Perturbations)")
    print(f"  False Positive Rate     : {fp_rate:.2f}% (False Alarm rate)")
    print(f"{'=' * 65}\n")


def main():
    if not os.path.exists(MODEL_PATH):
        raise FileNotFoundError(f"Trained model not found at {MODEL_PATH}. Run train_tcn.py first.")

    # 1. Load artifacts
    print("Loading model and normalization parameters...")
    model = tf.keras.models.load_model(MODEL_PATH)
    if os.path.exists(CONTRACT_PATH):
        import json
        with open(CONTRACT_PATH, "r", encoding="utf-8") as f:
            contract = json.load(f)
        mean = np.array(contract["input"]["channel_mean"], dtype=np.float32)
        std = np.array(contract["input"]["channel_std"], dtype=np.float32)
    elif os.path.exists(STATS_PATH):
        stats = np.load(STATS_PATH)
        mean, std = stats["channel_mean"], stats["channel_std"]
    else:
        raise FileNotFoundError("Neither model_contract.json nor normalization_stats.npz found.")

    # 2. Extract real held-out test windows
    uci_root = resolve_uci_har_root()
    if uci_root is None:
        print("WARNING: UCI HAR dataset directory not found. Skipping real data evaluation.")
        X_real = np.zeros((0, 1500, 6))
    else:
        print(f"Found UCI HAR dataset at: {uci_root}")
        cfg = GeneratorConfig()
        target_length = int(cfg.duration_seconds * cfg.sample_rate_hz)
        X_real, _ = extract_regular_anchor_windows(uci_root, split="test", target_length=target_length)
        print(f"Extracted {len(X_real)} real held-out test walking windows (30s @ 50Hz).")

    # ---------------------------------------------------------
    # TEST 1: Real Regular Walking (Testing True Negatives)
    # Ground Truth: Negative (Regular Gait / y=0)
    # ---------------------------------------------------------
    if len(X_real) > 0:
        X_real_norm = (X_real - mean) / std
        preds_real = model.predict(X_real_norm, verbose=0).flatten()

        tn = int(np.sum(preds_real <= 0.5))
        fp = int(np.sum(preds_real > 0.5))

        print("\n" + "#" * 65)
        print(" TEST 1: Real Held-Out Regular Walking (Ground Truth: REGULAR / 0)")
        print("#" * 65)
        print(f"Raw Probabilities: min={preds_real.min():.5f}, mean={preds_real.mean():.5f}, max={preds_real.max():.5f}")
        print(f"Predicted Regular (p <= 0.5) : {tn}/{len(preds_real)} ({tn / len(preds_real) * 100:.1f}%) -> True Negatives")
        print(f"Predicted Perturbed (p > 0.5) : {fp}/{len(preds_real)} ({fp / len(preds_real) * 100:.1f}%) -> False Positives")

        # ---------------------------------------------------------
        # TEST 2: Injected Perturbations on Real Walking Anchors
        # Ground Truth: Positive (Perturbed Gait / y=1)
        # ---------------------------------------------------------
        rng = np.random.default_rng(42)

        # 2a. Freezing of Gait (3 episodes of 1.5s - 2.5s sudden halt in walking)
        X_freeze = X_real.copy()
        for i in range(len(X_freeze)):
            for _ in range(3):
                dur = rng.integers(75, 125)
                start = rng.integers(100, 1500 - dur - 50)
                # Subject stops moving; sensors register static gravity plus slight resting noise
                X_freeze[i, start : start + dur, :3] = np.array([0.0, 0.0, 9.8]) + rng.normal(0, 0.05, (dur, 3))
                X_freeze[i, start : start + dur, 3:6] = rng.normal(0, 0.05, (dur, 3))

        preds_freeze = model.predict((X_freeze - mean) / std, verbose=0).flatten()
        tp_freeze = int(np.sum(preds_freeze > 0.5))
        fn_freeze = int(np.sum(preds_freeze <= 0.5))

        # 2b. Fall & Post-Fall Immobility (Impact spike followed by cessation of gait)
        X_fall = X_real.copy()
        for i in range(len(X_fall)):
            impact_idx = rng.integers(600, 900)
            X_fall[i, impact_idx : impact_idx + 25, :3] += 15.0  # 15 m/s^2 impact spike (~0.5s)
            X_fall[i, impact_idx + 25 :, :3] = np.array([0.0, 0.0, 9.8])  # immobile post-fall
            X_fall[i, impact_idx + 25 :, 3:6] = 0.0

        preds_fall = model.predict((X_fall - mean) / std, verbose=0).flatten()
        tp_fall = int(np.sum(preds_fall > 0.5))
        fn_fall = int(np.sum(preds_fall <= 0.5))

        # 2c. Brief Impact Spike Only (15 frames = 0.3s blip with NO subsequent gait change)
        X_spike = X_real.copy()
        X_spike[:, 750 : 765, :3] += 3.0  # 0.3s blip
        preds_spike = model.predict((X_spike - mean) / std, verbose=0).flatten()
        tp_spike = int(np.sum(preds_spike > 0.5))
        fn_spike = int(np.sum(preds_spike <= 0.5))

        print("\n" + "#" * 65)
        print(" TEST 2: Injected Perturbations on Real Gait (Ground Truth: PERTURBED / 1)")
        print("#" * 65)
        print(f"A) Freezing of Gait (Hesitation / Halts):")
        print(f"   Detected (TP) : {tp_freeze}/{len(X_real)} ({tp_freeze / len(X_real) * 100:.1f}%)")
        print(f"   Missed   (FN) : {fn_freeze}/{len(X_real)} ({fn_freeze / len(X_real) * 100:.1f}%)")

        print(f"\nB) Fall + Post-Fall Immobility:")
        print(f"   Detected (TP) : {tp_fall}/{len(X_real)} ({tp_fall / len(X_real) * 100:.1f}%)")
        print(f"   Missed   (FN) : {fn_fall}/{len(X_real)} ({fn_fall / len(X_real) * 100:.1f}%)")

        print(f"\nC) Brief Micro-Spike (15 frames / 0.3s blip only):")
        print(f"   Detected (TP) : {tp_spike}/{len(X_real)} ({tp_spike / len(X_real) * 100:.1f}%)")
        print(f"   Missed   (FN) : {fn_spike}/{len(X_real)} ({fn_spike / len(X_real) * 100:.1f}%)")
        print(f"   * Note: The TCN uses GlobalAveragePooling across 30 seconds (1500 timesteps).")
        print(f"     A 0.3s blip without gait stoppage only affects 1% of the window and is")
        print(f"     intentionally ignored as transient sensor noise rather than sustained decline.")

        # Combined Real Test Metrics (Clean + Freezing)
        print_metrics_block(
            "Real Gait Evaluation Summary (Clean Regular + Freezing Perturbations)",
            tn=tn,
            fp=fp,
            tp=tp_freeze,
            fn=fn_freeze,
        )

    # ---------------------------------------------------------
    # TEST 3: Synthetic Held-Out Dataset (All 4 Perturbation Types)
    # ---------------------------------------------------------
    print("\n" + "#" * 65)
    print(" TEST 3: Synthetic Benchmark Dataset (200 Held-Out Samples)")
    print("#" * 65)
    X_syn, y_syn_gen, meta_syn = generate_dataset(200, seed=999)
    y_syn_tcn = 1 - y_syn_gen  # 0 = regular, 1 = perturbed

    X_syn_norm = (X_syn - mean) / std
    preds_syn = model.predict(X_syn_norm, verbose=0).flatten()

    syn_pred_binary = (preds_syn > 0.5).astype(int)
    tn_syn = int(np.sum((y_syn_tcn == 0) & (syn_pred_binary == 0)))
    fp_syn = int(np.sum((y_syn_tcn == 0) & (syn_pred_binary == 1)))
    tp_syn = int(np.sum((y_syn_tcn == 1) & (syn_pred_binary == 1)))
    fn_syn = int(np.sum((y_syn_tcn == 1) & (syn_pred_binary == 0)))

    print_metrics_block(
        "Synthetic Test Set Performance",
        tn=tn_syn,
        fp=fp_syn,
        tp=tp_syn,
        fn=fn_syn,
    )

    # Breakdown per perturbation type in synthetic set
    print("Breakdown by Synthetic Perturbation Type:")
    for ptype in PERTURBATION_TYPES:
        indices = [
            i for i, m in enumerate(meta_syn)
            if any(p["type"] == ptype for p in m.get("perturbations_applied", []))
        ]
        if indices:
            sub_preds = preds_syn[indices]
            detected = np.sum(sub_preds > 0.5)
            missed = len(sub_preds) - detected
            print(f"  - {ptype:<15}: Detected {detected:>3}/{len(indices):<3} ({detected / len(indices) * 100:.1f}%), Missed (FN): {missed}")


if __name__ == "__main__":
    main()