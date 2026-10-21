package com.sally.blokku.core

import org.junit.Assert.assertEquals
import org.junit.Test

class JavaPickerTest {
    @Test fun old() = assertEquals(8, JavaPicker.forVersion("1.12.2"))
    @Test fun edge16() = assertEquals(8, JavaPicker.forVersion("1.16.5"))
    @Test fun mid() = assertEquals(17, JavaPicker.forVersion("1.20.1"))
    @Test fun edge204() = assertEquals(17, JavaPicker.forVersion("1.20.4"))
    @Test fun edge205() = assertEquals(21, JavaPicker.forVersion("1.20.5"))
    @Test fun new() = assertEquals(21, JavaPicker.forVersion("1.21.1"))
    @Test fun snapshot() = assertEquals(21, JavaPicker.forVersion("24w14a"))
    @Test fun ram() {
        assertEquals(1024, RamPolicy.snap(500))
        assertEquals(1536, RamPolicy.snap(1700))
        assertEquals(2048, RamPolicy.snap(9000))
    }
}
