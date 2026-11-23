package com.jhyunn.galaxystandby.media

import android.graphics.Bitmap

sealed interface MusicState {
    /** 알림 접근 권한이 없어 시스템 미디어 세션을 조회할 수 없음 */
    data object NoPermission : MusicState

    /** 권한은 있지만 활성 미디어 세션이 없음 */
    data object NoSession : MusicState

    data class Active(
        val sessionPackage: String,
        val title: String?,
        val artist: String?,
        val art: Bitmap?,
        val isPlaying: Boolean,
        /** 재생 상태가 마지막으로 갱신된 시점의 위치. 알 수 없으면 -1 */
        val positionMs: Long,
        /** positionMs가 기록된 SystemClock.elapsedRealtime() */
        val positionUpdatedAt: Long,
        val speed: Float,
        /** 알 수 없으면 0 이하 */
        val durationMs: Long,
        val canSkipPrevious: Boolean,
        val canSkipNext: Boolean,
        val canPlayPause: Boolean,
    ) : MusicState {
        fun positionAt(nowElapsed: Long): Long {
            if (positionMs < 0) return -1
            val advanced = if (isPlaying) ((nowElapsed - positionUpdatedAt) * speed).toLong() else 0L
            val pos = positionMs + advanced
            return if (durationMs > 0) pos.coerceIn(0, durationMs) else pos.coerceAtLeast(0)
        }
    }
}
