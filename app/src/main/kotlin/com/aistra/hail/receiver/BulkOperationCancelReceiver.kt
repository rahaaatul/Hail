package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.utils.BulkOperationNotifier

class BulkOperationCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BulkOperationNotifier.ACTION_CANCEL) {
            BulkOperationNotifier.cancel()
        }
    }
}