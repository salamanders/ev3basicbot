package info.benjaminhill.basicbot

import lejos.hardware.Battery
import lejos.hardware.port.SensorPort
import lejos.hardware.sensor.EV3IRSensor
import lejos.utility.Delay

import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

fun main() {
    LOG.info { "debug:${LOG.isDebugEnabled}" }


    val ir1 = EV3IRSensor(SensorPort.S3)

    println(Battery.getVoltageMilliVolt())

    val sp = ir1.distanceMode

    //Control loop
    for (i in 0..50) {
        val sample = FloatArray(sp.sampleSize())
        sp.fetchSample(sample, 0)
        println("$i: ${sample[0]}")
        Delay.msDelay(500)
    }
}


/*
BasicBot().use { bot ->
    LOG.info { "Time to dance." }
    bot.run()
    bot.turn(360 * 4)

    delay(5 * 1_000)
    bot.stop()

    println(BotState.headerTsv())

    bot.getHistory().sortedBy { it.ms }.forEach { state ->
        println(state.toTsv())
    }
}
LOG.info { "main: exiting app." }
 */


