package com.terrasync.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point required by Hilt.
 * Triggers the Hilt component hierarchy generation at compile time.
 */
@HiltAndroidApp
class TerraApplication : Application()
