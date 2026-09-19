package ca.stewark.helioflux.core.data.network

import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonConfigTest {
    @Test
    fun `mixed NOAA numeric json remains accessible`() {
        val value = helioFluxJson.parseToJsonElement("""{"known":12.5,"unknown":"ignored"}""").jsonObject
        assertEquals(12.5, value.getValue("known").jsonPrimitive.double, 0.0)
        assertEquals("ignored", value.getValue("unknown").jsonPrimitive.content)
    }
}
