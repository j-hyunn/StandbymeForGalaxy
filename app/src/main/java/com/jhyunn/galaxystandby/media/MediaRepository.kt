package com.jhyunn.galaxystandby.media

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 시스템의 활성 MediaSession을 관찰해 현재 음악 상태를 StateFlow로 노출하고,
 * 재생 컨트롤 명령을 선택된 세션에 전달한다.
 *
 * 메인 스레드에서만 호출한다. Activity의 onStart/onStop에 맞춰 start()/stop()을 호출한다.
 */
class MediaRepository(context: Context) {

    private val appContext = context.applicationContext
    private val sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(appContext, MediaListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<MusicState>(MusicState.NoSession)
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private val tracked = mutableListOf<Pair<MediaController, MediaController.Callback>>()
    private var current: MediaController? = null
    private var listening = false

    // URI로만 제공되는 앨범 커버 캐시 (마지막 1건)
    private var artUri: String? = null
    private var artFromUri: Bitmap? = null
    private var artJob: Job? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            track(controllers.orEmpty())
        }

    fun hasPermission(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(appContext)
            .contains(appContext.packageName)

    fun start() {
        if (listening) return
        if (!hasPermission()) {
            _state.value = MusicState.NoPermission
            return
        }
        try {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsListener, listenerComponent, mainHandler
            )
            listening = true
            track(sessionManager.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            // 권한 목록 반영이 늦거나 권한이 회수된 경우
            Log.w(TAG, "Media session access denied", e)
            stop()
            _state.value = MusicState.NoPermission
        }
    }

    fun stop() {
        if (listening) {
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
            listening = false
        }
        untrackAll()
        current = null
        artJob?.cancel()
    }

    /** Activity가 완전히 종료될 때 호출 */
    fun release() {
        stop()
        scope.cancel()
    }

    fun skipToPrevious() {
        current?.transportControls?.skipToPrevious()
    }

    fun skipToNext() {
        current?.transportControls?.skipToNext()
    }

    fun togglePlayPause() {
        val controller = current ?: return
        if (controller.playbackState.isActivelyPlaying()) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    private fun track(controllers: List<MediaController>) {
        untrackAll()
        for (controller in controllers) {
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
                override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
                override fun onSessionDestroyed() {
                    tracked.removeAll { it.first === controller }
                    runCatching { controller.unregisterCallback(this) }
                    refresh()
                }
            }
            controller.registerCallback(callback, mainHandler)
            tracked += controller to callback
        }
        refresh()
    }

    private fun untrackAll() {
        for ((controller, callback) in tracked) {
            runCatching { controller.unregisterCallback(callback) }
        }
        tracked.clear()
    }

    /** 재생 중인 세션을 우선하고, 없으면 기존 세션 → 시스템 우선순위 순으로 고른다. */
    private fun refresh() {
        val candidates = tracked.map { it.first }.filter { it.hasDisplayableContent() }
        val chosen = candidates.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: current?.let { cur -> candidates.firstOrNull { it.sessionToken == cur.sessionToken } }
            ?: candidates.firstOrNull()
        current = chosen
        _state.value = chosen?.toMusicState() ?: MusicState.NoSession
    }

    private fun MediaController.hasDisplayableContent(): Boolean {
        val state = playbackState?.state
        return metadata != null || (state != null && state != PlaybackState.STATE_NONE)
    }

    private fun MediaController.toMusicState(): MusicState.Active {
        val meta = metadata
        val playback = playbackState
        val actions = playback?.actions ?: 0L

        val title = meta?.textOf(MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = meta?.textOf(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
        )
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val playing = playback.isActivelyPlaying()
        val positionKnown = playback != null && playback.position != PlaybackState.PLAYBACK_POSITION_UNKNOWN

        return MusicState.Active(
            sessionPackage = packageName,
            title = title,
            artist = artist,
            art = meta?.let(::resolveArt),
            isPlaying = playing,
            positionMs = if (positionKnown) playback!!.position else -1L,
            positionUpdatedAt = playback?.lastPositionUpdateTime?.takeIf { it > 0 }
                ?: SystemClock.elapsedRealtime(),
            speed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            durationMs = duration,
            // actions 정보를 제공하지 않는 앱도 있어, 0이면 모든 버튼을 허용한다.
            canSkipPrevious = actions == 0L || actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canSkipNext = actions == 0L || actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canPlayPause = actions == 0L || actions and (
                PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE
                ) != 0L,
        )
    }

    private fun resolveArt(meta: MediaMetadata): Bitmap? {
        val bitmap = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bitmap != null) return bitmap

        val uri = meta.textOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        ) ?: return null

        if (uri == artUri) return artFromUri
        artUri = uri
        artFromUri = null
        artJob?.cancel()
        artJob = scope.launch {
            val loaded = withContext(Dispatchers.IO) { loadBitmap(Uri.parse(uri)) }
            if (artUri == uri && loaded != null) {
                artFromUri = loaded
                refresh()
            }
        }
        return null
    }

    /** 로컬에서 읽을 수 있는 URI만 처리한다. 네트워크 URL은 앱별 인증이 필요할 수 있어 지원하지 않는다. */
    private fun loadBitmap(uri: Uri): Bitmap? = runCatching {
        if (uri.scheme !in LOCAL_SCHEMES) return null
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_ART_PX && bounds.outHeight / (sample * 2) >= MAX_ART_PX) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.onFailure { Log.w(TAG, "Album art load failed: $uri", it) }.getOrNull()

    private fun MediaMetadata.textOf(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> getString(key)?.takeIf { it.isNotBlank() } }

    private fun PlaybackState?.isActivelyPlaying(): Boolean = when (this?.state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING -> true
        else -> false
    }

    private companion object {
        const val TAG = "MediaRepository"
        const val MAX_ART_PX = 512
        val LOCAL_SCHEMES = setOf(
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE,
            ContentResolver.SCHEME_ANDROID_RESOURCE,
        )
    }
}
