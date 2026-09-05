"""
UCI HAR real-data loader — Phase B anchoring (see docs/tcn_target_definition.md).

UCI HAR's "Inertial Signals" folder contains raw 128-sample windows (2.56s at
50Hz) with 50% overlap, per activity label and subject. This loader:

1. Reads the raw per-axis window files (total_acc_*, body_gyro_*).
2. Keeps only WALKING-labeled windows — level-ground, real human walking.
   Other activities (stairs, sitting, etc.) are NOT used, and are never
   treated as "perturbed" gait — they're a different activity, not gait
   irregularity, and labeling them as such would corrupt the label-0 class.
3. Stitches consecutive same-subject, same-activity windows back into
   longer continuous streams (using the 50%-overlap structure).
4. Slides a 1500-sample (30s) window across each stream to produce
   real label-1 ("regular gait") anchor examples.

NOTE: The Data Quality Gatekeeper (src/data_quality/gatekeeper.py) is
intentionally NOT applied to this data. UCI HAR's samples are already
pre-vetted, clean WALKING recordings from a controlled study, and the
Gatekeeper's orientation check assumes pocket-carry (gravity ~aligned to
device z-axis) which does not necessarily hold for UCI HAR's waist-mounted
device orientation. Applying it here could silently and incorrectly
reject good real data.

Units: per the dataset README, total_acc is in g's; converted here to
m/s^2 (x9.80665) to match src/synthetic/generator.py's convention. Gyro
is already in rad/s, left as-is.
"""

import os
import numpy as np

G_TO_MS2 = 9.80665


def resolve_activity_label_id(uci_har_root: str, activity_name: str = "WALKING") -> int:
    """
    Reads activity_labels.txt (format: '<id> <NAME>' per line, e.g. '1 WALKING')
    and returns the numeric id for the given activity name. Never hardcode this —
    it's dataset metadata, not a fixed convention to assume.
    """
    path = os.path.join(uci_har_root, "activity_labels.txt")
    mapping = {}
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            label_id, name = int(parts[0]), parts[1]
            mapping[name.upper()] = label_id

    key = activity_name.upper()
    if key not in mapping:
        raise ValueError(
            f"Activity '{activity_name}' not found in {path}. Available: {list(mapping.keys())}"
        )
    return mapping[key]


AXIS_FILES = {
    "accel": ["total_acc_x_{split}.txt", "total_acc_y_{split}.txt", "total_acc_z_{split}.txt"],
    "gyro": ["body_gyro_x_{split}.txt", "body_gyro_y_{split}.txt", "body_gyro_z_{split}.txt"],
}


def _load_axis_files(inertial_signals_dir: str, split: str, key: str) -> np.ndarray:
    """Loads and stacks the three axis files for accel or gyro. Returns (n_windows, 128, 3)."""
    arrays = []
    for fname_template in AXIS_FILES[key]:
        fname = fname_template.format(split=split)
        path = os.path.join(inertial_signals_dir, fname)
        arrays.append(np.loadtxt(path))  # shape (n_windows, 128)
    return np.stack(arrays, axis=-1)  # shape (n_windows, 128, 3)


def load_raw_windows(uci_har_root: str, split: str = "train"):
    """
    Loads raw UCI HAR windows for a split.

    Expects uci_har_root to contain: {split}/Inertial Signals/*.txt,
    {split}/y_{split}.txt, {split}/subject_{split}.txt

    Returns:
        accel: (n_windows, 128, 3) in m/s^2
        gyro: (n_windows, 128, 3) in rad/s
        labels: (n_windows,)
        subjects: (n_windows,)
    """
    inertial_dir = os.path.join(uci_har_root, split, "Inertial Signals")
    accel_g = _load_axis_files(inertial_dir, split, "accel")
    gyro = _load_axis_files(inertial_dir, split, "gyro")
    accel = accel_g * G_TO_MS2

    labels = np.loadtxt(os.path.join(uci_har_root, split, f"y_{split}.txt")).astype(int)
    subjects = np.loadtxt(os.path.join(uci_har_root, split, f"subject_{split}.txt")).astype(int)

    return accel, gyro, labels, subjects


def _stitch_overlapping_windows(windows: np.ndarray, overlap: int = 64) -> np.ndarray:
    """
    Stitches a sequence of consecutive 50%-overlapping windows (shape
    (n, window_len, channels)) into one continuous array by taking the
    full first window, then only the non-overlapping tail of each
    subsequent window.
    """
    if len(windows) == 0:
        return np.zeros((0, windows.shape[-1]))
    if len(windows) == 1:
        return windows[0]

    pieces = [windows[0]]
    for w in windows[1:]:
        pieces.append(w[overlap:])
    return np.concatenate(pieces, axis=0)


def _group_contiguous_runs(labels: np.ndarray, subjects: np.ndarray, target_label: int):
    """
    Yields (start_idx, end_idx) index ranges where subject and label are
    both constant and equal to target_label for subject, i.e. runs of
    consecutive rows that plausibly come from the same continuous trial.
    """
    n = len(labels)
    start = None
    for i in range(n):
        is_target = labels[i] == target_label
        same_subject_as_prev = (i == 0) or (subjects[i] == subjects[i - 1])
        if is_target and (start is None):
            start = i
        elif is_target and start is not None and not same_subject_as_prev:
            yield (start, i)
            start = i
        elif not is_target and start is not None:
            yield (start, i)
            start = None
    if start is not None:
        yield (start, n)


def extract_regular_anchor_windows(uci_har_root: str, split: str = "train",
                                    target_length: int = 1500, stride: int = 750,
                                    window_overlap: int = 64, activity_name: str = "WALKING"):
    """
    Full pipeline: load -> filter to WALKING -> stitch contiguous runs ->
    slide fixed-length windows -> return real label-1 anchor sessions.

    Returns:
        X_real: (n_samples, target_length, 6) array, columns
                [accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z]
        meta_list: list of dicts (subject id, source run info)
    """
    accel, gyro, labels, subjects = load_raw_windows(uci_har_root, split)
    walking_label_id = resolve_activity_label_id(uci_har_root, activity_name)

    sessions = []
    metas = []

    for start, end in _group_contiguous_runs(labels, subjects, walking_label_id):
        run_accel = _stitch_overlapping_windows(accel[start:end], overlap=window_overlap)
        run_gyro = _stitch_overlapping_windows(gyro[start:end], overlap=window_overlap)
        run = np.concatenate([run_accel, run_gyro], axis=1)  # (T, 6)

        if len(run) < target_length:
            continue  # too short to extract a full 30s window, skip

        subject_id = int(subjects[start])
        for w_start in range(0, len(run) - target_length + 1, stride):
            chunk = run[w_start:w_start + target_length]
            sessions.append(chunk)
            metas.append({
                "label": 1,
                "source": "uci_har",
                "split": split,
                "subject_id": subject_id,
                "run_start_idx": start,
                "run_end_idx": end,
                "chunk_offset": w_start,
                "gatekeeper_applied": False,
            })

    if not sessions:
        return np.zeros((0, target_length, 6)), []

    return np.stack(sessions, axis=0), metas