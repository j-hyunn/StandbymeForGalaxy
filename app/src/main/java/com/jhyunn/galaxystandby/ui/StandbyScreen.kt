package com.jhyunn.galaxystandby.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.jhyunn.galaxystandby.R
import com.jhyunn.galaxystandby.media.MusicState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StandbyScreen(
    music: MusicState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(StandbyColors.Background)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClockPanel(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                // 일반 터치에는 아무 메뉴도 띄우지 않고, 길게 누를 때만 설정 안내를 다시 연다.
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenGuide() }) },
        )
        Box(
            Modifier
                .padding(horizontal = 40.dp)
                .width(1.dp)
                .fillMaxHeight(0.5f)
                .background(StandbyColors.Divider)
        )
        MusicPanel(
            state = music,
            onPrevious = onPrevious,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onRequestPermission = onRequestPermission,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

// ---------------------------------------------------------------- Clock

@Composable
private fun ClockPanel(modifier: Modifier = Modifier) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            // 다음 초 경계에 맞춰 갱신해 분이 바뀌는 순간을 놓치지 않는다.
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    val dayFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        // 폰트 크기를 dp 기준으로 계산해 시스템 글꼴 배율에 따라 레이아웃이 깨지지 않도록 한다.
        val clockSize = min(maxWidth * 0.34f, maxHeight * 0.5f)
        val density = LocalDensity.current
        val clockSp = with(density) { clockSize.toSp() }
        val dateSp = with(density) { (clockSize * 0.16f).coerceAtLeast(16.dp).toSp() }

        Column {
            Text(
                text = now.format(timeFormat),
                style = TextStyle(
                    color = StandbyColors.TextPrimary,
                    fontSize = clockSp,
                    lineHeight = clockSp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.02).sp * clockSp.value,
                ),
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${now.format(dateFormat)}  ${now.format(dayFormat)}",
                style = TextStyle(
                    color = StandbyColors.TextSecondary,
                    fontSize = dateSp,
                    fontWeight = FontWeight.Normal,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Music

@Composable
private fun MusicPanel(
    state: MusicState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val artSize = min(maxHeight * 0.62f, maxWidth * 0.42f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(state = state, size = artSize)
            Spacer(Modifier.width(32.dp))
            Column(Modifier.weight(1f)) {
                when (state) {
                    MusicState.NoPermission -> PermissionPrompt(onRequestPermission)
                    MusicState.NoSession -> TrackInfo(
                        title = "No music playing",
                        subtitle = "음악 앱에서 재생을 시작하세요",
                        state = null,
                        controlsEnabled = false,
                        onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext,
                    )
                    is MusicState.Active -> TrackInfo(
                        title = state.title ?: "제목 정보 없음",
                        subtitle = state.artist ?: "",
                        state = state,
                        controlsEnabled = true,
                        onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext,
                    )
                }
            }
        }
    }
}

@Composable
private fun Artwork(state: MusicState, size: Dp) {
    val shape = RoundedCornerShape(12.dp)
    val art = (state as? MusicState.Active)?.art
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(StandbyColors.Divider.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "앨범 커버",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = StandbyColors.TextSecondary,
                modifier = Modifier.size(size * 0.36f),
            )
        }
    }
}

@Composable
private fun TrackInfo(
    title: String,
    subtitle: String,
    state: MusicState.Active?,
    controlsEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Text(
        text = title,
        color = StandbyColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        color = StandbyColors.TextSecondary,
        fontSize = 17.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(24.dp))
    PlaybackProgress(state)
    Spacer(Modifier.height(16.dp))
    Controls(
        enabled = controlsEnabled,
        isPlaying = state?.isPlaying == true,
        canPrevious = state?.canSkipPrevious ?: false,
        canPlayPause = state?.canPlayPause ?: false,
        canNext = state?.canSkipNext ?: false,
        onPrevious = onPrevious,
        onPlayPause = onPlayPause,
        onNext = onNext,
    )
}

@Composable
private fun PlaybackProgress(state: MusicState.Active?) {
    // 재생 중일 때만 0.5초마다 위치를 다시 계산한다. 일시정지/미재생 시에는 갱신하지 않는다.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val playing = state?.isPlaying == true
    LaunchedEffect(playing, state?.positionMs, state?.positionUpdatedAt) {
        now = SystemClock.elapsedRealtime()
        while (playing) {
            delay(500)
            now = SystemClock.elapsedRealtime()
        }
    }
    val duration = state?.durationMs ?: 0L
    val position = state?.positionAt(now) ?: -1L
    val fraction = if (duration > 0 && position >= 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(StandbyColors.Divider)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(StandbyColors.TextPrimary)
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val known = duration > 0 && position >= 0
        Text(if (known) formatTime(position) else " ", color = StandbyColors.TextSecondary, fontSize = 13.sp)
        Text(if (known) formatTime(duration) else " ", color = StandbyColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun Controls(
    enabled: Boolean,
    isPlaying: Boolean,
    canPrevious: Boolean,
    canPlayPause: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(R.drawable.ic_skip_previous, "이전 곡", enabled && canPrevious, 56.dp, 34.dp, onPrevious)
        ControlButton(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) "일시정지" else "재생",
            enabled && canPlayPause, 72.dp, 48.dp, onPlayPause,
        )
        ControlButton(R.drawable.ic_skip_next, "다음 곡", enabled && canNext, 56.dp, 34.dp, onNext)
    }
}

@Composable
private fun ControlButton(
    icon: Int,
    description: String,
    enabled: Boolean,
    touchSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(touchSize)
            .alpha(if (enabled) 1f else 0.3f),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = StandbyColors.TextPrimary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Text(
        text = "음악 정보를 표시하려면\n알림 접근 권한이 필요합니다",
        color = StandbyColors.TextPrimary,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "알림 내용은 읽거나 저장하지 않으며, 재생 중인 음악 세션 확인에만 사용합니다.",
        color = StandbyColors.TextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onRequestPermission) {
        Text("권한 설정 열기", color = StandbyColors.Accent, fontSize = 16.sp)
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
