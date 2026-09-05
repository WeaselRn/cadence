import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
import numpy as np
import tempfile
from real_data.uci_har_loader import _group_contiguous_runs, _stitch_overlapping_windows


def test_stitching_removes_overlap():
    windows = np.array([[[1], [2], [3], [4]], [[3], [4], [5], [6]]])  # overlap=2
    stitched = _stitch_overlapping_windows(windows, overlap=2)
    assert stitched.flatten().tolist() == [1, 2, 3, 4, 5, 6]


def test_subject_boundary_splits_runs_even_with_same_label():
    labels = np.array([1]*10 + [1]*10 + [2]*5)
    subjects = np.array([1]*10 + [2]*10 + [2]*5)
    runs = list(_group_contiguous_runs(labels, subjects, target_label=1))
    assert runs == [(0, 10), (10, 20)]


def test_non_walking_label_excluded():
    labels = np.array([2]*10)
    subjects = np.array([1]*10)
    runs = list(_group_contiguous_runs(labels, subjects, target_label=1))
    assert runs == []



def test_resolve_activity_label_id_reads_file():
    from real_data.uci_har_loader import resolve_activity_label_id
    with tempfile.TemporaryDirectory() as tmp:
        with open(os.path.join(tmp, "activity_labels.txt"), "w") as f:
            f.write("1 WALKING\n2 WALKING_UPSTAIRS\n3 WALKING_DOWNSTAIRS\n")
        assert resolve_activity_label_id(tmp, "WALKING") == 1
        assert resolve_activity_label_id(tmp, "walking_upstairs") == 2