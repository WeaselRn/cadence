"""
Synthetic IMU session generator.

Implements the labeling scheme from docs/tcn_target_definition.md:
- label 1 = "regular" steady-state gait
- label 0 = "perturbed" gait, with a random subset of four perturbation
  types applied (cadence drift, asymmetry, jitter, freezing episodes)

Output shape per session: (N, 6) = [accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z]
Default: 50 Hz, 30 seconds -> N = 1500, matching PRD section 3's fixed window.

WAVEFORM DESIGN (fixed after a real bug found via heuristics validation):
Each full gait cycle (left step + right step) is modeled with TWO peaks,
using amplitude * |sin(phase)|, where phase advances at cycle_freq_hz =
cadence / 120 (cadence counts BOTH feet per minute, and one full 2*pi
phase cycle now spans exactly 2 steps). This gives:
  - peak 1 at phase = pi/2   (within phase range [0, pi))   -- "left" step
  - peak 2 at phase = 3pi/2  (within phase range [pi, 2*pi)) -- "right" step
The asymmetry perturbation's half_cycle mask ([pi, 2*pi)) therefore lands
exactly on one of the two real per-cycle peaks, making injected asymmetry
actually visible to peak-based symmetry measurement (src/heuristics).

An earlier version used a single sin(phase) per cycle (one peak per cycle,
frequency = cadence/60). That made cadence detection accurate, but made
asymmetry structurally undetectable, since the single peak always fell
in the unaffected phase half. Cadence detection logic in src/heuristics
is unaffected by this change (it just counts peaks / duration), since two
peaks per (120/cadence)-second cycle still works out to `cadence`
peaks per minute.
"""

from dataclasses import dataclass, field
import numpy as np


@dataclass
class GeneratorConfig:
    sample_rate_hz: float = 50.0
    duration_seconds: float = 30.0
    cadence_range_spm: tuple = (90.0, 120.0)   # TOTAL steps per minute (both feet), label-1 baseline
    base_noise_std: float = 0.1                 # m/s^2, baseline sensor noise
    gyro_noise_std: float = 0.2                 # rad/s baseline noise
    step_amplitude: float = 1.5                 # m/s^2, vertical oscillation amplitude
    sway_amplitude: float = 0.3                 # m/s^2, lateral/forward sway
    perturbation_severity_range: tuple = (0.3, 1.0)


PERTURBATION_TYPES = ["cadence_drift", "asymmetry", "jitter", "freezing"]


def _base_regular_signal(cfg: GeneratorConfig, rng: np.random.Generator):
    """Builds a clean, regular walking session (the label-1 baseline)."""
    n = int(cfg.duration_seconds * cfg.sample_rate_hz)
    t = np.arange(n) / cfg.sample_rate_hz

    cadence = rng.uniform(*cfg.cadence_range_spm)  # total steps/min, both feet
    cycle_freq_hz = cadence / 120.0                # one full 2-step cycle per this frequency

    phase = 2 * np.pi * cycle_freq_hz * t

    accel = np.zeros((n, 6))
    accel[:, 2] = 9.8 + cfg.step_amplitude * np.abs(np.sin(phase))  # two peaks per cycle
    accel[:, 0] = cfg.sway_amplitude * np.sin(phase)                # alternating L/R sway
    accel[:, 1] = cfg.sway_amplitude * np.cos(phase)
    accel[:, 3:6] = 0.0  # gyro baseline, noise added later

    accel[:, 0:3] += rng.normal(0, cfg.base_noise_std, (n, 3))
    accel[:, 3:6] += rng.normal(0, cfg.gyro_noise_std, (n, 3))

    return accel, t, phase, cadence


def _apply_cadence_drift(accel, t, cfg, severity, rng):
    """Re-synthesizes the oscillation with a drifting instantaneous cycle frequency."""
    n = len(t)
    dt = 1.0 / cfg.sample_rate_hz
    base_cadence = rng.uniform(*cfg.cadence_range_spm)
    base_cycle_freq = base_cadence / 120.0
    drift_amplitude = base_cycle_freq * 0.4 * severity  # up to +-40% freq wobble at severity=1

    steps = rng.normal(0, drift_amplitude * dt, n)
    freq_wobble = np.cumsum(steps)
    freq_wobble -= freq_wobble.mean()
    instantaneous_freq = base_cycle_freq + freq_wobble

    phase = 2 * np.pi * np.cumsum(instantaneous_freq) * dt

    accel = accel.copy()
    accel[:, 2] = 9.8 + cfg.step_amplitude * np.abs(np.sin(phase))
    accel[:, 0] = cfg.sway_amplitude * np.sin(phase)
    accel[:, 1] = cfg.sway_amplitude * np.cos(phase)
    return accel, phase


