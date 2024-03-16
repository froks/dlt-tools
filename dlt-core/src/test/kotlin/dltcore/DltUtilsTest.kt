package dltcore

import kotlin.test.Test
import kotlin.test.assertEquals

class DltUtilsTest {
    @Test
    fun `test asStringValue`() {
        assertEquals("1234", 0x3031323334.toInt().asStringValue())
        assertEquals("123", 0x3031323300.toInt().asStringValue())
    }

    @org.junit.jupiter.api.Test
    fun `test asIntValue`() {
        assertEquals(0x3031323334.toInt(), "1234".asIntValue())
        assertEquals(0x3031323300.toInt(), "123".asIntValue())
    }
}
