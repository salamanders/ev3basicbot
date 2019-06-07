package info.benjaminhill.basicbot.ev3

import ev3dev.sensors.ev3.EV3GyroSensor
import ev3dev.sensors.ev3.EV3UltrasonicSensor
import lejos.hardware.port.Port

class DistanceSensor : FastSensor<Long>(
    sensorModeStr = "US-DIST-CM"
) {
    override fun setMode(port: Port) {
        EV3UltrasonicSensor(port).distanceMode!!
    }

    override fun read() = valueReader.readLong()
}

class GyroSensor : FastSensor<Float>(
    sensorModeStr = "GYRO-ANG"
) {
    override fun setMode(port: Port) {
        logger.debug { "Gyro resetting..." }
        val gyroSensor = EV3GyroSensor(port)
        gyroSensor.reset()
        val angleProvider = gyroSensor.angleAndRateMode!!
        val fa = FloatArray(angleProvider.sampleSize())
        angleProvider.fetchSample(fa, 0)
        gyroSensor.reset()
    }

    override fun read() = valueReader.readFloat()
}

class TachoSensor : FastSensor<Long>(
    sensorModeStr = "tacho-motor"
) {
    override fun setMode(port: Port) {
        // nothing required
    }

    override fun read() = valueReader.readLong()
}