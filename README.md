# Galaxy StandBy

충전 중 가로로 거치한 Samsung Galaxy를 탁상 시계 + 음악 컨트롤 디스플레이로 쓰는 Android 앱.

- 왼쪽: 대형 디지털 시계(24시간 `HH:mm`), 날짜(`YYYY.MM.DD`), 요일
- 오른쪽: 시스템 MediaSession 기반 음악 정보(앨범 커버, 제목, 아티스트, 진행 바)와 이전 / 재생·일시정지 / 다음 컨트롤
- 특정 스트리밍 서비스에 종속되지 않음 (MediaSession을 제공하는 모든 음악 앱)

## 구조

```
app/src/main/java/com/jhyunn/galaxystandby/
├── MainActivity.kt            가로 고정, 몰입 모드, 화면 켜짐 유지, 생명주기 처리
├── media/
│   ├── MediaListenerService   알림 접근 권한 대상(알림 내용은 사용하지 않음)
│   ├── MediaRepository        활성 세션 감시 · 세션 선택 · 컨트롤 명령 전달
│   └── MusicState             NoPermission / NoSession / Active
├── samsung/SamsungRoutines    Samsung 전용: 모드 및 루틴 열기 (일반 기능과 분리)
└── ui/                        StandbyScreen, OnboardingScreen, Theme
```

- 세션 선택: 재생 중인 세션 → 기존에 보던 세션 → 시스템 우선순위 첫 세션
- 권한 없음 / 세션 없음 / 메타데이터 일부 누락 상태를 구분해 표시
- `onStop`에서 세션 콜백 해제, `onStart`에서 권한 재확인 후 재연결

## 자동 실행

앱은 충전 감지나 백그라운드 Activity 실행을 하지 않는다. Samsung **모드 및 루틴**으로 설정한다.

1. 설정 › 모드 및 루틴 › 루틴 › **+**
2. 조건: **충전 상태 › 충전 중**
3. 동작: **앱 열기 › Galaxy StandBy**
4. 저장

메뉴 이름은 One UI 버전에 따라 다를 수 있다. 가로 방향 조건이 목록에 있으면 함께 추가할 수 있다.
앱 최초 실행 시 같은 안내가 표시되며, 이후에는 시계를 **길게 누르면** 다시 볼 수 있다.

## 빌드

필요: JDK 17, Android SDK (platform 35, build-tools 35.0.0)

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk (임시 debug 키 서명)
```

## 기기에 설치

갤럭시에서 개발자 옵션 › USB 디버깅을 켜고 USB로 연결한 뒤:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

첫 실행 시 안내 화면에서 **알림 접근 허용** → 목록에서 Galaxy StandBy 켜기.

## 참고

- 잠금 화면 위에서도 표시되도록 `setShowWhenLocked(true)`를 사용한다. (화면을 강제로 켜지는 않음)
- 앨범 커버는 MediaMetadata의 비트맵을 우선 사용하고, 로컬 URI(content/file/resource)만 추가로 읽는다. 네트워크 URL만 제공하는 앱은 기본 아이콘이 표시된다.
- 밝기는 시스템 설정을 따른다.
