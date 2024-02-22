package models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import api.*
import api.Movement.calculateAbsoluteDifferences
import api.Movement.getMovementStep
import api.StandardChessBoardLayout.BACK_ROW_BLACK
import api.StandardChessBoardLayout.BACK_ROW_WHITE

enum class PlayerType {
    AI, Human
}

class ChessBoardViewModel {
    var chessBoard by mutableStateOf(ChessBoard(humanPlayerColour = ChessColour.White))
    var currentPlayerType by mutableStateOf(PlayerType.Human)

    var selectedSquare by mutableStateOf<Coordinate?>(null)
    val possibleMoves = mutableStateListOf<Coordinate>()

    fun selectOrMovePiece(x: Int, y: Int) {
        if (currentPlayerType == PlayerType.AI) return

        when (selectedSquare) {
            null -> {
                possibleMoves.clear()
                val piece = chessBoard[x, y] ?: return
                if (piece.colour != chessBoard.humanPlayerColour) return
                selectedSquare = Coordinate(x, y)
                piece.possibleMoves(chessBoard, selectedSquare!!).forEach { possibleMoves.add(it) }
            }

            else -> {
                val selected = selectedSquare!!
                val destination = Coordinate(x, y)
                movePiece(selected, destination)
                selectedSquare = null
                possibleMoves.clear()
                if (currentPlayerType == PlayerType.AI) performRandomAIMove()
            }
        }
    }

    private fun movePiece(from: Coordinate, to: Coordinate) {
        val piece = chessBoard[from.x, from.y] ?: return

        if (piece is Pawn) {
            // Perform en passant capture
            // The pawn to be captured is on the same rank as the moving pawn's starting square
            // and on the same file as the moving pawn's destination square
            if (piece.isEnPassantMove(chessBoard, from, to)) {
                val capturedPawnPosition = Coordinate(from.x, to.y)
                chessBoard[capturedPawnPosition.x, capturedPawnPosition.y] = null
                completeMoveSequence(from, to)
                return
            }

            // Perform pawn promotion
            // When a pawn reaches the opposite sides back row
            // automatically change the piece to be a new queen with the same piece colour from before
            if (checkMove(from, to) && (to.x == BACK_ROW_WHITE) || to.x == BACK_ROW_BLACK) {
                chessBoard[from.x, from.y] = Queen(piece.colour) // Maybe allow customization later?
                completeMoveSequence(from, to)
                return
            }
        }

        // Check for castling by horizontal move of 2 squares
        if (piece is King
            && checkMove(from, to)
            && calculateAbsoluteDifferences(from, to).let { piece.isCastlingMove(it.first, it.second) }
        ) {
            // Determine direction of castling (y-axis)
            val direction = getMovementStep(from, to).second
            // Rook's original position (right for king side, left for queen side)
            val rookFromY = if (direction > 0) 7 else 0
            // Rook's new position next to the king
            val rookToY = if (direction > 0) to.y - 1 else to.y + 1

            // Move the king
            chessBoard[to.x, to.y] = piece
            chessBoard[from.x, from.y] = null
            piece.markAsMoved()

            // Move the rook
            val rook = chessBoard[from.x, rookFromY]
            chessBoard[from.x, rookToY] = rook
            chessBoard[from.x, rookFromY] = null
            rook?.markAsMoved()

            chessBoard.lastMove = LastMove(from, to, false)
            updateChessBoard()
            return
        }

        if (checkMove(from, to)) {
            completeMoveSequence(from, to)
        }
    }

    private fun togglePlayerTurn() {
        currentPlayerType = if (currentPlayerType == PlayerType.Human) PlayerType.AI else PlayerType.Human
    }

    private fun updateChessBoard() {
        chessBoard.turnsPlayed++
        chessBoard.updateState()
        chessBoard = chessBoard.deepCopy()
        togglePlayerTurn()
    }

    private fun completeMoveSequence(from: Coordinate, to: Coordinate) {
        chessBoard.lastMove = LastMove(from, to, chessBoard.isDoublePawnMove(from, to))
        chessBoard[to.x, to.y] = chessBoard[from.x, from.y]
        chessBoard[from.x, from.y] = null
        chessBoard[to.x, to.y]?.markAsMoved()
        updateChessBoard()
    }

    private fun checkMove(from: Coordinate, to: Coordinate): Boolean {
        val piece = chessBoard[from.x, from.y] ?: return false
        return piece.isMoveLegal(chessBoard, from, to)
    }

    private fun performRandomAIMove() {
        val aiPieces = chessBoard.getPiecesByColour(chessBoard.aiPlayerColour)
        val legalMoves = mutableListOf<Pair<Coordinate, List<Coordinate>>>()

        aiPieces.forEach { pair ->
            val from = pair.second
            val moves = pair.first.possibleMoves(chessBoard, from)
            if (moves.isNotEmpty()) {
                legalMoves.add(Pair(from, moves))
            }
        }

        if (legalMoves.isNotEmpty()) {
            val (from, moves) = legalMoves.random()
            val to = moves.random()
            movePiece(from, to)
        }
    }
}