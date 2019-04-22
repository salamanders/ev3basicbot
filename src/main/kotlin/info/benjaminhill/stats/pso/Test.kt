package info.benjaminhill.stats.pso

internal fun main() {
    val pso = PSOSwarm(SampleFunctions.boothsFunction)
    pso.run()
    println(pso.getBest().joinToString { "%.4f".format(it) })
}