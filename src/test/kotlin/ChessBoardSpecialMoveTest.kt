import api.ChessBoard
import api.ChessBoardTestSetups.enPassantDirection
import api.ChessBoardTestSetups.enPassantBlackPawnLastMove
import api.ChessBoardTestSetups.enPassantWhitePawnCoords
import api.ChessBoardTestSetups.getNewCastlingTestBoardInstance
import api.ChessBoardTestSetups.getNewEnPassantTestBoardInstance
import api.ChessBoardTestSetups.getNewPawnPromotionTestBoardInstance
import api.ChessBoardTestSetups.pawnPromotionDirection
import api.ChessBoardTestSetups.pawnPromotionWhitePawnCoords
import api.Coordinate
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChessBoardSpecialMoveTest {
    private lateinit var enPassantBoard: ChessBoard
    private lateinit var promotionBoard: ChessBoard
    private lateinit var castlingBoard: ChessBoard

    @BeforeEach
    fun setup() {
        enPassantBoard = getNewEnPassantTestBoardInstance()
        promotionBoard = getNewPawnPromotionTestBoardInstance()
        castlingBoard = getNewCastlingTestBoardInstance()
    }

    private fun assertMovePossible(
        board: ChessBoard,
        startCoords: Coordinate,
        expectedMove: Coordinate,
        message: String
    ) {
        val (startX, startY) = startCoords
        val possibleMoves = board[startX, startY]?.possibleMoves(board, startCoords) ?: emptySequence()
        assertTrue(possibleMoves.contains(expectedMove), message)
    }

    @Test
    fun `test en passant move is possible`() {
        val whitePawnCoordinate = enPassantWhitePawnCoords
        val enPassantTarget = enPassantBlackPawnLastMove.copy(x = enPassantBlackPawnLastMove.x + enPassantDirection)

        assertMovePossible(
            board = enPassantBoard,
            startCoords = whitePawnCoordinate,
            expectedMove = enPassantTarget,
            message = "En passant move should be possible."
        )
    }

    @Test
    fun `test pawn promotion move is possible`() {
        val whitePawnCoordinate = pawnPromotionWhitePawnCoords
        val promotionTarget = whitePawnCoordinate.copy(x = whitePawnCoordinate.x + pawnPromotionDirection)

        assertMovePossible(
            board = promotionBoard,
            startCoords = whitePawnCoordinate,
            expectedMove = promotionTarget,
            message = "Promotion move should be possible."
        )
    }

    @Test
    fun `test castling is possible`() {
        val kingStart = Coordinate(7, 4)
        val kingEnd = Coordinate(7, 6)
        val queenEnd = Coordinate(7, 2)

        val king = castlingBoard[kingStart.x, kingStart.y]

        assertTrue(
            king?.isMoveLegal(castlingBoard, kingStart, kingEnd) == true,
            "King side castling should be possible."
        )

        assertTrue(
            king?.isMoveLegal(castlingBoard, kingStart, queenEnd) == true,
            "Queen side castling should be possible."
        )
    }
}