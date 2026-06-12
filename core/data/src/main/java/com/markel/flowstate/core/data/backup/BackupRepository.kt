package com.markel.flowstate.core.data.backup

/**
 * Contract for the backup/restore operations.
 *
 * The export produces a JSON string containing all user data; the restore
 * reads that JSON string back and merges the data into the database
 * (additive / upsert strategy — existing records with the same PK are
 * overwritten, everything else is preserved).
 */
interface BackupRepository {

    /** Serialises all user data to a pretty-printed JSON string. */
    suspend fun exportToJson(): String

    /**
     * Parses [json] and upserts every entity into the database.
     *
     * @return [RestoreResult.Success] on a clean restore, or
     *         [RestoreResult.Error] with the specific failure reason.
     */
    suspend fun restoreFromJson(json: String): RestoreResult
}

/** Outcome of a restore operation. */
sealed class RestoreResult {
    data object Success : RestoreResult()
    data class Error(val type: RestoreErrorType) : RestoreResult()
}

/** Specific reason a restore failed. */
enum class RestoreErrorType {
    /** The file is not valid JSON or does not match the expected schema. */
    INVALID_FILE,

    /** The backup was created with a different database schema version. */
    SCHEMA_MISMATCH,

    /** An unexpected error occurred (I/O, database, etc.). */
    UNKNOWN
}