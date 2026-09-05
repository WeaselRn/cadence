import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import numpy as np
from src.synthetic.generator import generate_session, GeneratorConfig
from src.heuristics.heuristics import compute_heuristics


def test_cadence_matches_ground_truth_within_tolerance():
    cfg = GeneratorConfig()
    rng = np.random.default_rng(0)
    session, meta = generate_session(1, cfg=cfg, rng=rng)
    result = compute_heuristics(session, cfg.sample_rate_hz)
    ground_truth = meta["base_cadence_steps_per_min"]
    assert abs(result["cadence_steps_per_min"] - ground_truth) / ground_truth < 0.03


def test_regular_session_has_high_symmetry():
    cfg = GeneratorConfig()
    rng = np.random.default_rng(0)
    session, _ = generate_session(1, cfg=cfg, rng=rng)
    result = compute_heuristics(session, cfg.sample_rate_hz)
    assert result["symmetry_index"] > 0.9


def test_asymmetric_session_has_lower_symmetry_than_regular():
    cfg = GeneratorConfig()
    rng = np.random.default_rng(5)
    regular, _ = generate_session(1, cfg=cfg, rng=rng)
    regular_result = compute_heuristics(regular, cfg.sample_rate_hz)

    rng2 = np.random.default_rng(5)
    asymmetric, _ = generate_session(0, cfg=cfg, rng=rng2, perturbation_types=["asymmetry"])
    asymmetric_result = compute_heuristics(asymmetric, cfg.sample_rate_hz)

    assert asymmetric_result["symmetry_index"] < regular_result["symmetry_index"]
