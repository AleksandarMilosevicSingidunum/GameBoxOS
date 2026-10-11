package com.gamebox.os.ui

internal enum class ControllerSearchKeyAction {
    INSERT,
    BACKSPACE,
    SPACE,
    CLEAR,
    SUBMIT,
}

internal data class ControllerSearchKey(
    val label: String,
    val contentDescription: String,
    val action: ControllerSearchKeyAction,
    val value: String = "",
)

internal sealed interface ControllerSearchKeyResult {
    data class QueryChanged(val query: String) : ControllerSearchKeyResult
    data object Submitted : ControllerSearchKeyResult
}

private fun characterRow(characters: String): List<ControllerSearchKey> =
    characters.map { character ->
        val value = character.toString()
        ControllerSearchKey(
            label = value,
            contentDescription = "Key $value",
            action = ControllerSearchKeyAction.INSERT,
            value = value,
        )
    }

private fun actionKey(
    label: String,
    contentDescription: String,
    action: ControllerSearchKeyAction,
) = ControllerSearchKey(label, contentDescription, action)

internal val controllerSearchKeyboardRows: List<List<ControllerSearchKey>> = listOf(
    characterRow("QWERTYUIOP"),
    characterRow("ASDFGHJKL"),
    characterRow("ZXCVBNM") + actionKey("Delete", "Backspace", ControllerSearchKeyAction.BACKSPACE),
    characterRow("1234567890"),
    listOf(
        actionKey("Space", "Space", ControllerSearchKeyAction.SPACE),
        actionKey("Clear", "Clear search", ControllerSearchKeyAction.CLEAR),
        actionKey("Search", "Finish search", ControllerSearchKeyAction.SUBMIT),
    ),
)

internal fun applyControllerSearchKey(
    query: String,
    key: ControllerSearchKey,
): ControllerSearchKeyResult = when (key.action) {
    ControllerSearchKeyAction.INSERT -> ControllerSearchKeyResult.QueryChanged(query + key.value)
    ControllerSearchKeyAction.BACKSPACE -> ControllerSearchKeyResult.QueryChanged(query.dropLast(1))
    ControllerSearchKeyAction.SPACE -> ControllerSearchKeyResult.QueryChanged(
        if (query.isNotEmpty() && !query.endsWith(' ')) "$query " else query,
    )
    ControllerSearchKeyAction.CLEAR -> ControllerSearchKeyResult.QueryChanged("")
    ControllerSearchKeyAction.SUBMIT -> ControllerSearchKeyResult.Submitted
}
