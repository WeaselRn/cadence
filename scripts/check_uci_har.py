import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from src.real_data.uci_har_loader import extract_regular_anchor_windows

UCI_HAR_ROOT = r"A:\UCI HAR Dataset"  

X_train_real, meta_train = extract_regular_anchor_windows(UCI_HAR_ROOT, split="train")
X_test_real, meta_test = extract_regular_anchor_windows(UCI_HAR_ROOT, split="test")

print("train real anchors:", X_train_real.shape)
print("test real anchors:", X_test_real.shape)
if len(meta_train) > 0:
    print("sample metadata:", meta_train[0])