package models

import api.Coordinate
import androidx.compose.runtime.*
import api.distance

class ConsoleViewModel {
    val commandHistory = mutableStateListOf<String>()

    var isDebug by mutableStateOf(false)
        private set
    var isConsoleVisible by mutableStateOf(false)
        private set

    fun toggleConsoleVisibility() {
        isConsoleVisible = !isConsoleVisible
    }

    fun executeCommand(command: String) {
        commandHistory.add("> $command")

        val parts = command.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.isEmpty()) return

        val issuing = parts[0].lowercase()
        when (issuing) {
            "debug" -> isDebug = !isDebug
            "distance" -> {
                try {
                    if (parts.size != 5) return
                    val from = Coordinate(parts[1].toInt(), parts[2].toInt())
                    val to = Coordinate(parts[3].toInt(), parts[4].toInt())
                    val distance = from.distance(to)
                    commandHistory.add("> $distance")
                } catch (e: NumberFormatException) {
                    commandHistory.add("> Invalid input")
                }
            }
        }
    }
}
