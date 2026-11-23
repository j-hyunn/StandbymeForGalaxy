package com.jhyunn.galaxystandby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 최초 실행 시(또는 시계를 길게 눌렀을 때) 보여주는 설정 안내.
 * 1) 알림 접근 권한  2) Samsung Modes and Routines 자동 실행 루틴
 * 어떤 단계도 필수가 아니며, "시작하기"로 언제든 스탠바이 화면에 들어갈 수 있다.
 */
@Composable
fun OnboardingScreen(
    hasNotificationAccess: Boolean,
    isSamsung: Boolean,
    routinesInstalled: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onOpenRoutines: () -> Unit,
    onStart: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxSize()
            .background(StandbyColors.Background)
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Column(
            Modifier
                .weight(0.8f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Galaxy StandBy", color = StandbyColors.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(12.dp))
            Text(
                "충전 중 가로로 거치하면 탁상 시계와 음악 컨트롤로 사용할 수 있습니다.",
                color = StandbyColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StandbyColors.TextPrimary,
                    contentColor = StandbyColors.Background,
                ),
            ) { Text("시작하기", fontSize = 16.sp) }
            Spacer(Modifier.height(10.dp))
            Text(
                "설정을 건너뛰어도 앱을 직접 실행해 사용할 수 있습니다.\n나중에 시계를 길게 누르면 이 안내를 다시 볼 수 있습니다.",
                color = StandbyColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        Spacer(Modifier.width(48.dp))

        Column(
            Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Step(
                number = "1",
                title = "알림 접근 허용",
                status = if (hasNotificationAccess) "허용됨" else "필요",
                statusHighlighted = !hasNotificationAccess,
                body = "다른 앱에서 재생 중인 음악 정보를 가져오려면 Android의 '알림 접근' 권한이 필요합니다. " +
                    "목록에서 Galaxy StandBy를 켜 주세요. 알림 내용은 읽거나 저장하지 않습니다.",
            ) {
                if (!hasNotificationAccess) {
                    OutlinedButton(onClick = onOpenNotificationAccess) {
                        Text("알림 접근 설정 열기", color = StandbyColors.TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            if (isSamsung) {
                Step(
                    number = "2",
                    title = "충전 시 자동 실행 (모드 및 루틴)",
                    status = "선택",
                    statusHighlighted = false,
                    body = ROUTINE_STEPS,
                ) {
                    OutlinedButton(onClick = onOpenRoutines) {
                        Text(
                            if (routinesInstalled) "모드 및 루틴 열기" else "설정 열기",
                            color = StandbyColors.TextPrimary,
                        )
                    }
                }
            } else {
                Step(
                    number = "2",
                    title = "자동 실행",
                    status = "미지원",
                    statusHighlighted = false,
                    body = "충전 시 자동 실행은 Samsung 기기의 '모드 및 루틴'을 이용합니다. " +
                        "이 기기에서는 앱을 직접 실행해 주세요.",
                ) {}
            }
        }
    }
}

private const val ROUTINE_STEPS =
    "1. 설정 › 모드 및 루틴 › 루틴 탭에서 + 를 누릅니다.\n" +
        "2. 조건(이 경우): '충전 상태' › '충전 중'을 선택합니다.\n" +
        "3. 동작(실행할 작업): '앱 열기' › Galaxy StandBy를 선택합니다.\n" +
        "4. 저장합니다.\n" +
        "※ 기기·One UI 버전에 따라 메뉴 이름이 다를 수 있습니다. 화면 방향 같은 가로 거치 조건이 목록에 있다면 함께 추가해도 됩니다."

@Composable
private fun Step(
    number: String,
    title: String,
    status: String,
    statusHighlighted: Boolean,
    body: String,
    action: @Composable () -> Unit,
) {
    Row {
        Text(number, color = StandbyColors.TextSecondary, fontSize = 18.sp, modifier = Modifier.width(28.dp))
        Column {
            Row {
                Text(title, color = StandbyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                Text(
                    status,
                    color = if (statusHighlighted) StandbyColors.Accent else StandbyColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(body, color = StandbyColors.TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(10.dp))
            action()
        }
    }
}
