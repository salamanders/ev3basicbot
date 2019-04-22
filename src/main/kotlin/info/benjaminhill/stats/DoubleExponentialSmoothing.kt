package info.benjaminhill.stats

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
            // Hard reset (NaN from a bad read, or sample0)
            data = sample
            smoothedData = data
            trend = Float.NaN
            return
        }
        if (trend.isNaN()) {
            // next sample after a reset (still rebooting)
            trend = sample - data
            level = data
            data = sample
            return
        }
        // Normal operations
        smoothedData = trend + level
        val nextLevel = alpha * data + (1 - alpha) * (level + trend)
        trend = beta * (nextLevel - level) + (1 - beta) * trend
        level = nextLevel
        // (data is updated last, not sure if that is good or bad...)
        data = sample
    }

    /**
     * @param pctBetweenSamples how long in percent since the last sample.
     * Default to predict what is about to happen.  Set to pct since last time step.
     */
    fun get(pctBetweenSamples: Float = 1f): Float =
        (1 - pctBetweenSamples) * smoothedData + pctBetweenSamples * getNextPredicted()

    private fun getNextPredicted() = level + trend

    fun getSquaredError() = (smoothedData - data).let { it * it }
}

