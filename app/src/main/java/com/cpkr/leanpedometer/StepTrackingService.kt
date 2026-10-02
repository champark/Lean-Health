package com.cpkr.leanpedometer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class StepTrackingService :
    Service(),
    SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var stepStore: StepStore
    private var stepCounter: Sensor? = null
    private var listenerRegistered = false

    override fun onCreate() {
        super.onCreate()
        stepStore = StepStore(applicationContext)
        sensorManager =
            getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounter =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_STEP_COUNTER,
            )
        ensureNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (
            !hasActivityRecognitionPermission(this) ||
            stepCounter == null
        ) {
            stepStore.setTrackingEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )

        if (!listenerRegistered) {
            listenerRegistered =
                sensorManager.registerListener(
                    this,
                    stepCounter,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
        }

        if (!listenerRegistered) {
            stepStore.setTrackingEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }

        stepStore.setTrackingEnabled(true)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (
            event?.sensor?.type !=
            Sensor.TYPE_STEP_COUNTER
        ) {
            return
        }

        val raw =
            event.values.firstOrNull()
                ?.toLong()
                ?: return

        stepStore.recordSensorValue(raw)
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    override fun onDestroy() {
        if (listenerRegistered) {
            sensorManager.unregisterListener(this)
            listenerRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureNotificationChannel() {
        val manager =
            getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "걸음 수 기록",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description =
                    "Lean Pedometer가 걸음 수 센서를 기록 중임을 표시합니다."
            },
        )
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Lean Pedometer")
            .setContentText("걸음 수를 기록하고 있습니다.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    companion object {
        private const val CHANNEL_ID =
            "lean_pedometer_tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            if (!hasActivityRecognitionPermission(context)) {
                return
            }
            ContextCompat.startForegroundService(
                context,
                Intent(
                    context,
                    StepTrackingService::class.java,
                ),
            )
        }

        fun stop(context: Context) {
            StepStore(context.applicationContext)
                .setTrackingEnabled(false)
            context.stopService(
                Intent(
                    context,
                    StepTrackingService::class.java,
                ),
            )
        }
    }
}
