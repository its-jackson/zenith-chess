package api

import api.StandardChessBoardLayout.BACK_ROW_BLACK
import api.StandardChessBoardLayout.BACK_ROW_WHITE
import api.StandardChessBoardLayout.bishopPositions
import api.StandardChessBoardLayout.KING_POSITION
import api.StandardChessBoardLayout.knightPositions
import api.StandardChessBoardLayout.PAWN_ROW_BLACK
import api.StandardChessBoardLayout.PAWN_ROW_WHITE
import api.StandardChessBoardLayout.QUEEN_POSITION
import api.StandardChessBoardLayout.rookPositions
import api.Movement.getMovementStep
import api.StandardChessBoardLayout.MAX_SIZE
import api.StandardChessBoardLayout.MIN_SIZE
import kotlin.math.abs

data class Coordinate(val x: Int, val y: Int)

fun Coordinate.distance(to: Coordinate) = Movement.distance(
    fromX = this.x,
    fromY = this.y,
    toX = to.x,
    toY = to.y
)

data class LastMove(
    val from: Coordinate,
    val to: Coordinate,
    val isDoublePawnMove: Boolean
)

enum class ChessColour {
    White, Black
}

fun ChessColour.opposite() = if (this == ChessColour.White) ChessColour.Black else ChessColour.White

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

enum class ChessDirection(val dx: Int, val dy: Int) {
    Up(0, 1),
    Down(0, -1),
    Left(-1, 0),
    Right(1, 0),

    TopRight(1, 1),
    TopLeft(1, -1),
    BottomRight(-1, 1),
    BottomLeft(-1, -1);

    companion object {
        fun fromCoordinates(from: Coordinate, to: Coordinate): ChessDirection? {
            val dx = to.x - from.x
            val dy = to.y - from.y
            return entries.find { it.dx == dx && it.dy == dy }
        }
    }
}

object StandardChessBoardLayout {
    const val MAX_SIZE = 8
    const val MIN_SIZE = 0

    const val PAWN_ROW_WHITE = 6
    const val PAWN_ROW_BLACK = 1

    const val BACK_ROW_WHITE = 7
    const val BACK_ROW_BLACK = 0

    const val QUEEN_POSITION = 3
    const val KING_POSITION = 4

    val rookPositions = intArrayOf(0, 7)
    val knightPositions = intArrayOf(1, 6)
    val bishopPositions = intArrayOf(2, 5)
}

class ChessBoard(
    val humanPlayerColour: ChessColour = ChessColour.White,
    var state: ChessState = ChessState.Ongoing,
    var turnsPlayed: Int = 1,
    var lastMove: LastMove? = null,
    private val board: Array<Array<ChessPiece?>> = Array(MAX_SIZE) { Array(MAX_SIZE) { null } }
) {
    val aiPlayerColour get() = humanPlayerColour.opposite()

    init {
        setup()
    }

    operator fun get(x: Int, y: Int): ChessPiece? = board[x][y]

    operator fun set(
        x: Int,
        y: Int,
        piece: ChessPiece?
    ) {
        board[x][y] = piece
    }

    private fun setup() {
        when (humanPlayerColour) {
            ChessColour.White -> setupAsWhiteSide()
            else -> setupAsBlackSide()
        }
    }

    private fun setupAsWhiteSide() {
        (MIN_SIZE until MAX_SIZE).forEach { i ->
            this[PAWN_ROW_WHITE, i] = Pawn(ChessColour.White, Movement.Direction.Down)
            this[PAWN_ROW_BLACK, i] = Pawn(ChessColour.Black, Movement.Direction.Up)
        }

        rookPositions.forEach { i ->
            this[BACK_ROW_WHITE, i] = Rook(ChessColour.White)
            this[BACK_ROW_BLACK, i] = Rook(ChessColour.Black)
        }

        knightPositions.forEach { i ->
            this[BACK_ROW_WHITE, i] = Knight(ChessColour.White)
            this[BACK_ROW_BLACK, i] = Knight(ChessColour.Black)
        }

        bishopPositions.forEach { i ->
            this[BACK_ROW_WHITE, i] = Bishop(ChessColour.White)
            this[BACK_ROW_BLACK, i] = Bishop(ChessColour.Black)
        }

        this[BACK_ROW_WHITE, QUEEN_POSITION] = Queen(ChessColour.White)
        this[BACK_ROW_BLACK, QUEEN_POSITION] = Queen(ChessColour.Black)

        this[BACK_ROW_WHITE, KING_POSITION] = King(ChessColour.White)
        this[BACK_ROW_BLACK, KING_POSITION] = King(ChessColour.Black)
    }

    private fun setupAsBlackSide() {
        (MIN_SIZE until MAX_SIZE).forEach { i ->
            this[PAWN_ROW_BLACK, i] = Pawn(ChessColour.White, Movement.Direction.Up)
            this[PAWN_ROW_WHITE, i] = Pawn(ChessColour.Black, Movement.Direction.Down)
        }

        rookPositions.forEach { i ->
            this[BACK_ROW_BLACK, i] = Rook(ChessColour.White)
            this[BACK_ROW_WHITE, i] = Rook(ChessColour.Black)
        }

        knightPositions.forEach { i ->
            this[BACK_ROW_BLACK, i] = Knight(ChessColour.White)
            this[BACK_ROW_WHITE, i] = Knight(ChessColour.Black)
        }

        bishopPositions.forEach { i ->
            this[BACK_ROW_BLACK, i] = Bishop(ChessColour.White)
            this[BACK_ROW_WHITE, i] = Bishop(ChessColour.Black)
        }

        this[BACK_ROW_BLACK, QUEEN_POSITION] = Queen(ChessColour.White)
        this[BACK_ROW_WHITE, QUEEN_POSITION] = Queen(ChessColour.Black)

        this[BACK_ROW_BLACK, KING_POSITION] = King(ChessColour.White)
        this[BACK_ROW_WHITE, KING_POSITION] = King(ChessColour.Black)
    }
}

