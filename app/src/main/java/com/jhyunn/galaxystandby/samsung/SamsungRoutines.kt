package com.jhyunn.galaxystandby.samsung

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Samsung 기기 전용 기능. 일반 Android 기능과 분리해 둔다.
 * 자동 실행 자체는 Samsung Modes and Routines에 맡기고, 여기서는 설정 화면으로 안내만 한다.
 */
object SamsungRoutines {
    private const val ROUTINES_PACKAGE = "com.samsung.android.app.routines"

    fun isSamsungDevice(): Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    fun isRoutinesInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(ROUTINES_PACKAGE) != null

    /**
     * Modes and Routines 앱을 열고, 직접 열 수 없으면 시스템 설정을 연다.
     * @return Modes and Routines를 직접 열었으면 true
     */
    fun open(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(ROUTINES_PACKAGE)
        if (intent != null) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
                // 설정으로 대체
            } catch (_: SecurityException) {
                // 설정으로 대체
            }
        }
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return false
    }
}
