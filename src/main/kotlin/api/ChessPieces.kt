package api

import api.Movement.calculateAbsoluteDifferences
import api.Movement.isDiagonalMovement
import api.Movement.isStraightMovement
import api.StandardChessBoardLayout.MAX_SIZE
import api.StandardChessBoardLayout.MIN_SIZE
import kotlin.math.abs

interface Attackable {
    fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean
}

interface Movable {
    fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean
}

interface Positionable {
    var hasMoved: Boolean

    fun markAsMoved() {
        if (hasMoved) return
        hasMoved = true
    }
}

abstract class ChessPiece(
    val colour: ChessColour,
    val imgResource: String
) : Attackable, Movable, Positionable {
    override var hasMoved = false

    abstract fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): Sequence<Coordinate>
}

fun ChessPiece.deepCopy(): ChessPiece {
    return when (this) {
        is Pawn -> Pawn(this.colour, this.direction).apply { hasMoved = this@deepCopy.hasMoved }
        is Rook -> Rook(this.colour).apply { hasMoved = this@deepCopy.hasMoved }
        is Knight -> Knight(this.colour).apply { hasMoved = this@deepCopy.hasMoved }
        is Bishop -> Bishop(this.colour).apply { hasMoved = this@deepCopy.hasMoved }
        is Queen -> Queen(this.colour).apply { hasMoved = this@deepCopy.hasMoved }
        is King -> King(this.colour).apply { hasMoved = this@deepCopy.hasMoved }
        else -> throw IllegalArgumentException("Unknown ChessPiece type")
    }
}

class Pawn(
    colour: ChessColour,
    val direction: Movement.Direction
) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_pawn.png" else "black_pawn.png"
) {
    private val step = direction.step

    override fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = captureMove(chessBoard, from, to) || isEnPassantMove(chessBoard, from, to)

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = (canAttack(chessBoard, from, to)
            || forwardMove(chessBoard, from, to)
            || initialDoubleForwardMove(chessBoard, from, to))
            && !chessBoard.couldBeInCheck(from, to, colour)

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ) = sequence {
        val (x, y) = position
        val standardMoveCoordinate = Coordinate(x + step, y)
        val doubleMoveCoordinate = Coordinate(x + 2 * step, y)

        // Standard move
        val (standardX, standardY) = standardMoveCoordinate
        if (chessBoard[standardX, standardY] == null) {
            if (!chessBoard.couldBeInCheck(position, standardMoveCoordinate, colour))
                yield((Coordinate(x + step, y)))

            // Double move
            val (doubleX, doubleY) = doubleMoveCoordinate
            if (!hasMoved
                && chessBoard[doubleX ,doubleY] == null
                && !chessBoard.couldBeInCheck(position, doubleMoveCoordinate, colour)) yield(doubleMoveCoordinate)
        }

        // Capturing moves
        listOf(
            Movement.Direction.Down.step,
            Movement.Direction.Up.step
        ).forEach { dy ->
            if (y + dy in MIN_SIZE..< MAX_SIZE) {
                val target = chessBoard[x + step, y + dy]
                val moveCoordinate = Coordinate(x + step, y + dy)
                if (chessBoard.couldBeInCheck(position, moveCoordinate, colour)) return@forEach
                if (target != null && target.colour != colour) yield(moveCoordinate)
                if (isEnPassantMove(chessBoard, position, moveCoordinate)) yield(moveCoordinate)
            }
        }
    }

    fun isEnPassantMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = chessBoard.lastMove?.let { lastMove ->
        val isLastMoveDoublePawnStep = lastMove.isDoublePawnMove &&
                chessBoard[lastMove.to.x, lastMove.to.y] is Pawn &&
                abs(lastMove.from.x - lastMove.to.x) == 2

        val isAdjacent = from.x == lastMove.to.x &&
                abs(from.y - lastMove.to.y) == 1

        val moveToEnPassantSquare = to.x == from.x + step &&
                to.y == lastMove.to.y

        return isLastMoveDoublePawnStep && isAdjacent && moveToEnPassantSquare
    } ?: false

    private fun forwardMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = from.x + step == to.x
            && from.y == to.y
            && chessBoard[to.x, to.y] == null

    private fun initialDoubleForwardMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate,
    ) = !hasMoved
            && from.x + 2 * step == to.x
            && from.y == to.y && chessBoard[to.x, to.y] == null
            && chessBoard[from.x + step, from.y] == null

    private fun captureMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = from.x + step == to.x
            && (from.y + 1 == to.y || from.y - 1 == to.y)
            && chessBoard[to.x, to.y] != null
            && chessBoard[to.x, to.y]?.colour != this.colour
}

class Rook(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_rook.png" else "black_rook.png"
) {
    private val directions = sequenceOf(
        ChessDirection.Up, ChessDirection.Down,
        ChessDirection.Left, ChessDirection.Right
    ).map { Coordinate(it.dx, it.dy) }

    override fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = calculateAbsoluteDifferences(from, to).let { (dx, dy) ->
        isStraightMovement(dx, dy) && chessBoard.isPathClear(from, to)
    }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = canAttack(chessBoard, from, to)
            && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
            && !chessBoard.couldBeInCheck(from, to, colour)

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ) = chessBoard.legalMovesForBishopAndRook(directions, position, this)
}

