package api

import api.Movement.calculateAbsoluteDifferences
import api.Movement.getMovementStep
import api.Movement.isDiagonalMovement
import api.Movement.isStraightMovement

const val SIZE = 8

data class Coordinate(val x: Int, val y: Int)

fun Coordinate.distance(to: Coordinate) = Movement.distance(
    fromX = this.x,
    fromY = this.y,
    toX = to.x,
    toY = to.y
)

enum class ChessColour {
    White, Black
}

enum class ChessState {
    Ongoing, // The game is ongoing, and no special condition is met.
    Check, // One of the kings is in check, but the game is not over.
    Checkmate, // The game is over because one of the kings cannot escape capture.
    Stalemate, // The game is a draw because the player to move has no legal move and their king is not in check.
    DrawByRepetition, // The game is a draw because the same position has occurred three times with the same player to move and all possible moves.
    DrawByInsufficientMaterial, // The game is a draw because neither player has enough pieces to force a checkmate.
    DrawByFiftyMoveRule, // The game is a draw because there have been fifty moves by each player without the movement of any pawn and without any capture.
    DrawByAgreement, // The players have agreed to a draw.
    Resignation // One of the players has resigned, ending the game.
}

enum class Direction(val dx: Int, val dy: Int) {
    Up(0, 1),
    Down(0, -1),
    Left(-1, 0),
    Right(1, 0),

    TopRight(1, 1),
    TopLeft(1, -1),
    BottomRight(-1, 1),
    BottomLeft(-1, -1);

    companion object {
        fun fromCoordinates(from: Coordinate, to: Coordinate): Direction? {
            val dx = to.x - from.x
            val dy = to.y - from.y
            return entries.find { it.dx == dx && it.dy == dy }
        }
    }
}

class ChessBoard {
    private val board: Array<Array<ChessPiece?>> = Array(SIZE) {
        Array(SIZE) { null }
    }

    private var state = ChessState.Ongoing
    private var turnsPlayed = 1

    init {
        setupAsWhiteSide()
    }

    private fun setupAsWhiteSide() {
        (0 until SIZE).forEach { i ->
            this[6, i] = Pawn(ChessColour.White)
            this[1, i] = Pawn(ChessColour.Black)
        }

        listOf(0, 7).forEach { i ->
            this[7, i] = Rook(ChessColour.White)
            this[0, i] = Rook(ChessColour.Black)
        }

        listOf(1, 6).forEach { i ->
            this[7, i] = Knight(ChessColour.White)
            this[0, i] = Knight(ChessColour.Black)
        }

        listOf(2, 5).forEach { i ->
            this[7, i] = Bishop(ChessColour.White)
            this[0, i] = Bishop(ChessColour.Black)
        }

        this[7, 3] = Queen(ChessColour.White)
        this[0, 3] = Queen(ChessColour.Black)

        this[7, 4] = King(ChessColour.White)
        this[0, 4] = King(ChessColour.Black)
    }

    private fun setupAsBlackSide() {
        (0 until SIZE).forEach { i ->
            this[1, i] = Pawn(ChessColour.White)
            this[6, i] = Pawn(ChessColour.Black)
        }

        listOf(0, 7).forEach { i ->
            this[0, i] = Rook(ChessColour.White)
            this[7, i] = Rook(ChessColour.Black)
        }

        listOf(1, 6).forEach { i ->
            this[0, i] = Knight(ChessColour.White)
            this[7, i] = Knight(ChessColour.Black)
        }

        listOf(2, 5).forEach { i ->
            this[0, i] = Bishop(ChessColour.White)
            this[7, i] = Bishop(ChessColour.Black)
        }

        this[0, 3] = Queen(ChessColour.White)
        this[7, 3] = Queen(ChessColour.Black)

        this[0, 4] = King(ChessColour.White)
        this[7, 4] = King(ChessColour.Black)
    }

    operator fun get(x: Int, y: Int): ChessPiece? = board[x][y]

    operator fun set(
        x: Int,
        y: Int,
        piece: ChessPiece?
    ) {
        board[x][y] = piece
    }

    fun deepCopy(): ChessBoard {
        val copy = ChessBoard()
        for (x in board.indices) {
            for (y in board[x].indices) {
                copy[x, y] = board[x][y]
            }
        }
        return copy
    }
}

