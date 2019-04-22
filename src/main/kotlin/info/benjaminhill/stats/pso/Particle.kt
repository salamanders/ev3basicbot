package info.benjaminhill.stats.pso

import kotlin.random.Random

/**
 * Represents a particle from the Particle PSOSwarm Optimization algorithm.
 *
 * Construct a Particle with a random starting position.
 * @param function OptimizableFunction to work on
 * @param inertiaC resistance to change
 * @param cognitiveC amount to move towards your own best
 * @param socialC amount to move towards global best
 */
internal class Particle(
    private val function: OptimizableFunction,
    private val inertiaC: Double = 0.0,
    private val cognitiveC: Double = 0.0,
    private val socialC: Double = 0.0
) {

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
     *
     * @param globalBest + socialC move towards this
     */
    fun updateVelocityAndPosition(globalBest: Particle) {
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

        // Time to move
        this.position += velocity
    }


    override fun toString(): String = "{pos:$position, v:$velocity, bestPos:$bestPosition, bestEval:$bestEval}"
}
