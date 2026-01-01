package com.goodsauce.home_master.logic

import com.goodsauce.home_master.logic.ntfy.NtfyClient
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val ntfyClient: NtfyClient
) {
    fun sendNotification(message : String, notificationSubtopic : String) {
        ntfyClient.notify(message, notificationSubtopic)
    }
}