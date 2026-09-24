package ca.stewark.helioflux.ui.home

import android.content.Context

interface HomeSolarViewStore {
    fun read(): HomeSolarViewState
    fun save(view: HomeSolarViewState)
}

class SharedPreferencesHomeSolarViewStore(
    context: Context,
    name: String = "home_solar_view",
) : HomeSolarViewStore {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun read(): HomeSolarViewState =
        HomeSolarViewState(
            scale = preferences.getFloat("scale", 1f),
            panFractionX = preferences.getFloat("pan_x", 0f),
            panFractionY = preferences.getFloat("pan_y", 0f),
        ).normalized()

    override fun save(view: HomeSolarViewState) {
        val safe = view.normalized()
        preferences.edit()
            .putFloat("scale", safe.scale)
            .putFloat("pan_x", safe.panFractionX)
            .putFloat("pan_y", safe.panFractionY)
            .apply()
    }
}