fun ChessBoard.deepCopy(): ChessBoard {
    val copy = ChessBoard(
        humanPlayerColour = this.humanPlayerColour,
        state = this.state,
        turnsPlayed = this.turnsPlayed,
        lastMove = this.lastMove?.copy()
    )

    (MIN_SIZE until MAX_SIZE).forEach { x ->
        (MIN_SIZE until MAX_SIZE).forEach { y ->
            this[x, y].let {
                copy[x, y] = it?.deepCopy()
            }
        }
    }

    return copy
}

fun ChessBoard.isDoublePawnMove(from: Coordinate, to: Coordinate): Boolean {
    val piece = this[from.x, from.y] ?: return false
    return piece is Pawn && abs(from.x - to.x) == 2
}

fun ChessBoard.isGameOver() = state == ChessState.Checkmate
        || state == ChessState.Stalemate
        || state == ChessState.DrawByRepetition
        || state == ChessState.DrawByInsufficientMaterial
        || state == ChessState.DrawByFiftyMoveRule
        || state == ChessState.DrawByAgreement
        || state == ChessState.Resignation

/**
 * A player is in check if their king is in under attack and has a legal move to remove the threat.
 */
fun ChessBoard.isCheck(colour: ChessColour): Boolean {
    val kingPosition = findPieceCoordinate<King>(colour) ?: return false
    return isUnderAttack(kingPosition, colour)
}

/**
 * A player is in checkmate if their king is in check and
 * there are no legal moves available to remove the king from attack.
 */
fun ChessBoard.isCheckmate(colour: ChessColour): Boolean {
    if (!isCheck(colour)) return false

    // Iterate over all pieces of the current player to find any legal move that would remove the check
    val pieces = getPiecesByColour(colour)
    for ((piece, position) in pieces) {
        val possibleMoves = piece.possibleMoves(this, position)
        for (move in possibleMoves) {
            // Make a new chess board and simulate the possible move for the piece
            val testBoard = this.deepCopy().apply {
                // Use a deep copy of the piece for the simulation
                val pieceCopy = piece.deepCopy().apply { markAsMoved() }
                this[move.x, move.y] = pieceCopy
                this[position.x, position.y] = null
            }
            // If after the move the king is not in check, it's not checkmate
            if (!testBoard.isCheck(colour)) return false
        }
    }

    return true
}

/**
 * A stalemate occurs when the player to move is not in check but has no legal moves.
 */
fun ChessBoard.isStalemate(colour: ChessColour): Boolean {
    if (isCheck(colour)) return false

    // Check if there are no legal moves for any piece
    val pieces = getPiecesByColour(colour)
    return pieces.all { (piece, position) ->
        val pieceMoves = piece.possibleMoves(this, position)
        pieceMoves.isEmpty()
    }
}

fun ChessBoard.isDrawByInsufficientMaterial(): Boolean {
    // TODO Add logic to determine if either player can't checkmate the other
    return false // Implement based on actual rules
}

fun ChessBoard.isDrawByRepetition(): Boolean {
    // TODO Need to track board states after each move to implement this
    return false
}

fun ChessBoard.isDrawByFiftyMoveRule(): Boolean {
    // TODO Implement based on move history and rules
    return false
}

fun ChessBoard.isDrawByAgreement(): Boolean {
    // TODO
    return false
}

fun ChessBoard.isResignation(): Boolean {
    // TODO
    return false
}

