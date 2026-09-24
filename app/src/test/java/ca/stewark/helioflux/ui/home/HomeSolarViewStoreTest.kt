package ca.stewark.helioflux.ui.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeSolarViewStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun freshStoreUsesDefaultAndNewInstanceReadsSavedView() {
        val name = "home-solar-view-test-roundtrip"
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        val store = SharedPreferencesHomeSolarViewStore(context, name)
        assertEquals(HomeSolarViewState(), store.read())
        val saved = HomeSolarViewState(2f, 0.25f, -0.2f)
        store.save(saved)
        assertEquals(saved, SharedPreferencesHomeSolarViewStore(context, name).read())
    }

    @Test fun corruptValuesAreSanitizedAndResetClearsPosition() {
        val name = "home-solar-view-test-corrupt"
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            .putFloat("scale", Float.NaN)
            .putFloat("pan_x", Float.POSITIVE_INFINITY)
            .putFloat("pan_y", 99f)
            .commit()
        val store = SharedPreferencesHomeSolarViewStore(context, name)
        assertEquals(HomeSolarViewState(), store.read())
        store.save(HomeSolarViewState(4f, 1f, 1f))
        store.save(HomeSolarViewState())
        assertEquals(HomeSolarViewState(), SharedPreferencesHomeSolarViewStore(context, name).read())
    }
}
