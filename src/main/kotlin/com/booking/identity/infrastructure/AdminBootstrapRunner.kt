package com.booking.identity.infrastructure

import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import com.booking.shared.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminBootstrapRunner(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val appProperties: AppProperties
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(AdminBootstrapRunner::class.java)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        val bootstrapEmail = appProperties.bootstrapAdmin.email.trim().lowercase()
        val bootstrapPassword = appProperties.bootstrapAdmin.password

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin user already exists. Skipping bootstrap initialization.")
            return
        }

        if (userRepository.existsByEmailIgnoreCase(bootstrapEmail)) {
            log.info("User with email {} already exists. Skipping bootstrap admin initialization.", bootstrapEmail)
            return
        }

        val admin = User(
            email = bootstrapEmail,
            passwordHash = passwordEncoder.encode(bootstrapPassword),
            role = Role.ADMIN,
            name = "Administrator",
            phone = "+70000000000"
        )

        userRepository.save(admin)
        log.info("Bootstrap admin user created with email: {}", bootstrapEmail)
    }
}
