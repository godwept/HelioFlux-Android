package ca.stewark.helioflux.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkImageLoadingDependencyTest {
    @Test
    fun coilHasNetworkFetcherForRemoteImagery() {
        val catalog = File("../gradle/libs.versions.toml").readText()
        val build = File("build.gradle.kts").readText()
        assertTrue(catalog.contains("io.coil-kt.coil3:coil-network-okhttp"))
        assertTrue(build.contains("implementation(libs.coil.network.okhttp)"))
        assertTrue(catalog.contains("io.coil-kt.coil3:coil-gif"))
        assertTrue(build.contains("implementation(libs.coil.gif)"))
    }

    @Test
    fun coilSingletonUsesProgressAwareOkHttpFetcher() {
        val containerSource = File("src/main/java/ca/stewark/helioflux/AppContainer.kt").readText()
        val applicationSource = File("src/main/java/ca/stewark/helioflux/HelioFluxApplication.kt").readText()

        assertTrue(containerSource.contains("OkHttpNetworkFetcherFactory"))
        assertTrue(containerSource.contains("ImageDownloadProgressInterceptor"))
        assertTrue(applicationSource.contains("SingletonImageLoader.setSafe"))
        assertTrue(applicationSource.contains("container.imageLoader"))
    }
}
