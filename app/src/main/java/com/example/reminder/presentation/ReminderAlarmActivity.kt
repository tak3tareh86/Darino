package com.example.reminder.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.reminder.receiver.ReminderBroadcastReceiver

class ReminderAlarmActivity : ComponentActivity() {

    private var reminderId: String = ""
    private var notificationId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        reminderId = intent.getStringExtra(EXTRA_REMINDER_ID).orEmpty()
        notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "یادآوری دارینو" }
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        setContent {
            MaterialTheme {
                ReminderAlarmContent(
                    title = title,
                    body = body,
                    onComplete = { completeReminder() },
                    onSnooze = { snoozeReminder() },
                    onDismiss = { dismissAlarm() }
                )
            }
        }
    }

    private fun completeReminder() {
        if (reminderId.isNotBlank()) {
            sendBroadcast(Intent(this, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_COMPLETE_REMINDER
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            })
        } else {
            dismissAlarm()
            return
        }
        finish()
    }

    private fun snoozeReminder() {
        if (reminderId.isNotBlank()) {
            sendBroadcast(Intent(this, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_SNOOZE_REMINDER
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderBroadcastReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(ReminderBroadcastReceiver.EXTRA_SNOOZE_MINUTES, 15)
            })
        }
        finish()
    }

    private fun dismissAlarm() {
        if (notificationId != -1) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        finish()
    }

    companion object {
        const val EXTRA_REMINDER_ID = "alarm_extra_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "alarm_extra_notification_id"
        const val EXTRA_TITLE = "alarm_extra_title"
        const val EXTRA_BODY = "alarm_extra_body"
    }
}

@Composable
private fun ReminderAlarmContent(
    title: String,
    body: String,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
 ) {
    BackHandler(onBack = onDismiss)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101827)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Alarm,
                contentDescription = null,
                tint = Color(0xFF35C5B3),
                modifier = Modifier.height(76.dp)
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "زمان یادآوری فرا رسیده",
                color = Color(0xFF8FE7DC),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2938))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (body.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = body,
                            color = Color(0xFFD0D9E4),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A995))
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("انجام شد", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Snooze, contentDescription = null)
                Text("۱۵ دقیقه بعد", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("بستن هشدار")
            }
        }
    }
}
