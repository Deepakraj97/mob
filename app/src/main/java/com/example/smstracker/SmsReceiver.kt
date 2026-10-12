package com.example.smstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long SMS arrive in parts; join parts from the same sender
        val bySender = msgs.groupBy { it.originatingAddress ?: "" }
        for ((sender, parts) in bySender) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            val ts = parts.first().timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
            SmsParser.parse(sender, body, ts)?.let { TransactionStore.add(context, it) }
        }
    }
}
