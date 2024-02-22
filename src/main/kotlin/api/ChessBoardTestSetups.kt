package api

object ChessBoardTestSetups {
    val enPassantDirection = Movement.Direction.Down.step
    val enPassantWhitePawnCoords = Coordinate(4, 4)
    val enPassantBlackPawnCoords = Coordinate(4, 3)
    val enPassantBlackPawnLastMove = Coordinate(6, 3)

    val pawnPromotionDirection = Movement.Direction.Down.step
    val pawnPromotionWhitePawnCoords = Coordinate(1, 6)
    val pawnPromotionBlackPawnCoords = Coordinate(6, 4)
    val pawnPromotionBlackPawnLastMove = Coordinate(5, 4)

    fun getNewEnPassantTestBoardInstance(): ChessBoard {
        val board = ChessBoard().also { it.clearAllPieces() }

        // Place a white pawn in position to perform en passant
        board[enPassantWhitePawnCoords.x, enPassantWhitePawnCoords.y] =
            Pawn(ChessColour.White, Movement.Direction.Up).apply { markAsMoved() }

        // Place a black pawn that has just moved two squares forward next to the white pawn
        board[enPassantBlackPawnCoords.x, enPassantBlackPawnCoords.y] =
            Pawn(ChessColour.Black, Movement.Direction.Down).apply { markAsMoved() }

        // Set the last move to reflect this two square move
        board.lastMove = LastMove(
            from = enPassantBlackPawnLastMove,
            to = enPassantBlackPawnCoords,
            isDoublePawnMove = true
        )

        return board
    }

    fun getNewPawnPromotionTestBoardInstance(): ChessBoard {
        val board = ChessBoard().also { it.clearAllPieces() }

        board[pawnPromotionWhitePawnCoords.x, pawnPromotionWhitePawnCoords.y] =
            Pawn(ChessColour.White, Movement.Direction.Down).apply { markAsMoved() }

        board[pawnPromotionBlackPawnCoords.x, pawnPromotionBlackPawnCoords.y] =
            Pawn(ChessColour.Black, Movement.Direction.Up).apply { markAsMoved() }

        board.lastMove = LastMove(
            from = pawnPromotionBlackPawnLastMove,
            to = Coordinate(pawnPromotionBlackPawnCoords.x, pawnPromotionBlackPawnCoords.y),
            isDoublePawnMove = false
        )

        return board
    }

    fun getNewCastlingTestBoardInstance(): ChessBoard {
        val board = ChessBoard().also { it.clearAllPieces() }

        val whiteKingStartPos = Coordinate(7, 4)
        val whiteRookKingRightSideStartPos = Coordinate(7, 7)
        val whiteRookKingLeftSideStartPos = Coordinate(7, 0)

        board[whiteKingStartPos.x, whiteKingStartPos.y] = King(ChessColour.White)
        board[whiteRookKingRightSideStartPos.x, whiteRookKingRightSideStartPos.y] = Rook(ChessColour.White)
        board[whiteRookKingLeftSideStartPos.x, whiteRookKingLeftSideStartPos.y] = Rook(ChessColour.White)

        return board
    }
}