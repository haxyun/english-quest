package com.englishquest.app

import android.app.Application
import com.englishquest.app.notif.ReminderScheduler

class EnglishQuestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
