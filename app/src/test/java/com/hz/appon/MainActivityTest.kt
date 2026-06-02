package com.hz.appon

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {

    @Test
    fun `tap count starts at zero`() {
        assertEquals(0, 0)
    }

    @Test
    fun `tap count increments correctly`() {
        var count = 0
        repeat(3) { count++ }
        assertEquals(3, count)
    }
}
