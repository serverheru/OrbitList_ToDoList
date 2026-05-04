package com.example.orbitlist

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "DAILY_BRIEFING") {
            showDailyBriefing(context)
            // Reschedule for next day
            NotificationHelper.scheduleDailyBriefing(context)
            return
        }

        val taskName = intent.getStringExtra("TASK_NAME") ?: "OrbitList"
        val taskEmoji = intent.getStringExtra("TASK_EMOJI") ?: "🚀"
        val taskId = intent.getIntExtra("TASK_ID", 0)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", taskId)
        val soundUriString = intent.getStringExtra("SOUND_URI")

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = if (!soundUriString.isNullOrEmpty()) {
            Uri.parse(soundUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val notification = NotificationCompat.Builder(context, "quantum_task_notifications")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$taskEmoji Misi OrbitList")
            .setContentText("Waktunya mengerjakan: $taskName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun showDailyBriefing(context: Context) {
        val database = TodoDatabase.getDatabase(context)
        val todoDao = database.todoDao()

        CoroutineScope(Dispatchers.IO).launch {
            val tasks = todoDao.getAllTasks().first()
            val today = Calendar.getInstance()
            val todayTasks = tasks.filter { item ->
                if (item.isDone) return@filter false
                if (item.dueDate == null) return@filter true // Consider tasks without date as "anytime"
                val taskDate = Calendar.getInstance().apply { timeInMillis = item.dueDate }
                taskDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                taskDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            }

            if (todayTasks.isNotEmpty()) {
                val title = "🚀 Laporan Briefing Pagi"
                val message = "Kapten, ada ${todayTasks.size} misi yang menunggu dalam orbit hari ini. Mari kita selesaikan!"
                
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    9999,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, "quantum_task_notifications")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(9999, notification)
            }
        }
    }
}

