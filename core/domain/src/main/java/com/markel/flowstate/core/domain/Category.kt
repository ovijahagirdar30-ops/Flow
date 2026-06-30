package com.markel.flowstate.core.domain

data class Category(
    val id: Int = 0,
    val name: String,
    val position: Int = 0
) {
    companion object {
        /**
         * The fixed id of the "General" category — the default category that
         * every task, idea and checklist belongs to when no user category is
         * assigned (or when categories are disabled).
         *
         * This row is inserted by MIGRATION_18_19
         * and can never be deleted by the user.
         */
        const val GENERAL_ID = 1
    }
}