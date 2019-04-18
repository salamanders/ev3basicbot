package info.benjaminhill.stats

import info.benjaminhill.stats.pso.OptimizableFunction
import info.benjaminhill.stats.pso.PSOSwarm
import java.util.concurrent.ThreadLocalRandom

/**
 * Based on https://geekprompt.github.io/Java-implementation-for-Double-Exponential-Smoothing-for-time-series-with-linear-trend/
 *
 * Performs double exponential smoothing for given time series.
 * This method is suitable for fitting series with linear trend.
 *
 * @param alpha Smoothing factor for data. Closer to 1 means bias more from recent values.
 * @param beta Smoothing factor for trend.  Closer to 1 means trend decays faster
 *
 * @return Instance of model that can be used to get current smoothed value and forecast future values
 *
 */
class DoubleExponentialSmoothing(val alpha: Float = 0.3f, val beta: Float = 0.4f) {
    init {
        require(alpha in 0.0..1.0)
        require(beta in 0.0..1.0)
    }

    private var data = Float.NaN
    private var smoothedData = Float.NaN
    private var trend = Float.NaN
    private var level = Float.NaN

    fun add(sample: Float) {
        if (data.isNaN() || !sample.isFinite()) {
            // Reset (NaN from a bad read, or sample0)
            data = sample
            smoothedData = data
            trend = Float.NaN
            return
        }
        if (trend.isNaN()) {
            // sample1 after a reset
            trend = sample - data
            level = data
            data = sample
            return
        }
        smoothedData = trend + level
        val nextLevel = alpha * data + (1 - alpha) * (level + trend)
        trend = beta * (nextLevel - level) + (1 - beta) * trend
        level = nextLevel
        data = sample
    }

    /**
     * @param pctBetweenSamples how long in percent since the last sample.  Default to predict what is about to happen.
     */
    fun get(pctBetweenSamples: Float = 1f): Float =
        (1 - pctBetweenSamples) * smoothedData + pctBetweenSamples * getNextPredicted()

    private fun getNextPredicted() = level + trend
    fun getSquaredError() = (smoothedData - data).let { it * it }
}



fun main() {
    /*
    // Like baymax, grid search is not fast.
    fun gridSearchAlphaBeta(data: FloatArray): Triple<Float, Float, Float> {
        val gridGranularity = 200
        var leastABE = Triple(Float.NaN, Float.NaN, Float.MAX_VALUE)

        for (a in 0 until gridGranularity) {
            for (b in 0 until gridGranularity) {
                val alpha = a / gridGranularity.toFloat()
                val beta = b / gridGranularity.toFloat()
                val des = DoubleExponentialSmoothing(alpha, beta)
                var sumOfSquaredErrors = 0f
                data.forEach { sample ->
                    des.add(sample)
                    des.getSquaredError().let {
                        if (it.isFinite()) {
                            sumOfSquaredErrors += it
                        }
                    }
                }

                if (sumOfSquaredErrors < leastABE.third) {
                    leastABE = Triple(alpha, beta, sumOfSquaredErrors)
                }

            }
        }
        return leastABE
    }
    */


    fun psoAlphaBeta(data: FloatArray): Pair<Float, Float> {

        val f = OptimizableFunction(
            customDomains = arrayOf(
                (0.0).rangeTo(1.0),
                (0.0).rangeTo(1.0)
            )
        ) {
            // Unsure if clamping here will mess with results
            val alpha = Math.max(0.0, Math.min(1.0, it[0])).toFloat()
            val beta = Math.max(0.0, Math.min(1.0, it[1])).toFloat()
            val des = DoubleExponentialSmoothing(alpha, beta)
            var sumOfSquaredErrors = 0f
            data.forEach { sample ->
                des.add(sample)
                des.getSquaredError().let { sqError ->
                    if (sqError.isFinite()) {
                        sumOfSquaredErrors += sqError
                    }
                }
            }
            sumOfSquaredErrors.toDouble()
        }

        val pso = PSOSwarm(
            function = f,
            numOfParticles = 200,
            smallestMovePct = 1E-10
        )
        pso.run()
        return pso.getBest().let { best ->
            Pair(best[0].toFloat(), best[1].toFloat())
        }
    }

    val lessRandom = 0.9
    val rnd = ThreadLocalRandom.current()!!
    val fakeData = FloatArray(100_000) {
        val realY = Math.cos(it / 10.0) * 30
        val distanceBasedRnd = rnd.nextGaussian() * realY * lessRandom
        val universalRnd = rnd.nextGaussian() * lessRandom
        (realY + distanceBasedRnd + universalRnd).toFloat()
    }

    val trainingData = fakeData.sliceArray(0 until fakeData.size - 100)
    val testData = fakeData.sliceArray(fakeData.size - 100 until fakeData.size)

    val (alpha, beta) = psoAlphaBeta(trainingData)
    println("Estimated alpha:$alpha, beta:$beta")

    val bestModel = DoubleExponentialSmoothing(alpha = alpha, beta = beta)
    println("ActualNext\tModelForecast")
    testData.forEach {
        println(
            listOf(
                it,
                bestModel.get(1f)
            ).joinToString("\t")
        )
        bestModel.add(it)
    }

}
