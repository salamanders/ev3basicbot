package info.benjaminhill.basicbot

import kotlinx.coroutines.*
import lejos.hardware.Battery
import lejos.hardware.motor.EV3LargeRegulatedMotor
import lejos.hardware.port.MotorPort
import lejos.hardware.port.SensorPort
import lejos.hardware.sensor.EV3GyroSensor
import lejos.hardware.sensor.EV3IRSensor
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

/**
 * Control your basic 2 wheeled bot with a range sensor and a gyro
 * TODO: Tacho to Gyro calibration to turn precise degrees.
 */
class BasicBot : AutoCloseable, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private val motorLeft = EV3LargeRegulatedMotor(MotorPort.A)
    private val motorRight = EV3LargeRegulatedMotor(MotorPort.D)

    val distanceSensor: SmoothSensor
    val angleSensor: SmoothSensor

    init {
        LOG.info { "charge: ${Battery.getVoltageMilliVolt()}" }
        (motorLeft.maxSpeed * SLOWDOWN).let { slowSpeed ->
            motorLeft.setSpeed(slowSpeed)
            motorRight.setSpeed(slowSpeed)
        }
        distanceSensor = EV3IRSensor(SensorPort.S1).let { device ->
            SmoothSensor(device, device.distanceMode)
        }
        LOG.info { "Start Distance: ${distanceSensor.get()}" }

        angleSensor = EV3GyroSensor(SensorPort.S4).let { device ->
            SmoothSensor(device, device.angleMode)
        }
        LOG.info { "Start Angle: ${angleSensor.get()}" }

        LOG.info { "All sensors created." }
    }


    /**
     * Turning left increases angle.
     * TODO: Sense gyro position
     */
    suspend fun turnLeft(relativeDeg: Int) {
        require(relativeDeg >= 0)

        val startDeg = angleSensor.get()
        val targetDeg = startDeg + relativeDeg

        var steps = 0
        motorLeft.backward()
        motorRight.forward()

        while (angleSensor.get() < targetDeg) {
            steps++
            delay(100)
        }
        motorLeft.flt(true)
        motorRight.flt(true)

        LOG.debug {
            val endDeg = angleSensor.get()
            "Rotated: steps:$steps, start:${Math.round(startDeg)}, delta:$relativeDeg, end:${Math.round(endDeg)}, miss:${Math.round(
                startDeg + relativeDeg - endDeg
            )}"
        }
    }

    /**
     * TODO: Combine with generic "turn"
     */
    suspend fun turnRight(relativeDeg: Int) {
        require(relativeDeg >= 0)

        val startDeg = angleSensor.get()
        val targetDeg = startDeg - relativeDeg

        var steps = 0
        motorLeft.forward()
        motorRight.backward()

        while (angleSensor.get() > targetDeg) {
            steps++
            delay(100)
        }
        motorLeft.flt(true)
        motorRight.flt(true)

        LOG.debug {
            val endDeg = angleSensor.get()
            "Rotated: steps:$steps, start:${Math.round(startDeg)}, delta:$relativeDeg, end:${Math.round(endDeg)}, miss:${Math.round(
                startDeg + relativeDeg - endDeg
            )}"
        }
    }

    override fun close() {
        motorLeft.close()
        motorRight.close()
        job.cancelChildren()
        distanceSensor.close()
        angleSensor.close()
        job.cancel()
        LOG.debug { "Bot closed smoothly." }
    }

    override fun toString(): String =
        "Bot: dist:${distanceSensor.get()}, angleSensor:${Math.round(angleSensor.get())}"

    companion object {
        private val LOG = KotlinLogging.logger {}
        private const val SLOWDOWN = 0.1f
    }
}