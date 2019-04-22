package info.benjaminhill.stats

import info.benjaminhill.stats.pso.OptimizableFunction
import info.benjaminhill.stats.pso.PSOSwarm
import mu.KotlinLogging


private val LOG = KotlinLogging.logger {}

internal fun main() {

    fun psoAlphaBeta(data: FloatArray): Pair<Float, Float> {

        val f = OptimizableFunction(
            customDomains = arrayOf(
                (0.0).rangeTo(1.0),
                (0.0).rangeTo(1.0)
            )
        ) {
            // Unsure if clamping here will mess with results.
            // TBD if resetting is better, but that may not settle... maybe culling?
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

        val desPsoCalibration = PSOSwarm(
            function = f,
            smallestMovePct = 1E-10
        )
        desPsoCalibration.run()
        return desPsoCalibration.getBest().let { best ->
            Pair(best[0].toFloat(), best[1].toFloat())
        }
    }

    val (trainingData, testData) = fakeSensorData().let { allData ->
        allData.toList().chunked((allData.size * 0.99).toInt()).map { it.toFloatArray() }
    }

    val (alpha, beta) = psoAlphaBeta(trainingData)
    LOG.info { "PSO estimated alpha:$alpha, beta:$beta" }

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