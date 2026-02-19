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
    entities = [TaskEntity::class, SubTaskEntity::class, IdeaEntity::class, CheckListEntity::class, CheckListItemEntity::class ], // List of all tables
    version = 8,
    exportSchema = true
)
abstract class FlowStateDatabase : RoomDatabase() {

    // Exposes our DAO so the rest of the app can use it
    abstract val taskDao: TaskDao
    abstract val ideaDao: IdeaDao
    abstract val checkListDao: CheckListDao

    // Room will use this to create the DB instance.
    companion object {
        const val DATABASE_NAME = "flowstate_db"
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE subtasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create ideas table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ideas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `content` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `color` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create lists table
                db.execSQL("CREATE TABLE IF NOT EXISTS `checklists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL)")

                // Create list items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `checklist_items` (
                        `id` TEXT NOT NULL, 
                        `listId` INTEGER NOT NULL, 
                        `text` TEXT NOT NULL, 
                        `isDone` INTEGER NOT NULL, 
                        `position` INTEGER NOT NULL,
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`listId`) REFERENCES `checklists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ideas ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}