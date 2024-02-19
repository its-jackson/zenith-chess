import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import api.ChessColour
import api.GameDetails
import composables.ChessBoard
import composables.ConsoleOverlay
import models.ChessBoardViewModel
import models.ConsoleViewModel

private val CustomColors = darkColors(
    primary = Color.Black,
    primaryVariant = Color.DarkGray,
    onPrimary = Color.White,
    secondary = Color.LightGray,
    onSecondary = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    background = Color.Black,
    onBackground = Color.White
)

private val CustomTypography = Typography(
    button = TextStyle(
        color = Color.White
    )
)

private val CustomShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

@Composable
fun CustomMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = CustomColors,
        typography = CustomTypography,
        shapes = CustomShapes,
        content = content
    )
}

@Composable
@Preview
fun App(
    chessBoardViewModel: ChessBoardViewModel,
    consoleViewModel: ConsoleViewModel
) {
    CustomMaterialTheme {
        Box {
            ChessBoard(
                chessBoardViewModel = chessBoardViewModel,
                consoleViewModel = consoleViewModel
            )

            if (consoleViewModel.isConsoleVisible) {
                ConsoleOverlay(consoleViewModel)
            }
        }
    }
}

private fun openConsoleTrigger(
    keyEvent: KeyEvent,
    consoleViewModel: ConsoleViewModel
): Boolean {
    return if (keyEvent.key == Key.Backslash && keyEvent.type == KeyEventType.KeyDown) {
        consoleViewModel.toggleConsoleVisibility()
        true
    } else {
        false
    }
}

fun main() = application {
    val consoleViewModel = ConsoleViewModel()
    val chessBoardViewModel = ChessBoardViewModel()

    Window(
        title = GameDetails.title,
        //state = WindowState(position = WindowPosition(Alignment.Center)),
        onCloseRequest = ::exitApplication,
        onPreviewKeyEvent = { keyEvent -> openConsoleTrigger(keyEvent, consoleViewModel) }
    ) {
        App(
            chessBoardViewModel = chessBoardViewModel,
            consoleViewModel = consoleViewModel
        )
    }
}
