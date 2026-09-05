"""
Combines synthetic data (both classes) with real UCI HAR WALKING anchors
(label-1 only, per docs/tcn_target_definition.md Phase B), applies
normalization (PRD section 3: zero-mean, unit-variance per channel), and
flips labels to the TCN's own target direction (see src/model/tcn.py
docstring: model learns P(perturbed), generator's label 1 = regular).

Real UCI HAR train-split anchors go into our training set (oversampled,
since there are only a few dozen). Real UCI HAR test-split anchors go
into our validation set, UNCHANGED and un-oversampled, specifically so
we get an honest read on real-data generalization. UCI HAR's own
train/test split is by subject (no subject appears in both), so there
is no leakage between our train and validation sets.
"""

from dataclasses import dataclass
import numpy as np

from src.synthetic.generator import generate_dataset, GeneratorConfig
from src.real_data.uci_har_loader import extract_regular_anchor_windows


@dataclass
class DatasetBundle:
    X_train: np.ndarray
    y_train: np.ndarray  # TCN-direction labels (1 = perturbed)
    X_val: np.ndarray
    y_val: np.ndarray
    channel_mean: np.ndarray
    channel_std: np.ndarray
    n_real_train_raw: int
    n_real_val: int


def _oversample_with_jitter(X: np.ndarray, factor: int, jitter_std: float, rng: np.random.Generator):
    """Repeats each real sample `factor` times, adding small distinct noise per copy."""
    if len(X) == 0 or factor <= 1:
        return X
    copies = [X]
    for _ in range(factor - 1):
        noise = rng.normal(0, jitter_std, X.shape)
        copies.append(X + noise)
    return np.concatenate(copies, axis=0)


def build_training_bundle(
    n_synthetic: int = 3000,
    uci_har_root: str = None,
    oversample_factor: int = 12,
    jitter_std: float = 0.05,
    val_split: float = 0.1,
    seed: int = 42,
) -> DatasetBundle:
    rng = np.random.default_rng(seed)
    cfg = GeneratorConfig()

    # 1. Synthetic data (both classes), split train/val
    X_syn, y_syn, _ = generate_dataset(n_synthetic, cfg=cfg, seed=seed)
    n_val_syn = int(round(n_synthetic * val_split))
    idx = rng.permutation(n_synthetic)
    val_idx, train_idx = idx[:n_val_syn], idx[n_val_syn:]
    X_syn_train, y_syn_train = X_syn[train_idx], y_syn[train_idx]
    X_syn_val, y_syn_val = X_syn[val_idx], y_syn[val_idx]

    # 2. Real UCI HAR anchors (label 1 = regular, in generator's convention)
    n_real_train_raw = 0
    n_real_val = 0
    real_train_pieces = []
    real_val_pieces = []

    if uci_har_root is not None:
        X_real_train, _ = extract_regular_anchor_windows(
            uci_har_root, split="train", target_length=int(cfg.duration_seconds * cfg.sample_rate_hz)
        )
        X_real_val, _ = extract_regular_anchor_windows(
            uci_har_root, split="test", target_length=int(cfg.duration_seconds * cfg.sample_rate_hz)
        )
        n_real_train_raw = len(X_real_train)
        n_real_val = len(X_real_val)

        X_real_train_over = _oversample_with_jitter(X_real_train, oversample_factor, jitter_std, rng)
        if len(X_real_train_over) > 0:
            real_train_pieces.append((X_real_train_over, np.ones(len(X_real_train_over), dtype=int)))
        if len(X_real_val) > 0:
            real_val_pieces.append((X_real_val, np.ones(len(X_real_val), dtype=int)))

    # 3. Combine synthetic + real (still in generator label convention: 1=regular, 0=perturbed)
    X_train_pieces = [X_syn_train] + [p[0] for p in real_train_pieces]
    y_train_pieces = [y_syn_train] + [p[1] for p in real_train_pieces]
    X_val_pieces = [X_syn_val] + [p[0] for p in real_val_pieces]
    y_val_pieces = [y_syn_val] + [p[1] for p in real_val_pieces]

    X_train = np.concatenate(X_train_pieces, axis=0)
    y_train_gen = np.concatenate(y_train_pieces, axis=0)
    X_val = np.concatenate(X_val_pieces, axis=0)
    y_val_gen = np.concatenate(y_val_pieces, axis=0)

    # shuffle train (val order doesn't matter for evaluation)
    shuffle_idx = rng.permutation(len(X_train))
    X_train, y_train_gen = X_train[shuffle_idx], y_train_gen[shuffle_idx]

    # 4. Flip to TCN's own label direction (1 = perturbed) -- see src/model/tcn.py
    y_train = 1 - y_train_gen
    y_val = 1 - y_val_gen

    # 5. Normalization: fit on train only, apply to both (PRD section 3)
    channel_mean = X_train.mean(axis=(0, 1))
    channel_std = X_train.std(axis=(0, 1))
    channel_std[channel_std < 1e-6] = 1e-6  # guard against divide-by-zero on a constant channel

    X_train_norm = (X_train - channel_mean) / channel_std
    X_val_norm = (X_val - channel_mean) / channel_std

    return DatasetBundle(
        X_train=X_train_norm,
        y_train=y_train,
        X_val=X_val_norm,
        y_val=y_val,
        channel_mean=channel_mean,
        channel_std=channel_std,
        n_real_train_raw=n_real_train_raw,
        n_real_val=n_real_val,
    )