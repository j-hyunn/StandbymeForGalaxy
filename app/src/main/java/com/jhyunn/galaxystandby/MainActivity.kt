package com.jhyunn.galaxystandby

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jhyunn.galaxystandby.media.MediaRepository
import com.jhyunn.galaxystandby.samsung.SamsungRoutines
import com.jhyunn.galaxystandby.ui.OnboardingScreen
import com.jhyunn.galaxystandby.ui.StandbyColors
import com.jhyunn.galaxystandby.ui.StandbyScreen
import com.jhyunn.galaxystandby.ui.StandbyTheme

class MainActivity : ComponentActivity() {

    private lateinit var media: MediaRepository

    private val prefs by lazy { getSharedPreferences("standby", MODE_PRIVATE) }
    private var showGuide by mutableStateOf(false)
    private var hasNotificationAccess by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        media = MediaRepository(this)
        showGuide = !prefs.getBoolean(KEY_ONBOARDING_DONE, false)

        // 충전 거치 중 잠금 화면 위에서도 바로 볼 수 있도록 한다. (화면을 강제로 켜지는 않음)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        setContent {
            StandbyTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(StandbyColors.Background)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                ) {
                    if (showGuide) {
                        OnboardingScreen(
                            hasNotificationAccess = hasNotificationAccess,
                            isSamsung = SamsungRoutines.isSamsungDevice(),
                            routinesInstalled = SamsungRoutines.isRoutinesInstalled(this@MainActivity),
                            onOpenNotificationAccess = ::openNotificationAccessSettings,
                            onOpenRoutines = { SamsungRoutines.open(this@MainActivity) },
                            onStart = {
                                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                                showGuide = false
                            },
                        )
                    } else {
                        // 스탠바이 화면이 보이는 동안에만 화면 켜짐을 유지한다.
                        DisposableEffect(Unit) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                        }
                        val music by media.state.collectAsStateWithLifecycle()
                        StandbyScreen(
                            music = music,
                            onPrevious = media::skipToPrevious,
                            onPlayPause = media::togglePlayPause,
                            onNext = media::skipToNext,
                            onRequestPermission = ::openNotificationAccessSettings,
                            onOpenGuide = { showGuide = true },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 설정 화면에서 돌아올 때마다 권한을 다시 확인한다.
        hasNotificationAccess = media.hasPermission()
        media.start()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onStop() {
        // 화면이 보이지 않을 때는 미디어 세션 콜백을 해제해 리소스를 반환한다.
        media.stop()
        super.onStop()
    }

    override fun onDestroy() {
        media.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun openNotificationAccessSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // 다음 후보 시도
            }
        }
    }

    private companion object {
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
