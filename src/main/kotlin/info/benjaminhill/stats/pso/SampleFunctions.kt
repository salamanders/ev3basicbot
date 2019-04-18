package info.benjaminhill.stats.pso

internal object SampleFunctions {

    private val DoubleArray.x: Double
        get() = this[0]

    private val DoubleArray.y: Double
        get() = this[1]

    /**
     * Calculate the result of (x^4)-2(x^3).
     * Domain is (-infinity, infinity).
     * Minimum is -1.6875 at x = 1.5.
     */
    val functionA by lazy {
        OptimizableFunction {
            Math.pow(it.x, 4.0) - 2 * Math.pow(it.x, 3.0)
        }
    }

    /**
     * Perform Ackley's function.
     * Domain is [5, 5]
     * Minimum is 0 at x = 0 & y = 0.
     */
    val ackleysFunction by lazy {
        OptimizableFunction(
            customDomains = arrayOf(
                (-5.0).rangeTo(5.0),
                (-15.0).rangeTo(15.0)
            )
        ) {
            val p1 = -20 * Math.exp(-0.2 * Math.sqrt(0.5 * (it.x * it.x + it.y * it.y)))
            val p2 = Math.exp(0.5 * (Math.cos(2.0 * Math.PI * it.x) + Math.cos(2.0 * Math.PI * it.y)))
            p1 - p2 + Math.E + 20.0
        }
    }


    /**
     * Perform Booth's function.
     * Domain is [-10, 10]
     * Minimum is 0 at x = 1 & y = 3.
     * @return      the z component
     */
    val boothsFunction by lazy {
        OptimizableFunction(
            customDomains = arrayOf(
                (-2500000.0).rangeTo(2500000.0),
                (-1500000.0).rangeTo(1500000.0)
            )
        ) {
            val p1 = Math.pow(it.x + 2 * it.y - 7, 2.0)
            val p2 = Math.pow(2 * it.x + it.y - 5, 2.0)
            p1 + p2
        }
    }

    val threeHumpCamelFunction by lazy {
        OptimizableFunction(2) {
            val p1 = 2.0 * it.x * it.x
            val p2 = 1.05 * Math.pow(it.x, 4.0)
            val p3 = Math.pow(it.x, 6.0) / 6
            p1 - p2 + p3 + it.x * it.y + it.y * it.y
        }
    }
}

