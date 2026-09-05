"""
Data Quality Gatekeeper (PRD section 4).

Takes a raw IMU session window and decides whether it is valid enough to run
through the heuristics + TCN, or should be discarded before inference.

Raw session shape convention: numpy array of shape (N, 6) where columns are
[accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z], sampled at `sample_rate_hz`.
"""

from dataclasses import dataclass
import numpy as np


@dataclass
class GatekeeperConfig:
    min_walking_duration_seconds: float = 10.0
    min_signal_quality_index: float = 0.5
    max_stationary_intervals: int = 2
    # Pocket-carry angle: angle (degrees) between the mean gravity vector and
    # the device's dominant axis. Small angle = consistent vertical carry
    # (pocket-like). Large / unstable angle = bag-like or inconsistent carry.
    max_orientation_angle_degrees: float = 35.0
    stationary_accel_std_threshold: float = 0.15  # m/s^2, below this = "still"
    stationary_window_seconds: float = 1.0


def _gravity_angle_degrees(accel: np.ndarray) -> float:
    """
    Angle between the mean accelerometer vector and the vertical (z) axis,
    in degrees. Near 0 degrees = device consistently oriented one way
    (pocket-like, assuming pocket carry keeps phone roughly upright/vertical).
    """
    mean_vec = accel.mean(axis=0)
    norm = np.linalg.norm(mean_vec)
    if norm < 1e-6:
        return 90.0  # degenerate signal, treat as bad orientation
    z_axis = np.array([0.0, 0.0, 1.0])
    cos_angle = np.dot(mean_vec, z_axis) / norm
    cos_angle = np.clip(cos_angle, -1.0, 1.0)
    angle_rad = np.arccos(cos_angle)
    return float(np.degrees(angle_rad))


def _count_stationary_intervals(accel: np.ndarray, sample_rate_hz: float, cfg: GatekeeperConfig) -> int:
    """
    Counts contiguous windows where accelerometer magnitude variance is
    near-zero, indicating the phone was set down / not moving.
    """
    window_size = max(1, int(cfg.stationary_window_seconds * sample_rate_hz))
    magnitudes = np.linalg.norm(accel, axis=1)
    stationary_windows = 0
    in_stationary = False
    for start in range(0, len(magnitudes) - window_size + 1, window_size):
        chunk = magnitudes[start:start + window_size]
        if chunk.std() < cfg.stationary_accel_std_threshold:
            if not in_stationary:
                stationary_windows += 1
                in_stationary = True
        else:
            in_stationary = False
    return stationary_windows


def _signal_quality_index(accel: np.ndarray, gyro: np.ndarray) -> float:
    """
    Simple proxy for signal quality: penalizes clipped/saturated readings
    and NaN/dropout frames. Returns a value in [0, 1].
    """
    total_frames = accel.shape[0]
    if total_frames == 0:
        return 0.0

    nan_frames = np.isnan(accel).any(axis=1) | np.isnan(gyro).any(axis=1)
    # crude saturation check: accel values pinned near sensor rails (~+-78 m/s^2 for many phones)
    saturated_frames = (np.abs(accel) > 75.0).any(axis=1)

    bad_frames = nan_frames | saturated_frames
    quality = 1.0 - (bad_frames.sum() / total_frames)
    return float(np.clip(quality, 0.0, 1.0))


def evaluate_session(raw_session: np.ndarray, sample_rate_hz: float, cfg: GatekeeperConfig = None) -> dict:
    """
    Evaluates a raw (N, 6) IMU session and returns a data_quality dict
    matching the PRD's Room DB schema shape.
    """
    if cfg is None:
        cfg = GatekeeperConfig()

    if raw_session.ndim != 2 or raw_session.shape[1] != 6:
        raise ValueError(f"Expected raw_session shape (N, 6), got {raw_session.shape}")

    accel = raw_session[:, 0:3]
    gyro = raw_session[:, 3:6]

    walking_duration_seconds = raw_session.shape[0] / sample_rate_hz
    signal_quality_index = _signal_quality_index(accel, gyro)
    stationary_intervals = _count_stationary_intervals(accel, sample_rate_hz, cfg)
    orientation_angle = _gravity_angle_degrees(accel)

    fails_duration = walking_duration_seconds < cfg.min_walking_duration_seconds
    fails_quality = signal_quality_index < cfg.min_signal_quality_index
    fails_stationary = stationary_intervals > cfg.max_stationary_intervals
    fails_orientation = orientation_angle > cfg.max_orientation_angle_degrees

    is_valid_session = not (fails_duration or fails_quality or fails_stationary or fails_orientation)

    return {
        "is_valid_session": is_valid_session,
        "walking_duration_seconds": round(walking_duration_seconds, 2),
        "signal_quality_index": round(signal_quality_index, 3),
        "stationary_intervals": stationary_intervals,
        "orientation_angle_degrees": round(orientation_angle, 2),
        "rejection_reasons": [
            reason for reason, failed in [
                ("walking_duration_too_short", fails_duration),
                ("signal_quality_too_low", fails_quality),
                ("too_many_stationary_intervals", fails_stationary),
                ("orientation_not_pocket_like", fails_orientation),
            ] if failed
        ],
    }