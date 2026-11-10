package com.example.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private const val RELEASES_URL = "https://api.github.com/repos/spacpraise/Line-calll/releases/latest"
private const val UPDATE_CHANNEL = "line_call_updates"
private const val UPDATE_NOTIFICATION_ID = 7301
private const val UPDATE_PREFS = "line_call_update_state"
private const val PENDING_APK = "pending_apk"
private const val PENDING_VERSION = "pending_version"
private const val PENDING_NOTES = "pending_notes"

object LineCallUpdateManager {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<ReleaseCheckWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "line_call_periodic_release_check",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
        val immediate = OneTimeWorkRequestBuilder<ReleaseCheckWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "line_call_open_release_check",
            ExistingWorkPolicy.KEEP,
            immediate
        )
    }

    fun hasPendingUpdate(context: Context): Boolean {
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val path = prefs.getString(PENDING_APK, null) ?: return false
        val apk = File(path)
        if (!apk.isFile || apk.length() == 0L) {
            clearPending(context)
            return false
        }
        val installed = installedVersionCode(context)
        val candidate = archiveVersionCode(context, apk) ?: return false
        if (installed >= candidate) {
            clearPending(context)
            return false
        }
        return true
    }

    fun pendingVersion(context: Context): String =
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .getString(PENDING_VERSION, "") ?: ""

    fun pendingNotes(context: Context): String =
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .getString(PENDING_NOTES, "") ?: ""

    fun installPending(context: Context): Boolean {
        if (!hasPendingUpdate(context)) return false
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val apk = File(prefs.getString(PENDING_APK, "") ?: return false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(permissionIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(install)
            true
        } catch (_: Exception) {
            false
        }
    }

    internal fun savePending(context: Context, apk: File, version: String, notes: String) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(PENDING_APK, apk.absolutePath)
            .putString(PENDING_VERSION, version)
            .putString(PENDING_NOTES, notes)
            .apply()
        showUpdateNotification(context, version, notes)
    }

    internal fun clearPending(context: Context) {
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        prefs.getString(PENDING_APK, null)?.let { File(it).delete() }
        prefs.edit().remove(PENDING_APK).remove(PENDING_VERSION).remove(PENDING_NOTES).apply()
        NotificationManagerCompat.from(context).cancel(UPDATE_NOTIFICATION_ID)
    }

    internal fun showUpdateNotification(context: Context, version: String, notes: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(UPDATE_CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = PendingIntent.getActivity(
            context, UPDATE_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("linecall_install_update", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val detail = notes.trim().take(180).ifBlank { "Bug fixes and improvements are available." }
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Line Call update available")
            .setContentText("Version $version — $detail")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Version $version\n\n$detail\n\nYour downloaded update is kept until installation is confirmed."))
            .setContentIntent(openApp)
            .addAction(0, "Install update", openApp)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification) } catch (_: SecurityException) { }
    }

    @Suppress("DEPRECATION")
    internal fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    @Suppress("DEPRECATION")
    internal fun archiveVersionCode(context: Context, apk: File): Long? {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
        } ?: return null
        if (info.packageName != context.packageName) return null
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    fun reconcileInstalledVersion(context: Context) {
        if (!hasPendingUpdate(context)) return
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val apk = File(prefs.getString(PENDING_APK, "") ?: return)
        val downloadedCode = archiveVersionCode(context, apk) ?: return
        if (installedVersionCode(context) >= downloadedCode) clearPending(context)
    }
}

class ReleaseCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LineCallUpdateManager.reconcileInstalledVersion(applicationContext)
            if (LineCallUpdateManager.hasPendingUpdate(applicationContext)) {
                LineCallUpdateManager.showUpdateNotification(
                    applicationContext,
                    LineCallUpdateManager.pendingVersion(applicationContext),
                    LineCallUpdateManager.pendingNotes(applicationContext)
                )
                return@withContext Result.success()
            }

            val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "LineCall-Android-Updater")
            }
            val release = try {
                if (connection.responseCode !in 200..299) return@withContext Result.retry()
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally { connection.disconnect() }

            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext Result.success()
            val tag = release.optString("tag_name").removePrefix("v")
            val assets = release.optJSONArray("assets") ?: return@withContext Result.success()
            var apkUrl: String? = null
            var apkName = "line-call-update.apk"
            var expectedDigest = ""
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    apkName = name
                    expectedDigest = asset.optString("digest")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext Result.success()

            val directory = File(applicationContext.filesDir, "updates").apply { mkdirs() }
            val finalApk = File(directory, apkName)
            val partial = File(directory, "$apkName.part")
            val download = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                setRequestProperty("User-Agent", "LineCall-Android-Updater")
            }
            try {
                if (download.responseCode !in 200..299) return@withContext Result.retry()
                download.inputStream.use { input ->
                    FileOutputStream(partial).use { output -> input.copyTo(output) }
                }
            } finally { download.disconnect() }

            if (expectedDigest.startsWith("sha256:", ignoreCase = true)) {
                val digest = MessageDigest.getInstance("SHA-256")
                partial.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                val actualDigest = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actualDigest.equals(expectedDigest.substringAfter(":"), ignoreCase = true)) {
                    partial.delete()
                    return@withContext Result.failure()
                }
            }

            val candidateCode = LineCallUpdateManager.archiveVersionCode(applicationContext, partial)
                ?: run { partial.delete(); return@withContext Result.failure() }
            if (candidateCode <= LineCallUpdateManager.installedVersionCode(applicationContext)) {
                partial.delete()
                return@withContext Result.success()
            }
            if (finalApk.exists()) finalApk.delete()
            if (!partial.renameTo(finalApk)) {
                partial.copyTo(finalApk, overwrite = true)
                partial.delete()
            }
            val notes = release.optString("body").take(4000)
            LineCallUpdateManager.savePending(applicationContext, finalApk, tag, notes)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
