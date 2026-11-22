package ru.school28.diary

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FileLog.init(this)
    }
}