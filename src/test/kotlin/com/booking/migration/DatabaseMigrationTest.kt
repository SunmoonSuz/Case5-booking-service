package com.booking.migration

import com.booking.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class DatabaseMigrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `schema migration applies extension and all tables successfully`() {
        // Check btree_gist extension
        val extensionCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'",
            Int::class.java
        )
        assertThat(extensionCount).isEqualTo(1)

        // Check tables exist
        val tables = listOf("users", "masters", "services", "master_services", "master_schedules", "bookings")
        for (table in tables) {
            val count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Int::class.java,
                table
            )
            assertThat(count).`as`("Table $table should exist").isEqualTo(1)
        }

        // Check exclusion constraint exists
        val constraintCount = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM pg_constraint
            WHERE conname = 'no_overlapping_active_bookings' AND contype = 'x'
            """.trimIndent(),
            Int::class.java
        )
        assertThat(constraintCount).`as`("Exclusion constraint should exist").isEqualTo(1)

        // Check chk_users_role check constraint exists
        val roleCheckCount = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM pg_constraint
            WHERE conname = 'chk_users_role' AND contype = 'c'
            """.trimIndent(),
            Int::class.java
        )
        assertThat(roleCheckCount).`as`("Role check constraint should exist").isEqualTo(1)
    }
}