fun ChessBoard.checkEndConditions(): Boolean {
    return false
}

fun ChessBoard.isPathClear(from: Coordinate, to: Coordinate): Boolean {
    val (xStep, yStep) = getMovementStep(from, to)

    var currentX = from.x
    var currentY = from.y

    while (currentX != to.x || currentY != to.y) {
        currentX += xStep
        currentY += yStep
        // Exit if we've reached the target coordinate to avoid checking the destination for a piece
        if (currentX == to.x && currentY == to.y) break
        if (this[currentX, currentY] != null) return false // Path is blocked
    }

    return true
}

fun ChessBoard.movesForBishopAndRook(
    directions: List<Coordinate>,
    position: Coordinate,
    thisPiece: ChessPiece
): List<Coordinate> {
    val possibleMoves = mutableListOf<Coordinate>()

    directions.forEach { (dx, dy) ->
        var currentX = position.x + dx
        var currentY = position.y + dy

        // Traverse diagonally in each direction
        while (currentX in 0 until SIZE && currentY in 0 until SIZE) {
            val nextPosition = Coordinate(currentX, currentY)
            val pieceAtNextPosition = this[currentX, currentY]

            // If we encounter a piece
            if (pieceAtNextPosition != null) {
                // If it's an opponent's piece, it can be captured, so it's a valid move
                if (pieceAtNextPosition.colour != thisPiece.colour) {
                    possibleMoves.add(nextPosition)
                }
                break // Stop looking further in this direction
            } else {
                // No piece encountered, add as a valid move
                possibleMoves.add(nextPosition)
            }

            currentX += dx
            currentY += dy
        }
    }

    return possibleMoves
}

/**
 * Check if the destination square is either empty or occupied by an opponent's piece
 */
fun ChessBoard.chessPieceNullOrNotThisColour(
    thisPiece: ChessPiece,
    toX: Int, toY: Int
): Boolean {
    val destinationPiece = this[toX, toY]
    return destinationPiece == null || destinationPiece.colour != thisPiece.colour
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
) : Movable, Positionable {
    override var hasMoved = false

    abstract fun possibleMoves(
        chessBoard: ChessBoard,
        position: Coordinate
    ): List<Coordinate>
}

class Pawn(colour: ChessColour) : ChessPiece(
    colour,
    if (colour == ChessColour.White) "white_pawn.png" else "black_pawn.png"
) {
    private val direction = if (colour == ChessColour.White) 1 else -1

    private fun forwardMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = from.x + direction == to.x
            && from.y == to.y
            && chessBoard[to.x, to.y] == null

    private fun initialDoubleForwardMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate,
    ) = !hasMoved
            && from.x + 2 * direction == to.x
            && from.y == to.y && chessBoard[to.x, to.y] == null
            && chessBoard[from.x + direction, from.y] == null

    private fun captureMove(
        chessBoard: ChessBoard,
        from: Coordinate,
        to: Coordinate
    ) = from.x + direction == to.x
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
        if (chessBoard[x + direction, y] == null) {
            possibleMoves.add((Coordinate(x + direction, y)))
            // Double move from start position
            if (!hasMoved && chessBoard[x + 2 * direction, y] == null) {
                possibleMoves.add(Coordinate(x + 2 * direction, y))
            }
        }

        // Capturing moves
        listOf(-1, 1).forEach { dy ->
            if (y + dy in 0..< SIZE) { // Ensure within board bounds
                val target = chessBoard[x + direction, y + dy]
                if (target != null && target.colour != this.colour) {
                    possibleMoves.add(Coordinate(x + direction, y + dy))
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
        Direction.Up, Direction.Down,
        Direction.Left, Direction.Right
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
        Direction.TopRight,
        Direction.TopLeft,
        Direction.BottomRight,
        Direction.BottomLeft
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
        Direction.Up, Direction.Down, Direction.Left, Direction.Right,
        Direction.TopRight, Direction.TopLeft, Direction.BottomRight, Direction.BottomLeft
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
                    chessBoard.chessPieceNullOrNotThisColour(this, newX, newY)) {
                    possibleMoves.add(Coordinate(newX, newY))
                }
            }
        }

        // TODO Add castling moves

        return possibleMoves
    }
}
