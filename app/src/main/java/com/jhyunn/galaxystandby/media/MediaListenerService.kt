package com.jhyunn.galaxystandby.media

import android.service.notification.NotificationListenerService

/**
 * 알림 내용을 읽기 위한 서비스가 아니다.
 * MediaSessionManager.getActiveSessions()는 활성화된 알림 리스너 컴포넌트를 요구하므로,
 * 사용자가 "알림 접근"을 허용할 대상으로만 존재한다.
 */
class MediaListenerService : NotificationListenerService()
