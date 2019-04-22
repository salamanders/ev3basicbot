package info.benjaminhill.stats.pso

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Represents a swarm of particles from the Particle PSOSwarm Optimization algorithm.
 * @param function the OptimizableFunction to find the minimum *starting* within the domains
 * @param numOfParticles the number of particles to create, default to 30 * dimensions
 * @param maxEpochs upper bound on number of generations, default to 1k * dimensions (hopefully quit much earlier from smallestMovePct)
 * @param inertiaC the particles' resistance to change, reasonable default
 * @param cognitiveC the cognitive component or introversion of the particle, reasonable default
 * @param socialC the social component or extroversion of the particle, reasonable default
 * @param smallestMovePct If the total velocity sq is less than this, assume stable and quit early
 */
class PSOSwarm(
    private val function: OptimizableFunction,
    private val numOfParticles: Int = function.domains.size * 5 + 3,
    private val maxEpochs: Int = 1_000 * function.domains.size,
    inertiaC: Double = 0.729844,
    cognitiveC: Double = 1.496180,
    socialC: Double = 1.496180,
    private val smallestMovePct: Double = 1E-20
) : Runnable {

    // Position and output (no velocity)
    private val globalBest = Particle(function)

    // Create a set of particles, each with random starting positions.
    private val particles = Array(numOfParticles) {
        Particle(function, inertiaC, cognitiveC, socialC)
    }

    private val totalSpaceSq = function.domains.map { it.endInclusive - it.start }.sumByDouble { it * it }

    fun getBest() = globalBest.bestPosition.getData()

    override fun run() {
        runBlocking(Dispatchers.IO) {

            for (epoch in 0 until maxEpochs) {
                // Bring everything up to date.
                coroutineScope {
                    particles.forEach { launch { it.updatePersonalBest() } }
                }

                // Potentially update new global best
                particles
                    .filter { it.bestEval < globalBest.bestEval }
                    .minBy { it.bestEval }?.let { newBest ->
                        globalBest.position.set(newBest.bestPosition)
                        globalBest.updatePersonalBest() // < one extra eval per epoch, meh.
                    }

                // Now everyone - move at the same time
                coroutineScope {
                    particles.forEach { launch { it.updateVelocityAndPosition(globalBest) } }
                }

                val travelPct = particles.sumByDouble { it.velocity.magnitudeSq() } / totalSpaceSq
                if (travelPct < smallestMovePct) {
                    LOG.debug { "Particles moved a very small pct, ending early after epoch $epoch" }
                    break
                }

                if (epoch and epoch - 1 == 0) {
                    LOG.debug { "epoch:$epoch, with ${particles.size} valid particles that traveled $travelPct. Best: ${globalBest.bestPosition}=${globalBest.bestEval}" }
                }
            }
        }
        LOG.info { "PSO RESULT: $globalBest" }
    }

    companion object {
        private val LOG = KotlinLogging.logger {}
    }
}

