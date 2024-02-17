package composables

import androidx.compose.foundation.background
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import models.ConsoleViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun BlinkingCursor(prompt: String) {
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(key1 = alpha) {
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    1f at 500 with androidx.compose.animation.core.LinearEasing
                    0f at 1000 with androidx.compose.animation.core.LinearEasing
                }
            )
        )
    }

    Text(prompt, color = Color.White.copy(alpha = alpha.value))
}

@Composable
fun ConsoleInputField(
    viewModel: ConsoleViewModel
) {
    val textState = remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        BlinkingCursor(prompt = "->")

        TextField(
            value = textState.value,
            onValueChange = { textState.value = it },
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = Color.White,
                textColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                viewModel.executeCommand(textState.value)
                textState.value = ""
            }),
            modifier = Modifier.focusRequester(focusRequester)
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ConsoleOverlay(viewModel: ConsoleViewModel) {
    if (viewModel.isConsoleVisible) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(8.dp)
        ) {
            val scrollState = rememberScrollState()

            Text("Dev Console", color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(bottom = 8.dp)
                    .verticalScroll(scrollState)
            ) {
                viewModel.commandHistory.takeLast(5).forEach { command ->
                    Text(command, color = Color.White)
                }
            }

            ConsoleInputField(
                viewModel = viewModel
            )
        }
    }
}