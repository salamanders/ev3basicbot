package info.benjaminhill.basicbot

import kotlinx.coroutines.*
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
class BasicBot : AutoCloseable, Runnable, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    // B, C is standard
    private val motorLeft = EV3LargeRegulatedMotor(MotorPort.B)
    private val motorRight = EV3LargeRegulatedMotor(MotorPort.C)

    private val distanceSensor = EV3IRSensor(SensorPort.S4)
    private val distanceSampleProvider = distanceSensor.distanceMode

    private val gyroSensor = EV3GyroSensor(SensorPort.S2)
    private val gyroSampleProvider = gyroSensor.angleAndRateMode

    private var isRunning = true
    private val history = mutableListOf<BotState>()

    private var targetGyro = 0f

    fun getHistory(): List<BotState> {
        val results = history.toList()
        history.clear()
        return results
    }

    /** Launch a tight running loop */
    override fun run() {
        launch {
            while (isRunning) {
                val now = System.currentTimeMillis()
                val state = BotState.build(distanceSampleProvider, gyroSampleProvider, motorLeft, motorRight)
                history.add(state)
                // Update all commands
                motorCommands(state)
            }
        }
    }

    private fun motorCommands(state: BotState) {
        if (Math.abs(targetGyro - state.angle) > 10) {
            if (state.angle < targetGyro) {
                // Turn Right
                motorLeft.forward()
                motorRight.backward()
            } else {
                // Turn Left
                motorLeft.backward()
                motorRight.forward()
            }
        } else {
            motorLeft.flt(true)
            motorRight.flt(true)
        }
    }

    fun stop() {
        this.isRunning = false
    }

    /**
     * TODO: Which way does angle increase? (left or right?)
     */
    fun turn(relativeDeg: Int) {
        targetGyro += relativeDeg
    }

    override fun close() {
        motorLeft.close()
        motorRight.close()

        distanceSensor.close()
        gyroSensor.close()

        job.cancelChildren()
        job.cancel()
        LOG.debug { "Bot closed smoothly." }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}