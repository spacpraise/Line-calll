package com.example.services

import android.app.backup.BackupManager
import android.content.Context

/** Requests an Android Auto Backup run after local contacts or call history change. */
object LocalBackupScheduler {
  fun request(context: Context) {
    runCatching { BackupManager(context.applicationContext).dataChanged() }
  }
}
