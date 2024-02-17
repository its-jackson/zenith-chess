package api

import kotlin.math.abs
import kotlin.math.max

object Movement {

    /**
     * Chebyshev distance
     *
     * Measures distance between two points as the maximum difference over any of their axis values.
     * Which aligns well with how pieces move on a chessboard in many scenarios.
     *
     * For pieces like the rook, bishop, and queen, which can move along ranks, files, or diagonals,
     * the Chebyshev distance gives a straightforward measure of how far apart two squares are along those paths.
     *
     * For the king, which moves one square in any direction, the Chebyshev distance directly corresponds
     * to the minimum number of moves needed to get from one square to another.
     */
    fun distance(
        fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ) = max(abs(toX - fromX), abs(toY - fromY))

    fun isDiagonalMovement(dx: Int, dy: Int) = dx == dy

    fun isStraightMovement(dx: Int, dy: Int) = dx == 0 || dy == 0

    fun calculateAbsoluteDifferences(from: Coordinate, to: Coordinate): Pair<Int, Int> {
        val dx = abs(from.x - to.x)
        val dy = abs(from.y - to.y)
        return Pair(dx, dy)
    }

    fun calculateDirectionalDifferences(from: Coordinate, to: Coordinate): Pair<Int, Int> {
        val dx = to.x - from.x
        val dy = to.y - from.y
        return Pair(dx, dy)
    }

    fun getMovementStep(from: Coordinate, to: Coordinate): Pair<Int, Int> {
        val (dx, dy) = calculateDirectionalDifferences(from, to)

        val xStep = when {
            dx > 0 -> 1
            dx < 0 -> -1
            else -> 0 // No horizontal movement
        }

        val yStep = when {
            dy > 0 -> 1
            dy < 0 -> -1
            else -> 0 // No vertical movement
        }

        return Pair(xStep, yStep)
    }
}