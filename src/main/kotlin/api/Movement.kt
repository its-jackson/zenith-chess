package api

import kotlin.math.abs
import kotlin.math.max

object Movement {
    sealed class Direction(val step: Int) {
        data object Up : Direction(1)
        data object Down : Direction(-1)
        data object None : Direction(0)
    }

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
            dx > 0 -> Direction.Up.step
            dx < 0 -> Direction.Down.step
            else -> Direction.None.step
        }

        val yStep = when {
            dy > 0 -> Direction.Up.step
            dy < 0 -> Direction.Down.step
            else -> Direction.None.step
        }

        return Pair(xStep, yStep)
    }
}