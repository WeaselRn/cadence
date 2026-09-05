package com.cadence.gaitradar.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AssessmentEntity::class], version = 1, exportSchema = false)
abstract class CadenceDatabase : RoomDatabase() {
    abstract fun assessmentDao(): AssessmentDao

    companion object {
        const val DATABASE_NAME = "cadence_assessment_db"
    }
}