fun ChessBoard.isUnderAttack(position: Coordinate, colour: ChessColour): Boolean {
    val enemyColour = colour.opposite()
    val enemyMoves = getPiecesByColour(enemyColour).flatMap {
        it.first.possibleMoves(this, it.second)
    }
    return enemyMoves.any { it == position }
}

fun ChessBoard.updateState() {
    state = when {
        isCheckmate(ChessColour.White) || isCheckmate(ChessColour.Black) -> ChessState.Checkmate
        isStalemate(ChessColour.White) || isStalemate(ChessColour.Black) -> ChessState.Stalemate
        isCheck(ChessColour.White) || isCheck(ChessColour.Black) -> ChessState.Check
        isDrawByInsufficientMaterial() -> ChessState.DrawByInsufficientMaterial
        isDrawByRepetition() -> ChessState.DrawByRepetition
        isDrawByFiftyMoveRule() -> ChessState.DrawByFiftyMoveRule
        isDrawByAgreement() -> ChessState.DrawByAgreement
        isResignation() -> ChessState.Resignation
        else -> ChessState.Ongoing
    }
}

fun ChessBoard.getPiecesByColour(chessColour: ChessColour): List<Pair<ChessPiece, Coordinate>> {
    return collectPieces { piece, _ ->
        chessColour == piece.colour
    }
}

/**
 * Finds the coordinate of the first piece of the specified type and colour.
 *
 * @param T The type of the chess piece to find.
 * @param colour The colour of the piece to find.
 * @return The coordinate of the found piece, or null if no such piece is found.
 */
inline fun <reified T : ChessPiece> ChessBoard.findPieceCoordinate(colour: ChessColour): Coordinate? {
    return onEachPiece { piece, coordinate ->
        if (piece is T && piece.colour == colour) coordinate else null
    }
}

/**
 * Iterates over all pieces on the board and collects them into a list based on a lambda condition.
 *
 * @param action A lambda function that takes the piece and its position and decides whether to include it in the result list.
 * @return A list of pairs containing the pieces and their coordinates that match the lambda condition.
 */
fun ChessBoard.collectPieces(action: (ChessPiece, Coordinate) -> Boolean): List<Pair<ChessPiece, Coordinate>> {
    val pieces = mutableListOf<Pair<ChessPiece, Coordinate>>()
    (MIN_SIZE until MAX_SIZE).forEach { x ->
        (MIN_SIZE until MAX_SIZE).forEach { y ->
            this[x, y]?.let { piece ->
                if (action(piece, Coordinate(x, y))) pieces.add(Pair(piece, Coordinate(x, y)))
            }
        }
    }
    return pieces
}

/**
 * Iterates over all pieces on the board and applies a given lambda function.
 *
 * @param action A lambda function that takes the piece and its position and returns a nullable Coordinate.
 * @return The first nonnull Coordinate returned by the lambda, or null if none are found.
 */
fun ChessBoard.onEachPiece(action: (ChessPiece, Coordinate) -> Coordinate?): Coordinate? {
    (MIN_SIZE until MAX_SIZE).forEach { x ->
        (MIN_SIZE until MAX_SIZE).forEach { y ->
            this[x, y]?.let {
                val result = action(it, Coordinate(x, y))
                if (result != null) return result
            }
        }
    }
    return null
}

fun ChessBoard.clearAllPieces() {
    (MIN_SIZE until MAX_SIZE).forEach { x ->
        (MIN_SIZE until MAX_SIZE).forEach { y ->
            this[x, y] = null
        }
    }
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

/**
 * Generates all possible moves for the bishop and rook chess pieces.
 * Used in combination for the queen chess piece too.
 */
fun ChessBoard.movesForBishopAndRook(
    directions: List<Coordinate>,
    position: Coordinate,
    thisPiece: ChessPiece
): List<Coordinate> {
    val possibleMoves = mutableListOf<Coordinate>()

    directions.forEach { (dx, dy) ->
        var currentX = position.x + dx
        var currentY = position.y + dy

        // Traverse in each direction
        while (currentX in MIN_SIZE until MAX_SIZE && currentY in MIN_SIZE until MAX_SIZE) {
            val nextPosition = Coordinate(currentX, currentY)
            val pieceAtNextPosition = this[currentX, currentY]

            // If we encounter a piece
            if (pieceAtNextPosition != null) {
                // If it's an opponent's piece, it can be captured, so it's a valid move
                if (pieceAtNextPosition.colour != thisPiece.colour) possibleMoves.add(nextPosition)
                break // Stop looking further in this direction
            } else {
                // No piece encountered, add as a valid move
                possibleMoves.add(nextPosition)
            }

            // Increment the coordinates
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
