package ca.stewark.helioflux

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityPackageTest {
    @Test
    fun mainActivityUsesExpectedPackage() {
        assertEquals("ca.stewark.helioflux", MainActivity::class.java.packageName)
    }
}
