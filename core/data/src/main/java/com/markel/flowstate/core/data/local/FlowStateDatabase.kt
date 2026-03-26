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
    entities = [TaskEntity::class, SubTaskEntity::class, IdeaEntity::class, CheckListEntity::class, CheckListItemEntity::class, GridOrderEntity::class, HabitEntity::class, HabitEntryEntity::class, HabitNumericEntryEntity::class ], // List of all tables
    version = 12,
    exportSchema = true
)
abstract class FlowStateDatabase : RoomDatabase() {

    // Exposes our DAO so the rest of the app can use it
    abstract val taskDao: TaskDao
    abstract val ideaDao: IdeaDao
    abstract val checkListDao: CheckListDao
    abstract val gridOrderDao: GridOrderDao
    abstract val habitDao: HabitDao

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
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `grid_order` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `itemType` TEXT NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE checklists ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL DEFAULT 'self_improvement',
                        `colorArgb` INTEGER NOT NULL DEFAULT -10185078,
                        `frequency` TEXT NOT NULL DEFAULT 'DAILY',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                        db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_entries_habitId` ON `habit_entries` (`habitId`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN habitType TEXT NOT NULL DEFAULT 'BOOLEAN'")
                db.execSQL("ALTER TABLE habits ADD COLUMN unit TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE habits ADD COLUMN targetValue REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE habits ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `habit_numeric_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitId` INTEGER NOT NULL,
                `epochDay` INTEGER NOT NULL,
                `value` REAL NOT NULL,
                FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON DELETE CASCADE
            )
        """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_numeric_entries_habitId` ON `habit_numeric_entries` (`habitId`)")
            }
        }
    }
}