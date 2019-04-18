package info.benjaminhill.stats.pso

import kotlin.random.Random

/**
 * @param customDomains if something other than (-100.0)..(100)
 * @param f function to evaluate.  Either the number of custom domains OR the numDimensions limit the available data.
 */
open class OptimizableFunction(
    numDimensions: Int = 1,
    customDomains: Array<ClosedFloatingPointRange<Double>>? = null,
    private val f: (data: DoubleArray) -> Double
) {
    val domains: Array<ClosedFloatingPointRange<Double>> = customDomains
        ?: Array(numDimensions) { (-100.0).rangeTo(100.0) }

    fun eval(data: DoubleArray): Double {
        require(data.size == domains.size)
        return f(data)
    }

    internal fun newZeroVector(): Vector = Vector(domains.size)

    internal fun newRandomVector(): Vector = newZeroVector().also {
        it.set(*(0 until domains.size).map { i ->
            Random.nextDouble() * (domains[i].endInclusive - domains[i].start) - domains[i].start
        }.toDoubleArray())
    }
}