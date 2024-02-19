package composables

import api.ChessPiece
import api.Coordinate
import api.SIZE
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import models.ChessBoardViewModel
import models.ConsoleViewModel

val lightSquareColor = Color(0xFFEEEED2)
val darkSquareColor = Color(0xFFB58863)
val selectionColor = Color(0xFFAED6F1)
val possibleMoveColor = Color(0xFFB7E4C7)

@Composable
fun ChessPiece(piece: ChessPiece?) {
    piece?.let {
        val bitmap = useResource(
            piece.imgResource,
            ::loadImageBitmap
        )

        Image(
            bitmap = bitmap,
            contentDescription = "${piece::class.simpleName}"
        )
    }
}

@Composable
fun ChessSquare(
    chessBoardViewModel: ChessBoardViewModel,
    consoleViewModel: ConsoleViewModel,
    row: Int,
    col: Int,
    width: Dp,
    height: Dp,
    piece: ChessPiece?,
) {
    val backgroundColour = if ((row + col) % 2 == 0) lightSquareColor else darkSquareColor
    val currentCoordinate = Coordinate(row, col)
    val isSelected = chessBoardViewModel.selectedSquare == currentCoordinate
    val isPossibleMove = chessBoardViewModel.possibleMoves.contains(currentCoordinate)

    val elevation by animateDpAsState(
        targetValue = if (isSelected || isPossibleMove) 2.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val animatedBrush = animateColorAsState(
        targetValue = when {
            isSelected -> selectionColor
            isPossibleMove -> possibleMoveColor
            else -> backgroundColour
        },
        animationSpec = tween(durationMillis = 600)
    )

    val outlineColor = when {
        isSelected -> selectionColor.copy(alpha = 0.3f)
        isPossibleMove -> Color.White.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val outlineWidth = if (isSelected || isPossibleMove) 1.dp else 0.dp

    Box(
        modifier = Modifier.width(width)
            .height(height)
            .shadow(elevation, RoundedCornerShape(2.dp))
            .border(width = outlineWidth, color = outlineColor, shape = RoundedCornerShape(2.dp))
            .background(animatedBrush.value)
            .clickable(
                indication = rememberRipple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                chessBoardViewModel.selectOrMovePiece(row, col)
            },
        contentAlignment = Alignment.Center
    ) {
        ChessPiece(piece)

        if (consoleViewModel.isDebug) {
            Text(
                text = "(x=$row, y=$col)",
                color = Color.Black,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun ChessBoard(
    chessBoardViewModel: ChessBoardViewModel,
    consoleViewModel: ConsoleViewModel,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val squareWidth = maxWidth / SIZE
        val squareHeight = maxHeight / SIZE

        Column {
            (0 until SIZE).forEach { rowIndex ->
                Row {
                    (0 until SIZE).forEach { columnIndex ->
                        ChessSquare(
                            consoleViewModel = consoleViewModel,
                            chessBoardViewModel = chessBoardViewModel,
                            piece = chessBoardViewModel.chessBoard[rowIndex, columnIndex],
                            row = rowIndex,
                            col = columnIndex,
                            width = squareWidth,
                            height = squareHeight
                        )
                    }
                }
            }
        }
    }
}
