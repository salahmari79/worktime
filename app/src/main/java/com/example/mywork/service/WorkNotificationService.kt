package com.example.mywork.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mywork.R
import com.example.mywork.ui.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class WorkNotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Work Hours",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for work hours management"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleMorningAlarm(alarmTime: LocalDateTime) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MORNING_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmTimeMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(alarmTimeMillis, pendingIntent),
            pendingIntent
        )
    }

    fun showHourlyNotification(hourNumber: Int, tasksRemaining: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Work Progress Update")
            .setContentText("Hour $hourNumber: $tasksRemaining tasks remaining")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(HOURLY_NOTIFICATION_ID + hourNumber, notification)
    }

    companion object {
        const val CHANNEL_ID = "work_hours_channel"
        const val ACTION_MORNING_ALARM = "com.example.mywork.MORNING_ALARM"
        const val MORNING_ALARM_REQUEST_CODE = 1001
        const val HOURLY_NOTIFICATION_ID = 2000
    }
} 