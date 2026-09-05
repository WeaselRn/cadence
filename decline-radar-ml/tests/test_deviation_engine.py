import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..",))
from src.deviation_engine.engine import initial_state, process_session, DeviationEngineConfig


def make_metrics(score, cadence=100, symmetry=0.95, speed=1.1):
    return {
        "mobility_stability_score": score,
        "cadence_steps_per_min": cadence,
        "symmetry_index": symmetry,
        "estimated_speed_m_s": speed,
    }


def run_baseline(state, cfg, scores):
    for score in scores:
        state, result = process_session(state, make_metrics(score), cfg)
    return state, result


def test_baseline_establishes_after_min_sessions():
    cfg = DeviationEngineConfig(min_baseline_sessions=4)
    state = initial_state()
    state, result = run_baseline(state, cfg, [86, 89, 88, 87])
    assert result["baseline_established"] is True
    assert state["baseline_stats"]["mobility_stability_score"]["mean"] == 87.5


def test_stable_session_does_not_deviate():
    cfg = DeviationEngineConfig(min_baseline_sessions=4)
    state = initial_state()
    state, _ = run_baseline(state, cfg, [86, 89, 88, 87])
    state, result = process_session(state, make_metrics(87), cfg)
    assert result["is_current_session_substantial_deviation"] is False


def test_persistent_change_flags_after_consecutive_deviations():
    cfg = DeviationEngineConfig(min_baseline_sessions=4, persistence_consecutive_sessions=3)
    state = initial_state()
    state, _ = run_baseline(state, cfg, [86, 89, 88, 87])
    for score in [50, 48]:
        state, result = process_session(state, make_metrics(score), cfg)
        assert result["persistent_change_flagged"] is False
    state, result = process_session(state, make_metrics(52), cfg)
    assert result["persistent_change_flagged"] is True
    assert result["consecutive_deviation_count"] == 3


def test_recovery_resets_consecutive_count():
    cfg = DeviationEngineConfig(min_baseline_sessions=4, persistence_consecutive_sessions=3)
    state = initial_state()
    state, _ = run_baseline(state, cfg, [86, 89, 88, 87])
    state, _ = process_session(state, make_metrics(50), cfg)
    state, _ = process_session(state, make_metrics(48), cfg)
    state, result = process_session(state, make_metrics(90), cfg)
    assert result["consecutive_deviation_count"] == 0
    assert result["is_current_session_substantial_deviation"] is False