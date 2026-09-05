package com.cadence.gaitradar.core.di

import com.cadence.gaitradar.core.baseline.PersonalBaselineEngine
import com.cadence.gaitradar.core.baseline.PersonalBaselineEngineImpl
import com.cadence.gaitradar.core.metrics.GaitMetricsEngine
import com.cadence.gaitradar.core.metrics.GaitMetricsEngineImpl
import com.cadence.gaitradar.core.quality.ImuQualityGate
import com.cadence.gaitradar.core.quality.ImuQualityGateImpl
import com.cadence.gaitradar.core.sensors.SensorCollector
import com.cadence.gaitradar.core.sensors.SensorCollectorImpl
import com.cadence.gaitradar.ml.MlInferenceAdapter
import com.cadence.gaitradar.ml.TfliteMlInferenceAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSensorCollector(
        sensorCollectorImpl: SensorCollectorImpl
    ): SensorCollector

    @Binds
    @Singleton
    abstract fun bindImuQualityGate(
        imuQualityGateImpl: ImuQualityGateImpl
    ): ImuQualityGate

    @Binds
    @Singleton
    abstract fun bindGaitMetricsEngine(
        gaitMetricsEngineImpl: GaitMetricsEngineImpl
    ): GaitMetricsEngine

    @Binds
    @Singleton
    abstract fun bindMlInferenceAdapter(
        tfliteMlInferenceAdapter: TfliteMlInferenceAdapter
    ): MlInferenceAdapter

    @Binds
    @Singleton
    abstract fun bindPersonalBaselineEngine(
        personalBaselineEngineImpl: PersonalBaselineEngineImpl
    ): PersonalBaselineEngine
}