def _apply_asymmetry(accel, phase, cfg, severity, rng):
    """
    Scales one of the two per-cycle peaks (phase in [pi, 2*pi), the
    "second step") relative to the other -- a proxy for uneven left/right
    steps. See module docstring for why this now actually works.
    """
    accel = accel.copy()
    half_cycle = (phase % (2 * np.pi)) >= np.pi
    asymmetry_factor = 1.0 - 0.6 * severity  # up to 60% amplitude reduction on one side
    accel[half_cycle, 2] = 9.8 + (accel[half_cycle, 2] - 9.8) * asymmetry_factor
    accel[half_cycle, 0:2] *= asymmetry_factor
    return accel


def _apply_jitter(accel, cfg, severity, rng):
    """Adds elevated high-frequency noise (tremor-like) on top of the signal."""
    accel = accel.copy()
    n = accel.shape[0]
    jitter_std_accel = cfg.base_noise_std * (1.0 + 6.0 * severity)
    jitter_std_gyro = cfg.gyro_noise_std * (1.0 + 6.0 * severity)
    accel[:, 0:3] += rng.normal(0, jitter_std_accel, (n, 3))
    accel[:, 3:6] += rng.normal(0, jitter_std_gyro, (n, 3))
    return accel


def _apply_freezing(accel, cfg, severity, rng):
    """Splices in 1-3 brief near-zero-motion segments (freezing episodes)."""
    accel = accel.copy()
    n = accel.shape[0]
    n_episodes = rng.integers(1, 4)
    min_len = int(0.5 * cfg.sample_rate_hz)
    max_len = int(2.0 * cfg.sample_rate_hz)

    for _ in range(n_episodes):
        episode_len = rng.integers(min_len, max_len + 1)
        if episode_len >= n:
            continue
        start = rng.integers(0, n - episode_len)
        end = start + episode_len
        residual_motion = (1.0 - severity) * 0.3  # more severe = more "frozen"
        accel[start:end, 0:2] *= residual_motion
        accel[start:end, 2] = 9.8 + (accel[start:end, 2] - 9.8) * residual_motion
        accel[start:end, 3:6] *= residual_motion
    return accel


def generate_session(label: int, cfg: GeneratorConfig = None, rng: np.random.Generator = None,
                      perturbation_types: list = None):
    """
    Generates one synthetic session.

    label: 1 = regular, 0 = perturbed
    perturbation_types: for label=0, restrict to this subset (default: random subset of all four)

    Returns (raw_session: np.ndarray of shape (N, 6), metadata: dict)
    """
    if cfg is None:
        cfg = GeneratorConfig()
    if rng is None:
        rng = np.random.default_rng()

    accel, t, phase, cadence = _base_regular_signal(cfg, rng)

    metadata = {
        "label": label,
        "sample_rate_hz": cfg.sample_rate_hz,
        "duration_seconds": cfg.duration_seconds,
        "base_cadence_steps_per_min": cadence,
        "perturbations_applied": [],
    }

    if label == 0:
        available = perturbation_types if perturbation_types is not None else PERTURBATION_TYPES
        n_to_apply = rng.integers(1, len(available) + 1)
        chosen = list(rng.choice(available, size=n_to_apply, replace=False))

        for ptype in chosen:
            ptype = str(ptype)
            severity = float(rng.uniform(*cfg.perturbation_severity_range))
            if ptype == "cadence_drift":
                accel, phase = _apply_cadence_drift(accel, t, cfg, severity, rng)
            elif ptype == "asymmetry":
                accel = _apply_asymmetry(accel, phase, cfg, severity, rng)
            elif ptype == "jitter":
                accel = _apply_jitter(accel, cfg, severity, rng)
            elif ptype == "freezing":
                accel = _apply_freezing(accel, cfg, severity, rng)
            else:
                raise ValueError(f"Unknown perturbation type: {ptype}")
            metadata["perturbations_applied"].append({"type": ptype, "severity": round(severity, 3)})

    return accel, metadata


def generate_dataset(n_samples: int, cfg: GeneratorConfig = None, seed: int = None,
                      balance: float = 0.5):
    """
    Generates a balanced dataset of synthetic sessions.

    Returns:
        X: np.ndarray of shape (n_samples, N, 6)
        y: np.ndarray of shape (n_samples,), binary labels
        metadata_list: list of per-sample metadata dicts
    """
    if cfg is None:
        cfg = GeneratorConfig()
    rng = np.random.default_rng(seed)

    n = int(cfg.duration_seconds * cfg.sample_rate_hz)
    X = np.zeros((n_samples, n, 6))
    y = np.zeros(n_samples, dtype=int)
    metadata_list = []

    n_regular = int(round(n_samples * balance))
    labels = np.array([1] * n_regular + [0] * (n_samples - n_regular))
    rng.shuffle(labels)

    for i, label in enumerate(labels):
        session, meta = generate_session(int(label), cfg=cfg, rng=rng)
        X[i] = session
        y[i] = label
        metadata_list.append(meta)

    return X, y, metadata_list