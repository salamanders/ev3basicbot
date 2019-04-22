package info.benjaminhill.basicbot

import lejos.hardware.Battery
import lejos.robotics.RegulatedMotor
import lejos.robotics.SampleProvider

/** Everything a growing bot should remember */
data class BotState(
    val ms: Long,
    val distance: Float,
    val angle: Float,
    val angleRate: Float,
    val volt: Int,
    val leftTacho: Int,
    val leftSpeed: Int,
    val rightTacho: Int,
    val rightSpeed: Int
) {

    fun header(): String = this::class.members.sortedBy { it.name }.joinToString { it.name }

    fun toTsv(): String =
        listOf(ms, distance, angle, angleRate, volt, leftTacho, leftSpeed, rightTacho, rightSpeed).joinToString("\t")

    companion object {
        val startMs = System.currentTimeMillis()

        fun build(
            distanceSampleProvider: SampleProvider,
            gyroSampleProvider: SampleProvider,
            motorLeft: RegulatedMotor,
            motorRight: RegulatedMotor
        ): BotState {
            val now = System.currentTimeMillis() - startMs
            val temp = FloatArray(2)

            distanceSampleProvider.fetchSample(temp, 0)
            val distance = temp[0]

            gyroSampleProvider.fetchSample(temp, 0)
            val angle = temp[0]
            val angleRate = temp[1]

            return BotState(
                now, distance, angle, angleRate,
                volt = Battery.getVoltageMilliVolt(),
                leftTacho = motorLeft.tachoCount,
                leftSpeed = motorLeft.rotationSpeed,
                rightTacho = motorRight.tachoCount,
                rightSpeed = motorRight.rotationSpeed
            )
        }

        fun headerTsv(): String = listOf(
            "ms",
            "distance",
            "angle",
            "angleRate",
            "volt",
            "leftTacho",
            "leftSpeed",
            "rightTacho",
            "rightSpeed"
        ).joinToString("\t")
    }
}
