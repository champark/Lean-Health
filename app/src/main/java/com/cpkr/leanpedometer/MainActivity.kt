package com.cpkr.leanpedometer

import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors =
                if (isSystemInDarkTheme()) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }

            MaterialTheme(colorScheme = colors) {
                LeanPedometerScreen()
            }
        }
    }
}

@Composable
private fun LeanPedometerScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val store =
        remember(appContext) {
            StepStore(appContext)
        }
    val sensorAvailable =
        remember(appContext) {
            val manager =
                appContext.getSystemService(
                    SensorManager::class.java,
                )
            manager?.getDefaultSensor(
                Sensor.TYPE_STEP_COUNTER,
            ) != null
        }

    var steps by remember {
        mutableLongStateOf(store.getTodaySteps())
    }
    var trackingEnabled by remember {
        mutableStateOf(store.isTrackingEnabled())
    }
    var recentDays by remember {
        mutableStateOf(store.getRecentDays(7))
    }
    var permissionRefresh by remember {
        mutableLongStateOf(0L)
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            permissionRefresh++
        }

    fun startTracking() {
        StepTrackingService.start(appContext)
        trackingEnabled = true

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(appContext)
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    val activityPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            permissionRefresh++
            if (granted) {
                startTracking()
            }
        }

    fun requestStart() {
        if (!sensorAvailable) {
            return
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q &&
            !hasActivityRecognitionPermission(appContext)
        ) {
            activityPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION,
            )
        } else {
            startTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (
            store.isTrackingEnabled() &&
            hasActivityRecognitionPermission(appContext)
        ) {
            StepTrackingService.start(appContext)
        }

        while (true) {
            steps = store.getTodaySteps()
            trackingEnabled =
                store.isTrackingEnabled()
            recentDays =
                store.getRecentDays(7)
            delay(1_000L)
        }
    }

    val recognitionGranted =
        remember(permissionRefresh) {
            hasActivityRecognitionPermission(
                appContext,
            )
        }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Lean Pedometer",
                style =
                    MaterialTheme.typography
                        .headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text =
                    when {
                        !sensorAvailable ->
                            "이 기기에는 걸음 수 센서가 없습니다."

                        !recognitionGranted ->
                            "활동 인식 권한이 필요합니다."

                        trackingEnabled ->
                            "걸음 수 기록 활성화"

                        else ->
                            "걸음 수 기록 중지됨"
                    },
                style =
                    MaterialTheme.typography.bodyMedium,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "오늘",
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                    )
                    Spacer(
                        modifier =
                            Modifier.height(8.dp),
                    )
                    Text(
                        text =
                            NumberFormat
                                .getNumberInstance(
                                    Locale.getDefault(),
                                )
                                .format(steps),
                        style =
                            MaterialTheme.typography
                                .displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "걸음",
                        style =
                            MaterialTheme.typography
                                .bodyLarge,
                    )
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { requestStart() },
                    enabled =
                        sensorAvailable &&
                            !trackingEnabled,
                ) {
                    Text("기록 시작")
                }

                OutlinedButton(
                    onClick = {
                        StepTrackingService.stop(
                            appContext,
                        )
                        trackingEnabled = false
                    },
                    enabled = trackingEnabled,
                ) {
                    Text("기록 중지")
                }
            }

            Text(
                text = "최근 7일",
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            recentDays.forEach { item ->
                HistoryRow(
                    date = item.date,
                    steps = item.steps,
                )
            }

            Spacer(
                modifier =
                    Modifier.weight(1f),
            )

            Text(
                text =
                    "Lean Diary는 이 앱의 읽기 전용 Provider를 통해 날짜별 걸음 수를 직접 조회할 수 있습니다.",
                style =
                    MaterialTheme.typography.bodySmall,
            )

            Text(
                text =
                    "처음 설치한 날에 휴대폰이 자정 이전부터 켜져 있었다면 설치 전 걸음 수는 정확히 복원할 수 없습니다.",
                style =
                    MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    date: LocalDate,
    steps: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
    ) {
        Text(
            text =
                date.format(
                    DateTimeFormatter
                        .ofPattern("MM.dd"),
                ),
        )
        Text(
            text =
                NumberFormat
                    .getNumberInstance(
                        Locale.getDefault(),
                    )
                    .format(steps),
            fontWeight = FontWeight.Medium,
        )
    }
}
