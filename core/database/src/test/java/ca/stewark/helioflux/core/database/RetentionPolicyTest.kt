package ca.stewark.helioflux.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionPolicyTest {
 @Test fun normalSeriesKeepAtLeast48HoursPlusMargin(){ assertTrue(RetentionPolicy.NORMAL_SERIES_MILLIS > 48 * RetentionPolicy.HOUR_MILLIS); assertEquals(1_000L-RetentionPolicy.NORMAL_SERIES_MILLIS,RetentionPolicy.normalSeriesCutoff(1_000L)) }
 @Test fun solarActivityAndEventsCoverDisplayed72HoursPlusMargin(){ assertTrue(RetentionPolicy.SOLAR_ACTIVITY_SERIES_MILLIS > 72 * RetentionPolicy.HOUR_MILLIS); assertTrue(RetentionPolicy.EVENT_MILLIS > 72 * RetentionPolicy.HOUR_MILLIS) }
 @Test fun onlyLatestFewOvationSnapshotsAreRetained(){ assertEquals(3,RetentionPolicy.OVATION_SNAPSHOT_COUNT) }
}
