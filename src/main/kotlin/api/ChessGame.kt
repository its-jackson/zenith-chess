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

// 8x8 Grid (64 pieces is the standard)
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
    val aiPlayerColour
        get() =
            humanPlayerColour.opposite()

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
            ChessColour.White -> setupAsStandardWhiteSide()
            else -> setupAsStandardBlackSide()
        }
    }

    private fun setupAsStandardWhiteSide() {
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

    private fun setupAsStandardBlackSide() {
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
            this[x, y].let { copy[x, y] = it?.deepCopy() }
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
        pieceMoves.count() == 0
    }
}

fun ChessBoard.isDrawByInsufficientMaterial(): Boolean {
    // TODO Add logic to determine if either player can't checkmate the other
    return false
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
    // TODO Implement based on when the AI and Human decide to both draw together
    return false
}

fun ChessBoard.isResignation(): Boolean {
    // TODO Implement based on when the user clicks on end, in the in-game menu composable
    return false
}

fun ChessBoard.isUnderAttack(target: Coordinate, colour: ChessColour): Boolean {
    val enemyPieces = getPiecesByColour(colour.opposite())
    for ((enemyPiece, enemyPosition) in enemyPieces) {
        if (enemyPiece.canAttack(this, enemyPosition, target))
            return true
    }
    return false
}

fun ChessBoard.couldBeUnderAttack(
    from: Coordinate,
    to: Coordinate,
    colour: ChessColour
): Boolean {
    val tempBoard = deepCopy()
    tempBoard.lastMove = LastMove(from, to, tempBoard.isDoublePawnMove(from, to))
    tempBoard[to.x, to.y] = tempBoard[from.x, from.y]
    tempBoard[from.x, from.y] = null
    tempBoard[to.x, to.y]?.markAsMoved()
    return tempBoard.isUnderAttack(Coordinate(to.x, to.y), colour)
}

fun ChessBoard.couldBeInCheck(
    from: Coordinate,
    to: Coordinate,
    colour: ChessColour
): Boolean {
    val tempBoard = deepCopy()
    tempBoard.lastMove = LastMove(from, to, tempBoard.isDoublePawnMove(from, to))
    tempBoard[to.x, to.y] = tempBoard[from.x, from.y]
    tempBoard[from.x, from.y] = null
    tempBoard[to.x, to.y]?.markAsMoved()
    return tempBoard.isCheck(colour)
}

fun ChessBoard.updateState() {
    state = when {
        isCheck(ChessColour.White) || isCheck(ChessColour.Black) -> ChessState.Check
        isCheckmate(ChessColour.White) || isCheckmate(ChessColour.Black) -> ChessState.Checkmate
        isStalemate(ChessColour.White) || isStalemate(ChessColour.Black) -> ChessState.Stalemate
        isDrawByInsufficientMaterial() -> ChessState.DrawByInsufficientMaterial
        isDrawByRepetition() -> ChessState.DrawByRepetition
        isDrawByFiftyMoveRule() -> ChessState.DrawByFiftyMoveRule
        isDrawByAgreement() -> ChessState.DrawByAgreement
        isResignation() -> ChessState.Resignation
        else -> ChessState.Ongoing
    }
}

fun ChessBoard.getPiecesByColour(chessColour: ChessColour) = collectPieces { piece, _ ->
    chessColour == piece.colour
}

/**
 * Finds the coordinate of the first piece of the specified type and colour.
 *
 * @param T The type of the chess piece to find.
 * @param colour The colour of the piece to find.
 * @return The coordinate of the found piece, or null if no such piece is found.
 */
inline fun <reified T : ChessPiece> ChessBoard.findPieceCoordinate(colour: ChessColour) = onEachPiece { piece, coordinate ->
    if (piece is T && piece.colour == colour) coordinate else null
}

/**
 * Iterates over all pieces on the board and collects them into a sequence based on a lambda condition.
 *
 * @param action A lambda function that takes the piece and its position and decides whether to include it in the result.
 * @return A sequence of pairs containing the pieces and their coordinates that match the lambda condition.
 */
fun ChessBoard.collectPieces(action: (ChessPiece, Coordinate) -> Boolean) = sequence {
    (MIN_SIZE until MAX_SIZE).forEach { x ->
        (MIN_SIZE until MAX_SIZE).forEach { y ->
            this@collectPieces[x, y]?.let { piece ->
                if (action(piece, Coordinate(x, y))) yield(Pair(piece, Coordinate(x, y)))
            }
        }
    }
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

/**
 * Checks that in a given direction that there is no obstacle between two chess pieces
 */
fun ChessBoard.isPathClear(from: Coordinate, to: Coordinate): Boolean {
    val (xStep, yStep) = getMovementStep(from, to)

    var currentX = from.x
    var currentY = from.y

    while (currentX != to.x || currentY != to.y) {
        currentX += xStep
        currentY += yStep
        // Exit if we've reached the target coordinate to avoid checking the destination for a piece
        if (currentX == to.x && currentY == to.y) break
        if (this[currentX, currentY] != null) return false
    }

    return true
}

/**
 * Generates all possible moves for the bishop and rook chess pieces
 */
fun ChessBoard.movesForBishopAndRook(
    directions: Sequence<Coordinate>,
    position: Coordinate,
    thisPiece: ChessPiece
) = sequence {
    directions.forEach { (dx, dy) ->
        var currentX = position.x + dx
        var currentY = position.y + dy

        while (
            currentX in MIN_SIZE until MAX_SIZE
            && currentY in MIN_SIZE until MAX_SIZE
        ) {
            val nextPosition = Coordinate(currentX, currentY)
            val pieceAtNextPosition = this@movesForBishopAndRook[currentX, currentY]

            if (pieceAtNextPosition != null) {
                if (pieceAtNextPosition.colour != thisPiece.colour) yield(nextPosition)
                break // Stop if a piece is encountered
            } else {
                yield(nextPosition)
            }

            currentX += dx
            currentY += dy
        }
    }
}

/**
 * Generates all possible legal moves for the bishop and rook chess pieces
 */
fun ChessBoard.legalMovesForBishopAndRook(
    directions: Sequence<Coordinate>,
    position: Coordinate,
    thisPiece: ChessPiece
) = movesForBishopAndRook(directions, position, thisPiece)
    .filter { move -> thisPiece.isMoveLegal(this, position, move) }

/**
 * Check if the destination square is either empty (null) or occupied by an enemies piece (colour)
 */
fun ChessBoard.chessPieceNullOrNotThisColour(
    thisPiece: ChessPiece,
    toX: Int, toY: Int
) = this[toX, toY].let { destinationPiece ->
    destinationPiece == null || destinationPiece.colour != thisPiece.colour
}
