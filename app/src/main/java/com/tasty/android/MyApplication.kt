package com.tasty.android

import android.app.Application
import com.tasty.android.core.AppContainer

// 앱 전역에 쓰일 초기화 코드 작성 해주십쇼.(예: Manager 등등)
class MyApplication: Application() {
    lateinit var container: AppContainer
        private set // external 읽기 제한

    override fun onCreate() {
        super.onCreate()
        // 앱 컨테이너 late init
        container = AppContainer()
    }
}