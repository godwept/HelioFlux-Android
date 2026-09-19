package ca.stewark.helioflux.core.database

import androidx.room.Room
import org.robolectric.RuntimeEnvironment

fun inMemoryDatabase(): HelioFluxDatabase =
    Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        HelioFluxDatabase::class.java,
    ).allowMainThreadQueries().build()
