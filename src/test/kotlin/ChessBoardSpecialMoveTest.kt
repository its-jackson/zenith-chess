import api.ChessBoard
import api.ChessBoardTestSetups.enPassantDirection
import api.ChessBoardTestSetups.enPassantBlackPawnLastMove
import api.ChessBoardTestSetups.enPassantWhitePawnCoords
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

    @BeforeEach
    fun setup() {
        enPassantBoard = getNewEnPassantTestBoardInstance()
        promotionBoard = getNewPawnPromotionTestBoardInstance()
    }

    private fun assertMovePossible(
        board: ChessBoard,
        startCoords: Coordinate,
        expectedMove: Coordinate,
        message: String
    ) {
        val possibleMoves = board[startCoords.x, startCoords.y]?.possibleMoves(board, startCoords) ?: emptyList()
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
}