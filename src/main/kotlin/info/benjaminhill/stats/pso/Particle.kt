package info.benjaminhill.stats.pso

import kotlin.random.Random

/**
 * Represents a particle from the Particle PSOSwarm Optimization algorithm.
 *
 * Construct a Particle with a random starting position.
 * @param function OptimizableFunction to work on
 */
internal class Particle(private val function: OptimizableFunction) {

    /** Random location *starting* within the allowed domain */
    val position: Vector = function.newRandomVector()
    val velocity: Vector = function.newZeroVector()
    val bestPosition: Vector = position.clone()

    /**
     * Current score of personal best solution (lower is better)
     * @return  the evaluation
     */
    var bestEval: Double = Double.MAX_VALUE
        private set

    init {
        updatePersonalBest()
    }

    /**
     * Update the personal best if the current evaluation is better.
     */
    fun updatePersonalBest() {
        val currentEval = function.eval(position.getData())
        if (currentEval < bestEval) {
            bestPosition.set(position)
            bestEval = currentEval
        }
    }

    /**
     * @param particle  the particle to update the velocity
     */
    fun updateVelocity(
        globalBest: Particle,
        inertiaC: Double,
        cognitiveC: Double,
        socialC: Double
    ) {
        // Natural friction
        velocity *= inertiaC

        // Steer towards your own best
        bestPosition.clone().let { pBest ->
            pBest -= position
            pBest *= cognitiveC
            pBest *= Random.nextDouble()
            velocity += pBest
        }

        // Steer towards global best
        globalBest.bestPosition.clone().let { gBest ->
            gBest -= position
            gBest *= socialC
            gBest *= Random.nextDouble()
            velocity += gBest
        }
    }

    /**
     * Update the position of a particle by adding its velocity to its position.
     */
    fun updatePosition() {
        this.position += velocity
    }

    override fun toString(): String = "{pos:$position, v:$velocity, bestPos:$bestPosition, bestEval:$bestEval}"
}
