package com.booking.identity.infrastructure

import com.booking.AbstractIntegrationTest
import com.booking.identity.domain.Role
import com.booking.shared.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

class AdminBootstrapRunnerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var appProperties: AppProperties

    @Autowired
    private lateinit var runner: AdminBootstrapRunner

    @BeforeEach
    fun cleanDatabase() {
        userRepository.deleteAll()
    }

    @Test
    fun `AdminBootstrapRunner creates admin user on empty database`() {
        runner.run(null)

        val adminOptional = userRepository.findByEmailIgnoreCase(appProperties.bootstrapAdmin.email)
        assertThat(adminOptional).isPresent

        val admin = adminOptional.get()
        assertThat(admin.role).isEqualTo(Role.ADMIN)
        assertThat(admin.email).isEqualTo(appProperties.bootstrapAdmin.email.lowercase())
        assertThat(admin.name).isEqualTo("Administrator")
        assertThat(admin.phone).isEqualTo("+70000000000")
        assertThat(passwordEncoder.matches(appProperties.bootstrapAdmin.password, admin.passwordHash)).isTrue()
    }

    @Test
    fun `AdminBootstrapRunner is idempotent and does not create duplicate user when run again`() {
        runner.run(null)
        val initialCount = userRepository.count()
        assertThat(initialCount).isEqualTo(1)

        // Run second time
        runner.run(null)
        assertThat(userRepository.count()).isEqualTo(1)
    }
}
