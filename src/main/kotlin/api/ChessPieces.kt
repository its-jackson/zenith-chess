package api

import api.Movement.calculateAbsoluteDifferences
import api.Movement.isDiagonalMovement
import api.Movement.isStraightMovement
import api.StandardChessBoardLayout.MAX_SIZE
import api.StandardChessBoardLayout.MIN_SIZE
import kotlin.math.abs

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
) : Movable, Positionable {
    override var hasMoved = false

    abstract fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ): List<Coordinate>
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

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        if (forwardMove(chessBoard, from, to)) return true
        if (initialDoubleForwardMove(chessBoard, from, to)) return true
        if (captureMove(chessBoard, from, to)) return true
        if (isEnPassantMove(chessBoard, from, to)) return true
        return false
    }

    override fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): List<Coordinate> {
        val possibleMoves = mutableListOf<Coordinate>()
        val (x, y) = position

        // TODO When a pawn reaches the opposite side of the board

        // Standard and double move
        if (chessBoard[x + step, y] == null) {
            possibleMoves.add((Coordinate(x + step, y)))
            if (!hasMoved && chessBoard[x + 2 * step, y] == null) possibleMoves.add(Coordinate(x + 2 * step, y))
        }

        // Capturing moves
        listOf(
            Movement.Direction.Down.step,
            Movement.Direction.Up.step
        ).forEach { dy ->
            if (y + dy in MIN_SIZE..< MAX_SIZE) {
                val target = chessBoard[x + step, y + dy]
                val coordinate = Coordinate(x + step, y + dy)
                if (target != null && target.colour != this.colour) possibleMoves.add(coordinate)
                if (isEnPassantMove(chessBoard, position, coordinate)) possibleMoves.add(coordinate)
            }
        }

        return possibleMoves
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
    private val directions = listOf(
        ChessDirection.Up, ChessDirection.Down,
        ChessDirection.Left, ChessDirection.Right
    ).map { Coordinate(it.dx, it.dy) }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        // TODO Castling with king move

        val (dx, dy) = calculateAbsoluteDifferences(from, to)
        if (!isStraightMovement(dx, dy)) return false
        return chessBoard.isPathClear(from, to)
                && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
    }

    override fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): List<Coordinate> {
        // TODO Castling with king move

        return chessBoard.movesForBishopAndRook(directions, position, this)
    }
}

class Knight(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_knight.png" else "black_knight.png"
) {
    private val moveOffsets = listOf(
        Coordinate(2, 1), Coordinate(2, -1),
        Coordinate(-2, 1), Coordinate(-2, -1),
        Coordinate(1, 2), Coordinate(1, -2),
        Coordinate(-1, 2), Coordinate(-1, -2)
    )

    private fun isLShapedMove(dx: Int, dy: Int) = (dx == 2 && dy == 1) || (dx == 1 && dy == 2)

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        val (dx, dy) = calculateAbsoluteDifferences(from, to)
        if (!isLShapedMove(dx, dy)) return false

        return chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
    }

    override fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): List<Coordinate> {
        return moveOffsets.map { offset ->
            Coordinate(position.x + offset.x, position.y + offset.y)
        }.filter { move ->
            move.x in MIN_SIZE until MAX_SIZE && move.y in MIN_SIZE until MAX_SIZE
                    && chessBoard.chessPieceNullOrNotThisColour(this, move.x, move.y)
        }
    }
}

class Bishop(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_bishop.png" else "black_bishop.png"
) {
    private val directions = listOf(
        ChessDirection.TopRight, ChessDirection.TopLeft,
        ChessDirection.BottomRight, ChessDirection.BottomLeft
    ).map { Coordinate(it.dx, it.dy) }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        val (dx, dy) = calculateAbsoluteDifferences(from, to)
        if (!isDiagonalMovement(dx, dy)) return false

        return chessBoard.isPathClear(from, to)
                && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
    }

    override fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): List<Coordinate> =
        chessBoard.movesForBishopAndRook(directions, position, this)
}

class Queen(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_queen.png" else "black_queen.png"
) {
    private val directions = listOf(
        ChessDirection.Up, ChessDirection.Down, ChessDirection.Left, ChessDirection.Right,
        ChessDirection.TopRight, ChessDirection.TopLeft, ChessDirection.BottomRight, ChessDirection.BottomLeft
    ).map { Coordinate(it.dx, it.dy) }

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        val (dx, dy) = calculateAbsoluteDifferences(from, to)
        if (!(isStraightMovement(dx, dy) || isDiagonalMovement(dx, dy))) return false

        return chessBoard.isPathClear(from, to)
                && chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
    }

    override fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): List<Coordinate> =
        chessBoard.movesForBishopAndRook(directions, position, this)
}

class King(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_king.png" else "black_king.png"
) {
    // Check for one square movement in any direction
    private fun isOneSquareAnyDirection(dx: Int, dy: Int) = dx > 1 || dy > 1

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        val (dx, dy) = calculateAbsoluteDifferences(from, to)
        if (isOneSquareAnyDirection(dx, dy)) return false

        // TODO Check for castling conditions here

        return chessBoard.chessPieceNullOrNotThisColour(this, to.x, to.y)
    }

    override fun possibleMoves(chessBoard: ChessBoard, position: Coordinate): List<Coordinate> {
        val possibleMoves = mutableListOf<Coordinate>()

        // Generate moves one square around the king
        for (dx in -1..1) {
            for (dy in -1..1) {// Skip the square where the king currently is
                if (dx == 0 && dy == 0) continue

                val newX = position.x + dx
                val newY = position.y + dy

                if (newX in MIN_SIZE until MAX_SIZE && newY in MIN_SIZE until MAX_SIZE &&
                    chessBoard.chessPieceNullOrNotThisColour(this, newX, newY)
                ) {
                    possibleMoves.add(Coordinate(newX, newY))
                }
            }
        }

        // TODO Add castling moves

        return possibleMoves
    }
}
