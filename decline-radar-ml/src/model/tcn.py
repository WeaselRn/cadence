"""
1D Temporal Convolutional Network (PRD section 5).

Input: (Sequence_Length, 6) raw IMU window (accel xyz, gyro xyz)
Output: single sigmoid neuron -> P(perturbed/atypical gait pattern).

IMPORTANT label direction: this model is trained on the FLIPPED target
relative to src/synthetic/generator.py's own label convention.
generator.py: label 1 = regular, label 0 = perturbed (see
docs/tcn_target_definition.md). This model's sigmoid output represents
P(perturbed), i.e. y_tcn = 1 - y_generator, so that PRD section 9's
"the TCN probability is inverted and scaled" reads correctly:
    mobility_stability_score = round((1 - model_output) * 100)
The label flip happens in the training script, not here or in the
generator — this file only defines the architecture.

Architecture:
- 3 TCN blocks, causal 1D convolutions, dilation rates 1, 2, 4
- Global Average Pooling (1D)
- Dense(1, sigmoid)

Kept intentionally small/edge-friendly: this needs to run as a quantized
.tflite model on-device. (~11.7K params as configured by default.)
"""

import tensorflow as tf
from tensorflow.keras import layers, models


def build_tcn(sequence_length: int = 1500, n_features: int = 6,
              n_filters: int = 32, kernel_size: int = 5,
              dilations=(1, 2, 4), dropout: float = 0.2) -> tf.keras.Model:
    inputs = layers.Input(shape=(sequence_length, n_features), name="imu_window")

    x = inputs
    for i, dilation_rate in enumerate(dilations):
        x = layers.Conv1D(
            filters=n_filters,
            kernel_size=kernel_size,
            dilation_rate=dilation_rate,
            padding="causal",
            activation="relu",
            name=f"tcn_block_{i+1}_conv",
        )(x)
        x = layers.BatchNormalization(name=f"tcn_block_{i+1}_bn")(x)
        x = layers.Dropout(dropout, name=f"tcn_block_{i+1}_dropout")(x)

    x = layers.GlobalAveragePooling1D(name="global_avg_pool")(x)
    outputs = layers.Dense(1, activation="sigmoid", name="movement_pattern_probability")(x)

    model = models.Model(inputs=inputs, outputs=outputs, name="gait_tcn")
    return model


def compile_model(model: tf.keras.Model, learning_rate: float = 1e-3) -> tf.keras.Model:
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss="binary_crossentropy",
        metrics=["accuracy", tf.keras.metrics.AUC(name="auc")],
    )
    return model