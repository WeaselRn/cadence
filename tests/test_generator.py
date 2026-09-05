import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
import numpy as np
from synthetic.generator import generate_dataset, generate_session, GeneratorConfig


def test_dataset_shapes_and_balance():
    X, y, meta = generate_dataset(20, seed=42)
    assert X.shape == (20, 1500, 6)
    assert y.shape == (20,)
    assert sorted(np.bincount(y)) == [10, 10]


def test_regular_session_has_no_perturbations():
    accel, meta = generate_session(1, rng=np.random.default_rng(0))
    assert meta["label"] == 1
    assert meta["perturbations_applied"] == []


def test_perturbed_session_has_at_least_one_perturbation():
    accel, meta = generate_session(0, rng=np.random.default_rng(0))
    assert meta["label"] == 0
    assert len(meta["perturbations_applied"]) >= 1


def test_freezing_produces_near_zero_motion_window():
    accel, meta = generate_session(0, rng=np.random.default_rng(2), perturbation_types=["freezing"])
    z_dev = np.abs(accel[:, 2] - 9.8)
    window_stds = [z_dev[i:i+25].std() for i in range(0, len(z_dev) - 25, 25)]
    assert min(window_stds) < 0.2