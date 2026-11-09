package com.booking.shared.config

import com.booking.booking.domain.Booking
import com.booking.booking.domain.BookingStatus
import com.booking.booking.infrastructure.BookingRepository
import com.booking.catalog.domain.Master
import com.booking.catalog.domain.Service
import com.booking.catalog.infrastructure.MasterRepository
import com.booking.catalog.infrastructure.ServiceRepository
import com.booking.identity.domain.Role
import com.booking.identity.domain.User
import com.booking.identity.infrastructure.UserRepository
import com.booking.schedule.domain.MasterSchedule
import com.booking.schedule.infrastructure.MasterScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Автоматическая инициализация демонстрационных данных для проверки прототипа
 * экспертной комиссией форума «Умная индустрия» (Кейс №5 «Сервис записи на услуги»).
 *
 * ВАЖНОЕ ПРЕДУПРЕЖДЕНИЕ (Этическая и юридическая оговорка):
 * Все создаваемые записи пользователей, имена специалистов, телефоны (+7900000000X)
 * и email-адреса (@booking.local) являются полностью синтетическими учебными данными,
 * не содержат персональных данных третьих лиц и предназначены исключительно для демонстрации.
 */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.demo-seed", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class DemoDataBootstrapRunner(
    private val userRepository: UserRepository,
    private val serviceRepository: ServiceRepository,
    private val masterRepository: MasterRepository,
    private val masterScheduleRepository: MasterScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val passwordEncoder: PasswordEncoder,
    private val appProperties: AppProperties
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DemoDataBootstrapRunner::class.java)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (serviceRepository.count() > 0 || masterRepository.count() > 0) {
            log.info("Demo catalog data already exists. Skipping demo seeding.")
            return
        }

        log.info("Initializing synthetic demo data for Case #5 (Service Booking)...")

        // 1. Тестовые учётные записи клиентов (для проверки сценария бронирования и ЛК)
        val client1 = ensureUser(
            email = "client@booking.local",
            rawPassword = "client12345",
            role = Role.CLIENT,
            name = "Иван Петров (Тестовый клиент)",
            phone = "+79000000001"
        )
        val client2 = ensureUser(
            email = "maria@booking.local",
            rawPassword = "client12345",
            role = Role.CLIENT,
            name = "Мария Соколова (Тестовый клиент)",
            phone = "+79000000002"
        )

        // 2. Каталог услуг (длительности 30 и 60 минут согласно бизнес-правилам)
        val srvHaircut = serviceRepository.save(
            Service(
                name = "Мужская стрижка и укладка",
                description = "Классическая или современная стрижка с мытьём головы и укладкой премиальным стайлингом.",
                durationMinutes = 60,
                price = BigDecimal("1800.00"),
                isActive = true
            )
        )
        val srvBeard = serviceRepository.save(
            Service(
                name = "Моделирование бороды и усов",
                description = "Оформление контуров опасной бритвой, распаривание горячим полотенцем и уход маслом.",
                durationMinutes = 30,
                price = BigDecimal("1100.00"),
                isActive = true
            )
        )
        val srvCombo = serviceRepository.save(
            Service(
                name = "Комплекс: Стрижка + Борода",
                description = "Полный комплекс ухода: стрижка, моделирование бороды, спа-уход за кожей лица.",
                durationMinutes = 60,
                price = BigDecimal("2500.00"),
                isActive = true
            )
        )
        val srvStyling = serviceRepository.save(
            Service(
                name = "Экспресс-укладка и уход",
                description = "Подготовка образа к мероприятию, мытьё головы, массаж и текстурная укладка.",
                durationMinutes = 30,
                price = BigDecimal("900.00"),
                isActive = true
            )
        )
        val srvKids = serviceRepository.save(
            Service(
                name = "Детская стрижка (до 12 лет)",
                description = "Бережная и аккуратная модельная стрижка для юных гостей.",
                durationMinutes = 30,
                price = BigDecimal("1200.00"),
                isActive = true
            )
        )

        // 3. Специалисты (мастера) с привязкой к услугам
        val masterAlexey = masterRepository.save(
            Master(
                name = "Алексей Громов",
                description = "Топ-барбер, стаж 8 лет. Специализация: сложные фейды, классические стрижки и королевское бритьё.",
                isActive = true,
                services = mutableSetOf(srvHaircut, srvBeard, srvCombo, srvStyling, srvKids)
            )
        )
        val masterDmitry = masterRepository.save(
            Master(
                name = "Дмитрий Волков",
                description = "Старший мастер, стаж 5 лет. Эксперт по моделированию бороды и мужским текстурным стрижкам.",
                isActive = true,
                services = mutableSetOf(srvHaircut, srvBeard, srvCombo, srvKids)
            )
        )
        val masterElena = masterRepository.save(
            Master(
                name = "Елена Смирнова",
                description = "Стилист-модельер, стаж 6 лет. Удлинённые формы, креативные укладки и детские стрижки.",
                isActive = true,
                services = mutableSetOf(srvHaircut, srvStyling, srvKids)
            )
        )

        // 4. Расписание мастеров на все дни недели (Пн..Вс: 10:00-14:00 и 15:00-20:00, перерыв 14:00-15:00)
        val allMasters = listOf(masterAlexey, masterDmitry, masterElena)
        for (master in allMasters) {
            for (dayOfWeek in 1..7) {
                masterScheduleRepository.save(
                    MasterSchedule(
                        master = master,
                        dayOfWeek = dayOfWeek,
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(14, 0)
                    )
                )
                masterScheduleRepository.save(
                    MasterSchedule(
                        master = master,
                        dayOfWeek = dayOfWeek,
                        startTime = LocalTime.of(15, 0),
                        endTime = LocalTime.of(20, 0)
                    )
                )
            }
        }

        // 5. Демонстрационные записи (активные будущие и архивные)
        val zoneId = ZoneId.of(appProperties.timezone)
        val today = ZonedDateTime.now(zoneId).toLocalDate()

        val tomorrow11 = today.plusDays(1).atTime(11, 0).atZone(zoneId).toInstant()
        val tomorrow12 = today.plusDays(1).atTime(12, 0).atZone(zoneId).toInstant()
        bookingRepository.save(
            Booking(
                client = client1,
                master = masterAlexey,
                service = srvCombo,
                serviceNameSnapshot = srvCombo.name,
                servicePriceSnapshot = srvCombo.price,
                startTime = tomorrow11,
                endTime = tomorrow12,
                status = BookingStatus.CONFIRMED
            )
        )

        val tomorrow16 = today.plusDays(1).atTime(16, 0).atZone(zoneId).toInstant()
        val tomorrow1630 = today.plusDays(1).atTime(16, 30).atZone(zoneId).toInstant()
        bookingRepository.save(
            Booking(
                client = client1,
                master = masterDmitry,
                service = srvBeard,
                serviceNameSnapshot = srvBeard.name,
                servicePriceSnapshot = srvBeard.price,
                startTime = tomorrow16,
                endTime = tomorrow1630,
                status = BookingStatus.PENDING
            )
        )

        val afterTomorrow12 = today.plusDays(2).atTime(12, 0).atZone(zoneId).toInstant()
        val afterTomorrow13 = today.plusDays(2).atTime(13, 0).atZone(zoneId).toInstant()
        bookingRepository.save(
            Booking(
                client = client2,
                master = masterElena,
                service = srvHaircut,
                serviceNameSnapshot = srvHaircut.name,
                servicePriceSnapshot = srvHaircut.price,
                startTime = afterTomorrow12,
                endTime = afterTomorrow13,
                status = BookingStatus.PENDING
            )
        )

        val yesterday11 = today.minusDays(1).atTime(11, 0).atZone(zoneId).toInstant()
        val yesterday12 = today.minusDays(1).atTime(12, 0).atZone(zoneId).toInstant()
        bookingRepository.save(
            Booking(
                client = client1,
                master = masterAlexey,
                service = srvHaircut,
                serviceNameSnapshot = srvHaircut.name,
                servicePriceSnapshot = srvHaircut.price,
                startTime = yesterday11,
                endTime = yesterday12,
                status = BookingStatus.COMPLETED
            )
        )

        log.info(
            "Demo data initialized: 5 services, 3 masters, 7-day schedules, 4 sample bookings, and test accounts (client@booking.local / client12345)."
        )
    }

    private fun ensureUser(
        email: String,
        rawPassword: String,
        role: Role,
        name: String,
        phone: String
    ): User {
        val existing = userRepository.findByEmailIgnoreCase(email)
        if (existing.isPresent) {
            return existing.get()
        }
        return userRepository.save(
            User(
                email = email.lowercase(),
                passwordHash = passwordEncoder.encode(rawPassword),
                role = role,
                name = name,
                phone = phone
            )
        )
    }
}
