PHASE 3 — REAL IMU COLLECTION & MODEL-READY SESSION
You are continuing the existing Android project Gait Functional Decline Radar. Phases 0, 1, and 2 are complete and working. Implement ONLY Phase 3 in this phase.
PRIMARY OBJECTIVE
Connect the existing assessment UI to Android SensorManager and produce a timestamped six-axis ImuSession for later ML preprocessing. The mobile app uses sensors only; there is no camera, video, CameraX, or MediaPipe.
END-TO-END CONTRACT
Home → Assessment Introduction → Sensor Readiness → 30-second Collection → ImuSession → Phase 4 Quality Gate.
SENSOR INPUT
Collect accelerometer X/Y/Z and gyroscope X/Y/Z using SensorManager. Request approximately 50 Hz. Do not assume Android delivers exactly 50 Hz.
SESSION MODEL
ImuSample retains timestamp, accelX/Y/Z and gyroX/Y/Z. ImuSession contains a unique session ID, start/end timestamps, duration, samples, counts, and observed sampling information.
SYNCHRONIZATION
Sensor streams do not necessarily arrive simultaneously. Retain timestamps and create a deterministic temporally aligned six-axis representation. Do not pair array indices blindly. Keep the strategy practical for the MVP.
MODEL-READINESS REQUIREMENT
Preserve raw Android sensor coordinate values in the six-channel order ax, ay, az, gx, gy, gz. Do not normalize, gravity-remove, convert to magnitude, or replace the ML sequence with Phase 5 metrics. ML preprocessing happens in Phase 6.
COLLECTION WINDOW
Collect approximately 30 seconds. The eventual model window is 1500 frames at 50 Hz, but raw collection does not need exactly 1500 events. Retain enough timestamped data for Phase 6 to construct the fixed 1500 × 6 input.
SAFETY AND LIFECYCLE
Show pocket/walking instructions, readiness, timer, collecting state, and Stop. Unregister listeners on completion, cancellation, lifecycle interruption, and errors. Never leave sensors running indefinitely.
VERIFICATION
On Pixel 9 verify both sensors deliver data, timestamps increase, duration is approximately 30 seconds, each attempt uses a fresh buffer/session ID, listeners are released, and an ImuSession is produced. Do not run ML in Phase 3.