class Knight(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_knight.png" else "black_knight.png"
) {
    private val offsets = sequenceOf(
        Coordinate(2, 1), Coordinate(2, -1),
        Coordinate(-2, 1), Coordinate(-2, -1),
        Coordinate(1, 2), Coordinate(1, -2),
        Coordinate(-1, 2), Coordinate(-1, -2)
    )

    private fun isLShapedMove(dx: Int, dy: Int) = (dx == 2 && dy == 1) || (dx == 1 && dy == 2)

    override fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = calculateAbsoluteDifferences(from, to).let { (dx, dy) ->
        isLShapedMove(dx, dy)
    }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = canAttack(chessBoard, from, to)
            && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
            && !chessBoard.couldBeInCheck(from, to, colour)

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ) = offsets.map { offset -> Coordinate(position.x + offset.x, position.y + offset.y) }
        .filter { move ->
            move.x in MIN_SIZE until MAX_SIZE && move.y in MIN_SIZE until MAX_SIZE
                    && chessBoard.chessPieceNullOrNotThisColour(this, move.x, move.y)
                    && !chessBoard.couldBeInCheck(position, move, colour)
    }
}

class Bishop(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_bishop.png" else "black_bishop.png"
) {
    private val directions = sequenceOf(
        ChessDirection.TopRight, ChessDirection.TopLeft,
        ChessDirection.BottomRight, ChessDirection.BottomLeft
    ).map { Coordinate(it.dx, it.dy) }

    override fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = calculateAbsoluteDifferences(from, to).let { (dx, dy) ->
        isDiagonalMovement(dx, dy) && chessBoard.isPathClear(from, to)
    }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = canAttack(chessBoard, from, to)
            && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
            && !chessBoard.couldBeInCheck(from, to, colour)

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ) = chessBoard.legalMovesForBishopAndRook(directions, position, this)
}

class Queen(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_queen.png" else "black_queen.png"
) {
    private val directions = sequenceOf(
        ChessDirection.Up, ChessDirection.Down, ChessDirection.Left, ChessDirection.Right,
        ChessDirection.TopRight, ChessDirection.TopLeft, ChessDirection.BottomRight, ChessDirection.BottomLeft
    ).map { Coordinate(it.dx, it.dy) }

    override fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = calculateAbsoluteDifferences(from, to).let { (dx, dy) ->
        (isStraightMovement(dx, dy) || isDiagonalMovement(dx, dy))
                && chessBoard.isPathClear(from, to)
    }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = canAttack(chessBoard, from, to)
            && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
            && !chessBoard.couldBeInCheck(from, to, colour)

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ) = chessBoard.legalMovesForBishopAndRook(directions, position, this)
}

class King(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_king.png" else "black_king.png"
) {
    override fun canAttack(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = calculateAbsoluteDifferences(from, to).let { (dx, dy) ->
        isStandardMove(dx, dy)
                && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
    }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        if (calculateAbsoluteDifferences(from, to).let { isCastlingMove(it.first, it.second) }) {
            return canCastle(chessBoard, from, to)
        }
        return canAttack(chessBoard, from, to) && !chessBoard.couldBeUnderAttack(from, to, colour)
    }

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ) = sequence {
        // Generate moves one square around the king
        for (dx in Movement.Direction.Down.step..Movement.Direction.Up.step) {
            for (dy in Movement.Direction.Down.step..Movement.Direction.Up.step) {
                // Skip the square where the king currently is
                if (dx == Movement.Direction.None.step
                    && dy == Movement.Direction.None.step) continue

                val newX = position.x + dx
                val newY = position.y + dy

                if (newX in MIN_SIZE until MAX_SIZE && newY in MIN_SIZE until MAX_SIZE
                    && chessBoard.chessPieceNullOrNotThisColour(this@King, newX, newY)) {
                    val potentialMove = Coordinate(newX, newY)
                    if (!chessBoard.couldBeUnderAttack(position, potentialMove, colour)) {
                        yield(potentialMove)
                    }
                }
            }
        }

        // Add castling moves if applicable and ensure they don't lead the king into check
        if (!hasMoved && chessBoard.state != ChessState.Check) {
            if (canCastle(chessBoard, position, Coordinate(position.x, position.y + 2))) {
                yield(Coordinate(position.x, position.y + 2))
            }
            if (canCastle(chessBoard, position, Coordinate(position.x, position.y - 2))) {
                yield(Coordinate(position.x, position.y - 2))
            }
        }
    }

    private fun isStandardMove(dx: Int, dy: Int) = dx <= 1 && dy <= 1

    fun isCastlingMove(dx: Int, dy: Int) = dx == 0 && (dy == 2 || dy == -2)

    private fun canCastle(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        if (chessBoard.state == ChessState.Check) return false

        // Castling is a horizontal move, so we check the y-axis
        val direction = if (to.y > from.y) 1 else -1
        var currentY = from.y + direction

        // Ensure path is clear between the king and the rook
        while (currentY != to.y) {
            if (chessBoard[from.x, currentY] != null) return false
            currentY += direction
        }

        // Check if the king moves through a square that is under attack
        for (offset in 1..2) {
            val checkY = from.y + direction * offset
            if (chessBoard.isUnderAttack(Coordinate(from.x, checkY), colour)) return false
        }

        // Determine the rook's initial position for castling
        val rookY = if (direction > 0) 7 else 0
        val rook = chessBoard[from.x, rookY]

        return rook is Rook && !rook.hasMoved
    }
}
