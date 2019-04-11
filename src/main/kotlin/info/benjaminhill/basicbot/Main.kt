package info.benjaminhill.basicbot

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

fun main() = runBlocking {
    LOG.info { "debug:${LOG.isDebugEnabled}" }

    BasicBot().use { bot ->
        LOG.info { "Time to dance." }


        repeat(3) {
            LOG.info { "Before rotation: $bot" }
            bot.distanceSensor.startRecording()
            bot.turnLeft(360)
            val distances = bot.distanceSensor.stopRecording()
            println(distances.joinToString())
            bot.turnRight(90)
            LOG.info { "After rotation: $bot" }
        }

    }
    LOG.info { "main: exiting app." }
}

