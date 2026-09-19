package ca.stewark.helioflux.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationPermission {
    fun shouldRequest(
        sdkInt: Int,
        alertsIntroduced: Boolean,
        alreadyGranted: Boolean,
    ): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU && alertsIntroduced && !alreadyGranted

    fun isGranted(context: android.content.Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
