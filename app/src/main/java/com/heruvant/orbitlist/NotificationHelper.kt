package com.heruvant.orbitlist

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "quantum_task_notifications"
    private const val CHANNEL_NAME = "Notifikasi OrbitList"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifikasi untuk pengingat tugas"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "DAILY_BRIEFING"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999, // Unique ID for daily briefing
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun scheduleNotification(context: Context, item: TodoItem) {
        if (item.dueDate == null || item.dueTime == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE)
        val globalSoundUri = prefs.getString("global_sound_uri", null)

        // Define the intervals: 1 hour before, 5 minutes before, and exactly at the time
        val intervals = listOf(
            -60 to " (1 Jam Lagi)",
            -5 to " (5 Menit Lagi)",
            0 to ""
        )

        intervals.forEach { (minutesOffset, labelSuffix) ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TASK_NAME", item.task + labelSuffix)
                putExtra("TASK_EMOJI", item.emoji)
                putExtra("TASK_ID", item.id)
                putExtra("SOUND_URI", globalSoundUri)
                // Use a unique notification ID for each interval to avoid overwriting
                putExtra("NOTIFICATION_ID", item.id * 10 + minutesOffset.coerceAtLeast(-60))
            }

            // requestID must be unique for each alarm to coexist
            val requestID = item.id * 10 + minutesOffset.coerceAtLeast(-60)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = item.dueDate
                val timeParts = item.dueTime.split(":")
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, minutesOffset)
            }

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelNotification(context: Context, itemId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel all three possible intervals
        listOf(0, -5, -60).forEach { offset ->
            val requestID = itemId * 10 + offset
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestID,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}