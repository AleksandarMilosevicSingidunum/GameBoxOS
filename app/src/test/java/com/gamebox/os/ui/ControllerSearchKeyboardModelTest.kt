package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerSearchKeyboardModelTest {
    @Test
    fun keyboardLayoutContainsEveryKeyInTheExpectedRows() {
        assertEquals(5, controllerSearchKeyboardRows.size)
        assertEquals("QWERTYUIOP", controllerSearchKeyboardRows[0].joinToString("") { it.label })
        assertEquals("ASDFGHJKL", controllerSearchKeyboardRows[1].joinToString("") { it.label })
        assertEquals(listOf("Z", "X", "C", "V", "B", "N", "M", "Delete"), controllerSearchKeyboardRows[2].map { it.label })
        assertEquals("1234567890", controllerSearchKeyboardRows[3].joinToString("") { it.label })
        assertEquals(listOf("Space", "Clear", "Search"), controllerSearchKeyboardRows[4].map { it.label })
    }

    @Test
    fun insertSpaceBackspaceAndClearProduceSafeQueryUpdates() {
        val insertZ = controllerSearchKeyboardRows[2].first()
        val space = controllerSearchKeyboardRows[4][0]
        val backspace = controllerSearchKeyboardRows[2].last()
        val clear = controllerSearchKeyboardRows[4][1]

        assertEquals(ControllerSearchKeyResult.QueryChanged("Z"), applyControllerSearchKey("", insertZ))
        assertEquals(ControllerSearchKeyResult.QueryChanged("Z "), applyControllerSearchKey("Z", space))
        assertEquals(ControllerSearchKeyResult.QueryChanged("Z "), applyControllerSearchKey("Z ", space))
        assertEquals(ControllerSearchKeyResult.QueryChanged(""), applyControllerSearchKey("", backspace))
        assertEquals(ControllerSearchKeyResult.QueryChanged("Z"), applyControllerSearchKey("Z ", backspace))
        assertEquals(ControllerSearchKeyResult.QueryChanged(""), applyControllerSearchKey("anything", clear))
    }

    @Test
    fun searchKeySubmitsWithoutChangingTheCurrentQuery() {
        val search = controllerSearchKeyboardRows.last().last()

        assertEquals(ControllerSearchKeyResult.Submitted, applyControllerSearchKey("Cave Story", search))
    }
}
