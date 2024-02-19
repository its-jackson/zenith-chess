package api

import api.Movement.calculateAbsoluteDifferences
import api.Movement.isDiagonalMovement
import api.Movement.isStraightMovement

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

class Pawn(
    colour: ChessColour,
    direction: Movement.Direction
) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_pawn.png" else "black_pawn.png"
) {
    private val step = direction.step

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

    override fun isMoveLegal(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ): Boolean {
        if (forwardMove(chessBoard, from, to)) {
            return true
        }

        if (initialDoubleForwardMove(chessBoard, from, to)) {
            return true
        }

        if (captureMove(chessBoard, from, to)) {
            return true
        }

        // TODO Add en passant logic

        return false
    }

    override fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ): List<Coordinate> {
        val possibleMoves = mutableListOf<Coordinate>()
        val (x, y) = position

        // TODO When a pawn reaches the opposite side of the board
        // val promotionRow

        // Standard move
        if (chessBoard[x + step, y] == null) {
            possibleMoves.add((Coordinate(x + step, y)))
            // Double move from start position
            if (!hasMoved && chessBoard[x + 2 * step, y] == null) {
                possibleMoves.add(Coordinate(x + 2 * step, y))
            }
        }

        // Capturing moves
        listOf(-1, 1).forEach { dy ->
            if (y + dy in 0..<SIZE) { // Ensure within board bounds
                val target = chessBoard[x + step, y + dy]
                if (target != null && target.colour != this.colour) {
                    possibleMoves.add(Coordinate(x + step, y + dy))
                }
                // Check for en passant conditions here, adding to possibleMoves if valid
            }
        }

        // En passant (this is a simplified version; you'll need additional state to check if en passant is applicable)
        // Suppose enPassantPossibleAt stores the square where en passant is possible
        // if (enPassantPossibleAt == Pair(x + direction, y + dy)) {
        //     possibleMoves.add(Pair(x + direction, y + dy))
        // }

        return possibleMoves
    }
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
            move.x in 0 until SIZE && move.y in 0 until SIZE
                    && chessBoard.chessPieceNullOrNotThisColour(this, move.x, move.y)
        }
    }
}

class Bishop(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_bishop.png" else "black_bishop.png"
) {
    private val directions = listOf(
        ChessDirection.TopRight,
        ChessDirection.TopLeft,
        ChessDirection.BottomRight,
        ChessDirection.BottomLeft
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
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue // Skip the square where the king currently is

                val newX = position.x + dx
                val newY = position.y + dy

                if (newX in 0 until SIZE && newY in 0 until SIZE &&
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
