package com.tvplayer.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val packageName: String,
    val label: String,
    /** TV banner (320×180) if the app has one, else its icon. */
    val image: ImageBitmap,
    val hasBanner: Boolean,
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pm: PackageManager = context.packageManager

    /** Apps with a TV launcher entry, minus this launcher, sorted by name. */
    suspend fun tvApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val activity = info.activityInfo
                val banner = activity.loadBanner(pm) ?: activity.applicationInfo.loadBanner(pm)
                val drawable = banner ?: activity.loadIcon(pm)
                AppInfo(
                    packageName = activity.packageName,
                    label = info.loadLabel(pm).toString(),
                    image = if (banner != null) drawable.toBitmap(320, 180).asImageBitmap()
                            else drawable.toBitmap(160, 160).asImageBitmap(),
                    hasBanner = banner != null,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun label(packageName: String): String? = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    /** @return false if the app isn't installed (or has no launchable activity). */
    fun launch(packageName: String): Boolean {
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    /** Android TV / Google TV system settings. */
    fun openSystemSettings() {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
