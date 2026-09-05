package com.cadence.gaitradar.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCadenceDatabase(
        @ApplicationContext context: Context
    ): CadenceDatabase {
        return Room.databaseBuilder(
            context,
            CadenceDatabase::class.java,
            CadenceDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideAssessmentDao(
        database: CadenceDatabase
    ): AssessmentDao {
        return database.assessmentDao()
    }
}
