package com.markel.flowstate.core.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * This is the main database class.
 * It tells Room which "Entities" (tables) it should be aware of
 * and which version of the database we are using.
 */
@Database(
    entities = [TaskEntity::class, SubTaskEntity::class], // List of all tables
    version = 6,
    exportSchema = true
)
abstract class FlowStateDatabase : RoomDatabase() {

    // Exposes our DAO so the rest of the app can use it
    abstract val taskDao: TaskDao

    // Room will use this to create the DB instance.
    companion object {
        const val DATABASE_NAME = "flowstate_db"
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE subtasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
            }
        }
    }
}