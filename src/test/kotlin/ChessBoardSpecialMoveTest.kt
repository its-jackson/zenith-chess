import api.ChessBoardTestSetups.enPassantDirection
import api.ChessBoardTestSetups.enPassantBlackPawnLastMove
import api.ChessBoardTestSetups.enPassantWhitePawnCoords
import api.ChessBoardTestSetups.getNewEnPassantTestBoardInstance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChessBoardSpecialMoveTest {
    @Test
    fun `test en passant move is possible`() {
        val board = getNewEnPassantTestBoardInstance()

        val whitePawnPosition = enPassantWhitePawnCoords

        val possibleMoves = board[whitePawnPosition.x, whitePawnPosition.y]
            ?.possibleMoves(board, whitePawnPosition) ?: emptyList()

        // The en passant target square would be where the black pawn moved from +- one step
        // in other words, if the pawn only moved one step
        val enPassantTarget = enPassantBlackPawnLastMove.copy(enPassantBlackPawnLastMove.x + enPassantDirection)

        assertTrue(possibleMoves.contains(enPassantTarget), "En passant move should be possible")
    }
}