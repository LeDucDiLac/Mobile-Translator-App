package com.example.translationapp

import android.app.Application

class TranslationApplication : Application() {
    // We create the database instance here, lazily.
    // "by lazy" means it won't be created until we actually use it (saves startup time).
    val database by lazy { AppDatabase.getDatabase(this) }
}