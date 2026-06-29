package com.sksdesign.sharkaudio.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PlayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PlayerNotificationController.ACTION_PREVIOUS -> SharkAudioRuntime.controller?.previous()
            PlayerNotificationController.ACTION_TOGGLE -> SharkAudioRuntime.controller?.toggle()
            PlayerNotificationController.ACTION_NEXT -> SharkAudioRuntime.controller?.next()
        }
    }
}
