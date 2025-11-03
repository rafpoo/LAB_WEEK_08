package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    // create a notification builder that will be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    // create a system handler that controls what thread the process is handled on
    private lateinit var serviceHandler: Handler

    // this is used for a two way communication
    // di tutorial ini ktia cuma pkae one way comms, jadi value nya null
    override fun onBind(intent: Intent): IBinder? = null

    // callback on create adalah bagian dari lifecycle nya
    override fun onCreate() {
        super.onCreate()

        // create the notfication with all of its contents and configs
        notificationBuilder = startForegroundService()

        // create the handler to control which thread the notif will be executed on
        // HandlerThread provides different threead for the process to be executed on
        // 'Handler' enqueues the process to HandlerThread to be executed
        // disiin, kita instansiasi HandlerThread baru "SecondThread"
        // kemudian HandlerThread tersebut dipass ke main handler "serviceHandler"
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    // custom func create the notif with all of its contents and configs
    private fun startForegroundService(): NotificationCompat.Builder {
        // create a pending intent that is executed when the user click the notif
        val pendingIntent = getPendingIntent()

        // to make notifs, we should know the keyword 'channel'
        // notifs menggunakan channel yang akan digfunakan untuk setup required confignya
        val channelId = createNotificationChannel()

        // Combine both pending intent and the channel into a notification builder
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        // setelah semua ready, mulai foreground service, kemudian notif akan muncul di layar user
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        // to create a pending intent, a flag is needed
        // flag adalah yang mengontrol apakah sebuah intent bisa dimodifikasi atau tidak nanti
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        // membuat agar ketika notif diklik, user diarahkan ke main activity
        // dengan cara set pending intent ke main activity
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String =
    // notification channel hanya ada untuk API 26 ke atas.
        // kita harus cek dulu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "002"
            val channelName = "002 Channel"

            // create the channel priority
            // priority ada IMPORTANCE_HIGH, IMPORTANCE_DEFAULT, dan IMPORTANCE_LOW
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            // build the channel notification based on previous 3 attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            // get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )

            // binds the channel into the NotrificationManager, that will trigger the notif later on
            service.createNotificationChannel(channel)

            channelId
        } else { "" }

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done, hi from second notification service")
            // setOnGoing mengatur apakah notif bisa di-dismiss atau tidak oleh user. Jika true, gabisa
            .setOngoing(true)

    // callback function ini adalah bagian dari lfiecycle
    // func ini akan dicall setelah service dimulai
    // in this case, setelah startForeground() dipanggil

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // posts the notification task to the handler
        // which will be executed on different thread
        serviceHandler.post {
            // sets up what happens after the notif is posted
            countDownFromTenToZero(notificationBuilder)

            // here, we are notifying the MainActivity that the service process is done
            // by returning the channel ID through LiveData
            notifyCompletion(Id)

            // stops the foreground service, which closes the notification
            // but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)

            // stop and destroy the service
            stopSelf()
        }

        return returnValue
    }

    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            // updates the notification context text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)

            // notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    // update the LiveData with the return channel id through the main thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableId.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xBEE
        const val EXTRA_ID = "Id"

        // this is LiveData, yang bisa update secara live dan update UI secara live
        private val mutableId = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableId
    }
}