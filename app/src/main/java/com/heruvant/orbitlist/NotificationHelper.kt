package com.heruvant.orbitlist

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun scheduleDailyBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "DAILY_BRIEFING"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0) // Exact midnight
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (canScheduleExactAlarms(context)) {
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
    }

    fun scheduleNotification(context: Context, item: TodoItem) {
        if (item.dueDate == null || item.dueTime == null || item.isDone) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE)
        val globalSoundUri = prefs.getString("global_sound_uri", null)

        val intervals = listOf(
            -60 to " (1 Jam Lagi)",
            -5 to " (5 Menit Lagi)",
            0 to ""
        )

        intervals.forEachIndexed { index, (minutesOffset, labelSuffix) ->
            // UNIQUE ID FORMULA: TaskID * 10 + index (0, 1, 2)
            // This prevents collisions between different tasks and different intervals
            val requestID = item.id * 10 + index

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TASK_NAME", item.task + labelSuffix)
                putExtra("TASK_EMOJI", item.emoji)
                putExtra("TASK_ID", item.id)
                putExtra("SOUND_URI", globalSoundUri)
                putExtra("NOTIFICATION_ID", requestID)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = item.dueDate!!
                val timeParts = item.dueTime.split(":")
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, minutesOffset)
            }

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                if (canScheduleExactAlarms(context)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        }
    }

    fun cancelNotification(context: Context, itemId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(0, 1, 2).forEach { index ->
            val requestID = itemId * 10 + index
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
