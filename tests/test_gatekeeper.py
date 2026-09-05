import numpy as np
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from src.data_quality.gatekeeper import evaluate_session


def make_session(seconds, sample_rate=50, orientation="pocket"):
    n = int(seconds * sample_rate)
    t = np.arange(n) / sample_rate
    accel = np.zeros((n, 6))
    if orientation == "pocket":
        accel[:, 2] = 9.8 + 1.5 * np.sin(2 * np.pi * 1.8 * t)
        accel[:, 0] = 0.3 * np.sin(2 * np.pi * 1.8 * t)
    else:  # bag-like: gravity horizontal
        accel[:, 0] = 9.8
        accel[:, 2] = 0.2
    accel[:, 3:6] = np.random.normal(0, 0.2, (n, 3))
    return accel, sample_rate


def test_good_session_passes():
    accel, sr = make_session(30, orientation="pocket")
    result = evaluate_session(accel, sr)
    assert result["is_valid_session"] is True


def test_short_session_rejected():
    accel, sr = make_session(7, orientation="pocket")
    result = evaluate_session(accel, sr)
    assert result["is_valid_session"] is False
    assert "walking_duration_too_short" in result["rejection_reasons"]


def test_bag_orientation_rejected():
    accel, sr = make_session(30, orientation="bag")
    result = evaluate_session(accel, sr)
    assert result["is_valid_session"] is False
    assert "orientation_not_pocket_like" in result["rejection_reasons"]