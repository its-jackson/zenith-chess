package api

object ChessBoardTestSetups {
    val enPassantDirection = -1
    val enPassantWhitePawnCoords = Coordinate(4, 4)
    val enPassantBlackPawnCoords = Coordinate(4, 3)
    val enPassantBlackPawnLastMove = Coordinate(6, 3)

    fun getNewEnPassantTestBoardInstance(): ChessBoard {
        val board = ChessBoard().also { it.clearAllPieces() }

        // Place a white pawn in position to perform en passant
        board[enPassantWhitePawnCoords.x, enPassantWhitePawnCoords.y] =
            Pawn(ChessColour.White, Movement.Direction.Up).apply { this.markAsMoved() }

        // Place a black pawn that has just moved two squares forward next to the white pawn
        board[enPassantBlackPawnCoords.x, enPassantBlackPawnCoords.y] =
            Pawn(ChessColour.Black, Movement.Direction.Down).apply { this.markAsMoved() }

        // Set the last move to reflect this two square move
        board.lastMove = LastMove(
            from = enPassantBlackPawnLastMove,
            to = enPassantBlackPawnCoords,
            isDoublePawnMove = true
        )

        return board
    }
}