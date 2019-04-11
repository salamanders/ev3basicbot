package info.benjaminhill.stats

import java.util.concurrent.ThreadLocalRandom

/**
 * From https://geekprompt.github.io/Java-implementation-for-Double-Exponential-Smoothing-for-time-series-with-linear-trend/
 *
 * Performs double exponential smoothing for given time series.
 * This method is suitable for fitting series with linear trend.
 *
 * TODO: Auto-calibrate alpha, beta.
 * TODO: How much historical data before you are doing extra work for no gain, especially if rebuilding every sample?
 *
 * @param data  An array containing the recorded data of the time series
 * @param alpha Smoothing factor for data. Closer to 1 means bias more from recent values.
 * @param beta  Smoothing factor for trend.  Closer to 1 means trend decays faster
 *
 * @return Instance of model that can be used to get current smoothed value and forecast future values
 */
class DoubleExponentialSmoothing(private var data: FloatArray, alpha: Float = 0.8f, beta: Float = 0.2f) {

    val smoothedData = FloatArray(data.size)
    private val trend = FloatArray(data.size + 1)
    private val level = FloatArray(data.size + 1)

    init {
        require(alpha in 0.0..1.0)
        require(beta in 0.0..1.0)

        smoothedData[0] = data[0]
        trend[0] = data[1] - data[0]
        level[0] = data[0]

        for (t in data.indices) {
            smoothedData[t] = trend[t] + level[t]
            level[t + 1] = alpha * data[t] + (1 - alpha) * (level[t] + trend[t])
            trend[t + 1] = beta * (level[t + 1] - level[t]) + (1 - beta) * trend[t]
        }
    }

    /**
     * Forecasts future values.
     *
     * @param size no of future values that you need to forecast
     * @return forecast data
     */
    fun forecast(size: Int = 5) = FloatArray(size) { i ->
        level[level.size - 1] + (i + 1) * trend[trend.size - 1]
    }

    fun getSmoothedLatest() = smoothedData.last()

    fun getSSError() = data.indices.sumByDouble { (smoothedData[it] - data[it]).let { err -> err * err }.toDouble() }

    companion object {
        /** Grid search, slow as heck. */
        fun getBestAlphaBeta(data: FloatArray): Pair<Float, Float> {
            val granularity = 20
            var leastError = Double.MAX_VALUE
            var leastAB = Pair(0.0f, 0.0f)
            for (a in 0 until granularity) {
                for (b in 0 until granularity) {
                    val testAB = Pair(a / granularity.toFloat(), b / granularity.toFloat())
                    val des = DoubleExponentialSmoothing(data, testAB.first, testAB.second)
                    des.getSSError().let {
                        if (it < leastError) {
                            leastError = it
                            leastAB = testAB
                        }
                    }
                }
            }
            return leastAB
        }
    }
}

const val TRAINING_DATA_SIZE = 500
const val REAL_DATA_SIZE = 85
const val LESS_RANDOM = 0.3
fun main() {
    val rnd = ThreadLocalRandom.current()!!
    val trainingData = FloatArray(TRAINING_DATA_SIZE) {
        (Math.cos(it / (Math.PI * 2)) + rnd.nextGaussian() * LESS_RANDOM).toFloat()
    }
    val (alpha, beta) = DoubleExponentialSmoothing.getBestAlphaBeta(trainingData)
    println("alpha:$alpha, beta:$beta")

    val realData = FloatArray(REAL_DATA_SIZE) {
        (Math.cos((it + TRAINING_DATA_SIZE) / (Math.PI * 2)) + rnd.nextGaussian() * LESS_RANDOM).toFloat()
    }

    val model = DoubleExponentialSmoothing(
        data = realData,
        alpha = alpha,
        beta = beta
    )

    println("Sum of squared error: " + model.getSSError())

    // Print in sheets-friendly view
    println(listOf("Step", "Input", "Smoothed", "Forecast").joinToString("\t"))
    realData.mapIndexed { i, input ->
        println(listOf(i, input, model.smoothedData[i]).joinToString("\t"))
    }
    model.forecast(15).mapIndexed { i, forecast ->
        println(listOf(i + realData.size, "", "", forecast).joinToString("\t"))
    }
}
