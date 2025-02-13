package com.vrdc.notifications

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer
import java.io.IOException

class HadolintStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("Hadolint Notifications")

        try {
            val process = ProcessBuilder("hadolint", "--version").start()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                notificationGroup.createNotification(
                    "Hadolint missing",
                    "Hadolint is installed but returned error code $exitCode. Dockerfile validation will not work properly.",
                    NotificationType.WARNING
                ).notify(project)
            }
        } catch (e: IOException) {
            notificationGroup.createNotification(
                "Hadolint not found",
                "Hadolint executable not found in PATH. Dockerfile validation will not work. ${e.message}",
                NotificationType.ERROR
            ).notify(project)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Disposer.dispose(this)
        }
    }
}