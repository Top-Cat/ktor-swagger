package de.nielsfalk.ktor.swagger.sample

import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class ApplicationTest {

    @Test
    fun `start and then imidiately shutdown`() {
        run(8090, wait = false).stop(
            5.seconds.inWholeMilliseconds,
            5.seconds.inWholeMilliseconds
        )
    }
}
