package ca.stewark.helioflux.core.data.parser

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class HmibcMetadataParserTest {
    private val imageUrl =
        "https://iswa.ccmc.gsfc.nasa.gov/iswa_data_tree/observation/solar/sdo/" +
            "hmi-magnetogram-color_2048x2048/2026/09/20260922_111500_2048_HMIBC.jpg"

    @Test
    fun parseValidLatestHmibcSample() {
        val record =
            HmibcMetadataParser.parse(
                """{"iswa_sdo_aia_hmic_files":{"samples":[{"timestamp":"2026-09-22 11:15:00.0","url":"$imageUrl"}]}}""",
            )

        assertNotNull(record)
        assertEquals(Instant.parse("2026-09-22T11:15:00Z").toEpochMilli(), record!!.timestampMillis)
        assertEquals(imageUrl, record.url)
    }

    @Test
    fun emptySamplesReturnNull() {
        assertNull(
            HmibcMetadataParser.parse(
                """{"iswa_sdo_aia_hmic_files":{"samples":[]}}""",
            ),
        )
    }

    @Test
    fun invalidTimestampReturnsNull() {
        assertNull(
            HmibcMetadataParser.parse(
                """{"iswa_sdo_aia_hmic_files":{"samples":[{"timestamp":"not-a-time","url":"$imageUrl"}]}}""",
            ),
        )
    }

    @Test
    fun nonHttpsImageUrlReturnsNull() {
        assertNull(
            HmibcMetadataParser.parse(
                """{"iswa_sdo_aia_hmic_files":{"samples":[{"timestamp":"2026-09-22 11:15:00.0","url":"http://example.test/HMIBC.jpg"}]}}""",
            ),
        )
    }
}
