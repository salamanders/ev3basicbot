package info.benjaminhill.basicbot.ev3

import kotlinx.coroutines.Runnable
import lejos.hardware.port.MotorPort
import lejos.hardware.port.Port
import lejos.hardware.port.SensorPort
import mu.KLoggable

/**
 * @param sensorModeStr how to know you are looking in the right sysfs
 */
abstract class FastSensor<out T : Number>(
    sensorModeStr: String
) : Runnable {

    private val sensorDir = findDirectory(
        mustContain = sensorModeStr,
        globFilePattern = "glob:**mode"
    ).also {
        logger.info { "Found $sensorModeStr at path $it" }
    }

    private val port = addressToPort(sensorDir.resolve("address").toFile().readText().trim())

    internal val valueReader = SysfsReader(sensorDir.resolve("value0"))

    abstract fun setMode(port: Port)
    abstract fun read(): T?

    override fun run() {
        setMode(port)
    }

    companion object : KLoggable {
        override val logger = logger()

        protected fun addressToPort(address: String) = when (address) {
            "ev3-ports:in1" -> SensorPort.S1!!
            "ev3-ports:in2" -> SensorPort.S2!!
            "ev3-ports:in3" -> SensorPort.S3!!
            "ev3-ports:in4" -> SensorPort.S4!!
            "ev3-ports:outA" -> MotorPort.A!!
            "ev3-ports:outB" -> MotorPort.B!!
            "ev3-ports:outC" -> MotorPort.C!!
            "ev3-ports:outD" -> MotorPort.D!!
            else -> error("Invalid Port Address: $address")
        }
    }
}

