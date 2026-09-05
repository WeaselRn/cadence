"""
Deterministic Heuristic Gait Feature Extraction.

Extracts core biomechanical metrics from IMU session recordings:
- cadence_steps_per_min: Steps taken per minute (both feet).
- symmetry_index: Peak amplitude balance between alternating steps (1.0 = perfect symmetry).
- estimated_speed_m_s: Estimated forward walking speed based on cadence.
"""

from typing import Dict
import numpy as np
from scipy.signal import butter, filtfilt, find_peaks


def compute_heuristics(session: np.ndarray, sample_rate_hz: float = 50.0) -> Dict[str, float]:
    """
    Computes heuristic gait metrics from a 6-axis IMU session (accel_x..z, gyro_x..z).

    Args:
        session: (N, 6) or (N, 3+) array of motion data, where column 2 is vertical acceleration.
        sample_rate_hz: Sampling frequency in Hertz (default: 50 Hz).

    Returns:
        Dict with keys:
            - "cadence_steps_per_min": float
            - "symmetry_index": float
            - "estimated_speed_m_s": float
    """
    min_samples = int(sample_rate_hz * 1.0)
    if session is None or len(session) < min_samples:
        return {
            "cadence_steps_per_min": 0.0,
            "symmetry_index": 0.0,
            "estimated_speed_m_s": 0.0,
        }

    # Vertical axis (column 2 in standard pocket convention [accel_x, accel_y, accel_z, ...])
    z = session[:, 2]

    # 2nd-order lowpass filter at 4 Hz to attenuate high-frequency noise while preserving gait rhythm
    nyquist = sample_rate_hz / 2.0
    cutoff = min(4.0, nyquist * 0.5)
    b, a = butter(2, cutoff / nyquist, btype="low")

    padlen = min(15, len(z) - 1)
    z_smooth = filtfilt(b, a, z, padlen=padlen)

    # Minimum distance between steps (0.35s corresponds to ~170 steps/min maximum expected cadence)
    min_distance = max(1, int(sample_rate_hz * 0.35))
    peaks, _ = find_peaks(z_smooth, distance=min_distance, prominence=0.15)

    duration_seconds = len(session) / sample_rate_hz
    n_peaks = len(peaks)

    if n_peaks < 2 or duration_seconds <= 0:
        return {
            "cadence_steps_per_min": 0.0,
            "symmetry_index": 0.0,
            "estimated_speed_m_s": 0.0,
        }

    cadence = (n_peaks / duration_seconds) * 60.0

    # Step amplitudes relative to baseline
    peak_amps = z_smooth[peaks] - np.median(z_smooth)
    even_amps = peak_amps[0::2]
    odd_amps = peak_amps[1::2]

    mean_even = float(np.mean(even_amps)) if len(even_amps) > 0 else 0.0
    mean_odd = float(np.mean(odd_amps)) if len(odd_amps) > 0 else 0.0

    if max(mean_even, mean_odd) > 1e-6:
        symmetry = float(min(mean_even, mean_odd) / max(mean_even, mean_odd))
    else:
        symmetry = 0.0

    # Standard gait speed estimation: (cadence / 60) * step_length (~0.66m)
    estimated_speed = float((cadence / 60.0) * 0.66)

    return {
        "cadence_steps_per_min": float(cadence),
        "symmetry_index": float(symmetry),
        "estimated_speed_m_s": float(estimated_speed),
    }