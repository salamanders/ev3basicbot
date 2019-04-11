package info.benjaminhill.basicbot

import info.benjaminhill.stats.DoubleExponentialSmoothing
import kotlinx.coroutines.*
import lejos.hardware.sensor.BaseSensor
import lejos.robotics.SampleProvider
import mu.KotlinLogging
import org.apache.commons.collections4.queue.CircularFifoQueue
import kotlin.coroutines.CoroutineContext

/**
 *
 * Use DoubleExponentialSmoothing to get "better" samples without too much CPU overhead.
 * Always be sampling to stay up-to-date, throwing values into a Ring Buffer.
 * If not enough history yet, will return most recent sample.
 *
 * @see <a href="http://www.lejos.org/ev3/docs/index.html?lejos/hardware/sensor/package-summary.html">LeJOS Sensor API</a>
 */
class SmoothSensor(private val sensor: BaseSensor, private val mode: SampleProvider) : AutoCloseable, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private val sample = FloatArray(mode.sampleSize())
    private val sampleHistory = CircularFifoQueue<Float>(HISTORY_SIZE)
    private var lastSampleMs = 0L
    private var sampleCount = 0
    private var directSampleCount = 0
    private var smoothSampleCount = 0
    private var totalElapsedMs = 0L
    private val recording = mutableListOf<Float>()
    private var isRecording = false

    init {
        launch {
            while (job.isActive) {
                mode.fetchSample(sample, 0)
                if (sample[0].isFinite()) {
                    lastSampleMs = System.currentTimeMillis()
                    sampleHistory.add(sample[0]) // never overflows, yay circular!
                    sampleCount++

                    if (isRecording) {
                        recording.add(sample[0])
                    }
                }
                delay(SAMPLE_DELAY_MS)
            }
        }
    }


    fun startRecording() {
        isRecording = true
    }

    fun stopRecording(): List<Float> {
        isRecording = false
        val results = recording.toList()
        recording.clear()
        return results
    }

    /**
     * If not enough history, return last raw sample
     * Else find the point between last smoothed sample and estimated future to get "now"
     */
    fun get(): Float = if (sampleCount < HISTORY_SIZE) {
        directSampleCount++
        sampleHistory.last()
    } else {
        val startMs = System.currentTimeMillis()
        val smoother = DoubleExponentialSmoothing(sampleHistory.toFloatArray())
        val pctToNextSample = Math.min(1.0f, (startMs - lastSampleMs) / SAMPLE_DELAY_MS.toFloat())
        val averagedToFuture =
            smoother.forecast(1)[0] * pctToNextSample + smoother.getSmoothedLatest() * (1 - pctToNextSample)
        smoothSampleCount++

        val elapsedMs = System.currentTimeMillis() - startMs
        totalElapsedMs += elapsedMs

        if (elapsedMs > SAMPLE_DELAY_MS / 2) {
            LOG.warn { "Call to smoothed ${sensor.name}.get() took $elapsedMs ms." }
        }

        averagedToFuture
    }

    override fun close() {
        LOG.debug { "${sensor.name} closing, having proceeded for ms:$totalElapsedMs, sampled $sampleCount, provided direct:$directSampleCount, smoothed:$smoothSampleCount" }
        sensor.close()
    }

    companion object {
        const val SAMPLE_DELAY_MS = 100L
        const val HISTORY_SIZE = 1_000 / SAMPLE_DELAY_MS.toInt() // 1 second worth
        private val LOG = KotlinLogging.logger {}
    }
}