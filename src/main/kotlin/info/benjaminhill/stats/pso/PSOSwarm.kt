package info.benjaminhill.stats.pso

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Represents a swarm of particles from the Particle PSOSwarm Optimization algorithm.
 * @param function the OptimizableFunction to find the minimum *starting* within the domains
 * @param numOfParticles the number of particles to create, default to 30 * dimensions
 * @param epochs the max number of generations, default to 1k * dimensions
 * @param inertiaC the particles resistance to change, reasonable default
 * @param cognitiveC the cognitive component or introversion of the particle, reasonable default
 * @param socialC  the social component or extroversion of the particle, reasonable default
 * @param smallestMovePct If the total velocity sq is less than this, assume stable and quit early
 */
class PSOSwarm(
    private val function: OptimizableFunction,
    private val numOfParticles: Int = 30 * function.domains.size,
    private val epochs: Int = 1_000 * function.domains.size,
    private val inertiaC: Double = 0.729844,
    private val cognitiveC: Double = 1.496180,
    private val socialC: Double = 1.496180,
    private val smallestMovePct: Double = 1E-20
) : Runnable {

    // Position and output (no velocity)
    private val globalBest = Particle(function)

    fun getBest() = globalBest.bestPosition.getData()

    override fun run() {

        // Create a set of particles, each with random starting positions.
        val particles = Array(numOfParticles) {
            Particle(function)
        }.toMutableList()
        val totalSpaceSq = function.domains.map { it.endInclusive - it.start }.sumByDouble { it * it }

        runBlocking(Dispatchers.IO) {

            for (epoch in 0 until epochs) {
                // Bring everything up to date.
                coroutineScope {
                    for (p in particles) {
                        launch {
                            p.updatePersonalBest()
                        }
                    }
                }

                // Make sure best is correct.
                for (p in particles) {
                    if (p.bestEval < globalBest.bestEval) {
                        if (p.bestEval < globalBest.bestEval / 10) {
                            //println("Found 2x better global best: $globalBest")
                        }
                        globalBest.position.set(p.bestPosition)
                        globalBest.updatePersonalBest() // Extra eval, meh.
                    }
                }

                // Now everyone can move
                coroutineScope {
                    for (p in particles) {
                        launch {
                            p.updateVelocity(globalBest, inertiaC, cognitiveC, socialC)
                            p.updatePosition()
                        }
                    }
                }

                val travelPct = particles.sumByDouble { it.velocity.magnitudeSq() } / totalSpaceSq
                if (travelPct < smallestMovePct) {
                    println("Particles moved a very small pct, ending early after epoch $epoch")
                    break
                }

                if (epoch and epoch - 1 == 0) {
                    println("epoch:$epoch, with ${particles.size} valid particles that traveled $travelPct")
                }
            }
        }
        println("RESULT: $globalBest")
    }
}

