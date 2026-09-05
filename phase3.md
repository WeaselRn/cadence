PHASE 7 — FINAL ASSESSMENT PERSISTENCE WITH ROOM
Implement ONLY Phase 7 after Phase 6 is working.
PRIMARY OBJECTIVE
Persist completed derived assessments locally. Raw IMU samples remain temporary processing input and are not archived in the assessment table unless explicitly required.
FINAL ASSESSMENT RECORD
Store assessment ID, source session ID, timestamp, duration, available physical metrics, Mobility Stability Score, raw model output if needed for reproducibility, model version, algorithm version, and baseline/deviation state when required.
MODEL VERSIONING
Persist the exact model version used. Current committed artifact: 1.0.0. Keep the version centralized rather than hard-coded in multiple places.
NO FAILED RESULTS
Cancelled, quality-failed, preprocessing-failed, or model-failed sessions must not appear as completed assessments in History.
ROOM ARCHITECTURE
Use AssessmentEntity + AssessmentDao + AssessmentRepository behind domain models. UI should not depend directly on Room entities. Keep all data local/offline.
VERIFICATION
Save a successful model-backed assessment, force-close/reopen, verify persistence and History ordering, test deletion, and verify failed assessments are absent.
