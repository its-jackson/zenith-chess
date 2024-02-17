package models

import api.ChessBoard
import api.Coordinate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ChessBoardViewModel {
    var chessBoard by mutableStateOf(ChessBoard())
    var selectedSquare by mutableStateOf<Coordinate?>(null)
    val possibleMoves = mutableStateListOf<Coordinate>()

    fun selectOrMovePiece(x: Int, y: Int) {
        when (selectedSquare) {
            null -> {
                selectedSquare = Coordinate(x, y)
                possibleMoves.clear()
                val piece = chessBoard[x, y]
                piece?.possibleMoves(chessBoard, selectedSquare!!)?.forEach { possibleMoves.add(it) }
            }
            else -> {
                val selected = selectedSquare!!
                val destination = Coordinate(x, y)
                movePiece(selected, destination)
                selectedSquare = null
                possibleMoves.clear()
            }
        }
    }

    private fun movePiece(from: Coordinate, to: Coordinate) {
        if (checkMove(from, to)) {
            chessBoard[to.x, to.y] = chessBoard[from.x, from.y]
            chessBoard[to.x, to.y]?.markAsMoved()
            chessBoard[from.x, from.y] = null
            chessBoard = chessBoard.deepCopy() // Trigger state update
        }
    }

    private fun checkMove(from: Coordinate, to: Coordinate): Boolean {
        val piece = chessBoard[from.x, from.y] ?: return false
        return piece.isMoveLegal(chessBoard, from, to)
    }
}