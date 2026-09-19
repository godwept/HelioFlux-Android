package ca.stewark.helioflux

import android.app.Application

class HelioFluxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)
    }
}
