import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from src.real_data.uci_har_loader import extract_regular_anchor_windows

CANDIDATE_UCI_PATHS = [
    os.environ.get("UCI_HAR_ROOT", ""),
    os.path.join(os.path.dirname(__file__), "..", "data", "UCI HAR Dataset"),
    os.path.join(os.path.dirname(__file__), "..", "data", "UCI_HAR_Dataset"),
]
if os.path.exists(r"A:\UCI HAR Dataset"):
    CANDIDATE_UCI_PATHS.append(r"A:\UCI HAR Dataset")

UCI_HAR_ROOT = next((p for p in CANDIDATE_UCI_PATHS if p and os.path.exists(p) and os.path.exists(os.path.join(p, "activity_labels.txt"))), None)
if UCI_HAR_ROOT is None:
    print("UCI HAR Dataset not found. Set UCI_HAR_ROOT environment variable.")
    sys.exit(0)  

X_train_real, meta_train = extract_regular_anchor_windows(UCI_HAR_ROOT, split="train")
X_test_real, meta_test = extract_regular_anchor_windows(UCI_HAR_ROOT, split="test")

print("train real anchors:", X_train_real.shape)
print("test real anchors:", X_test_real.shape)
if len(meta_train) > 0:
    print("sample metadata:", meta_train[0])