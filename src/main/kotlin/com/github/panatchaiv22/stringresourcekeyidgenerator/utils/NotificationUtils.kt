package com.github.panatchaiv22.stringresourcekeyidgenerator.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object NotificationUtils {
    private const val NOTI_GROUP = "com.github.panatchaiv22.stringresourcekeyidgenerator"

    fun notify(msg: String, project: Project) {
        val notiGroup = NotificationGroupManager.getInstance().getNotificationGroup(NOTI_GROUP)
        val noti: Notification = notiGroup.createNotification(
            "String resource generator", "Error", msg, NotificationType.ERROR
        )
        noti.notify(project)
    }
}
