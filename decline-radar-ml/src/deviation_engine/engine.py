"""
Longitudinal Deviation Engine (PRD section 6).

Design note on PRD ambiguity: section 6 says the engine cross-references
"the deterministic metrics AND the TCN movement score" against baseline,
but section 7's JSON schema only has one is_current_session_substantial_deviation
flag. Resolution used here: baselines are tracked for ALL FOUR metrics
(mobility_stability_score, cadence, symmetry, speed) for transparency, but
the substantial-deviation flag is driven by mobility_stability_score alone
(PRIMARY_METRIC below) -- it already reflects the TCN's pattern-level read
on top of the heuristics. Revisit this once real longitudinal data shows
whether the heuristic metrics add independent signal worth combining.

Designed as a pure function over a serializable state dict, so the Android
side can load state from Room, call process_session(), and persist the
returned new_state -- no in-memory object needs to survive between app runs.

"Lower is worse" is assumed consistent across all four tracked metrics
(a real functional decline lowers cadence, symmetry, speed, and the
stability score alike), so deviation is always checked as value < mean - k*std,
never a two-sided check.
"""

from dataclasses import dataclass, field


TRACKED_METRICS = (
    "mobility_stability_score",
    "cadence_steps_per_min",
    "symmetry_index",
    "estimated_speed_m_s",
)
PRIMARY_METRIC = "mobility_stability_score"


@dataclass
class DeviationEngineConfig:
    min_baseline_sessions: int = 4       # PRD says "3 to 5" -- single configurable knob
    deviation_std_threshold: float = 2.0  # substantial deviation = below mean - k*std
    persistence_consecutive_sessions: int = 3
    min_std_floor: float = 1e-6          # guards against a zero-variance baseline


def initial_state() -> dict:
    return {
        "baseline_established": False,
        "baseline_stats": None,           # {metric: {"mean": float, "std": float}}
        "baseline_buffer": {m: [] for m in TRACKED_METRICS},
        "consecutive_deviation_count": 0,
    }


def _compute_baseline_stats(buffer: dict) -> dict:
    stats = {}
    for metric, values in buffer.items():
        n = len(values)
        mean = sum(values) / n
        variance = sum((v - mean) ** 2 for v in values) / n
        std = variance ** 0.5
        stats[metric] = {"mean": mean, "std": std}
    return stats


def process_session(state: dict, session_metrics: dict, cfg: DeviationEngineConfig = None):
    """
    session_metrics must contain all keys in TRACKED_METRICS.

    Returns (new_state: dict, result: dict) where result matches PRD section 7's
    deviation_engine JSON shape:
        baseline_established, is_current_session_substantial_deviation,
        consecutive_deviation_count, persistent_change_flagged
    """
    if cfg is None:
        cfg = DeviationEngineConfig()

    missing = [m for m in TRACKED_METRICS if m not in session_metrics]
    if missing:
        raise ValueError(f"session_metrics missing required keys: {missing}")

    new_state = {
        "baseline_established": state["baseline_established"],
        "baseline_stats": state["baseline_stats"],
        "baseline_buffer": {m: list(v) for m, v in state["baseline_buffer"].items()},
        "consecutive_deviation_count": state["consecutive_deviation_count"],
    }

    if not new_state["baseline_established"]:
        for metric in TRACKED_METRICS:
            new_state["baseline_buffer"][metric].append(session_metrics[metric])

        if len(new_state["baseline_buffer"][PRIMARY_METRIC]) >= cfg.min_baseline_sessions:
            new_state["baseline_stats"] = _compute_baseline_stats(new_state["baseline_buffer"])
            new_state["baseline_established"] = True

        result = {
            "baseline_established": new_state["baseline_established"],
            "is_current_session_substantial_deviation": False,
            "consecutive_deviation_count": 0,
            "persistent_change_flagged": False,
        }
        return new_state, result

    # Baseline already established -- evaluate this session against it
    primary_stats = new_state["baseline_stats"][PRIMARY_METRIC]
    std = max(primary_stats["std"], cfg.min_std_floor)
    threshold_value = primary_stats["mean"] - cfg.deviation_std_threshold * std

    is_deviation = session_metrics[PRIMARY_METRIC] < threshold_value

    if is_deviation:
        new_state["consecutive_deviation_count"] += 1
    else:
        new_state["consecutive_deviation_count"] = 0

    persistent_change_flagged = (
        new_state["consecutive_deviation_count"] >= cfg.persistence_consecutive_sessions
    )

    result = {
        "baseline_established": True,
        "is_current_session_substantial_deviation": is_deviation,
        "consecutive_deviation_count": new_state["consecutive_deviation_count"],
        "persistent_change_flagged": persistent_change_flagged,
    }
    return new_state, result