package com.example.core.data.notification

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationRepository private constructor(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "fomo_database"
    )
    .fallbackToDestructiveMigration()
    .build()

    private val dao = database.notificationDao()

    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val unreadCount: Flow<Int> = dao.getUnreadCount()

    suspend fun insert(notification: NotificationEntity) {
        dao.insertNotification(notification)
    }

    suspend fun markAsRead(id: Int) {
        dao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        dao.markAllAsRead()
    }

    suspend fun delete(id: Int) {
        dao.deleteNotification(id)
    }

    suspend fun clearAll() {
        dao.clearAllNotifications()
    }

    companion object {
        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context)
                INSTANCE = instance
                // Seed some initial unread notifications on first launch!
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val list = instance.allNotifications.firstOrNull()
                        if (list.isNullOrEmpty()) {
                            instance.seedNotifications()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                instance
            }
        }
    }

    private suspend fun seedNotifications() {
        insert(
            NotificationEntity(
                type = "MENTION",
                senderName = "Thabo_M",
                senderAvatar = "https://i.pravatar.cc/150?img=11",
                content = "mentioned you: '@jordan_r Are you heading to Amapiano Fridays tonight?'",
                timestamp = System.currentTimeMillis() - 15 * 60 * 1000, // 15 mins ago
                isRead = false,
                targetId = "evt_fomo_amapiano"
            )
        )
        insert(
            NotificationEntity(
                type = "FOLLOW",
                senderName = "Sarah Jenkins",
                senderAvatar = "https://i.pravatar.cc/150?img=32",
                content = "started following you",
                timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000, // 2 hours ago
                isRead = false
            )
        )
        insert(
            NotificationEntity(
                type = "LIKE",
                senderName = "Sipho_S",
                senderAvatar = "https://i.pravatar.cc/150?img=15",
                content = "liked your story from Cape Town",
                timestamp = System.currentTimeMillis() - 5 * 60 * 60 * 1000, // 5 hours ago
                isRead = false
            )
        )
        insert(
            NotificationEntity(
                type = "LIKE",
                senderName = "Naledi_K",
                senderAvatar = "https://i.pravatar.cc/150?img=26",
                content = "liked your post 'Amapiano • With 5 Friends'",
                timestamp = System.currentTimeMillis() - 24 * 60 * 60 * 1000, // 1 day ago
                isRead = true
            )
        )
    }
}
