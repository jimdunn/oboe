/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package un.thing.oboetester

import android.app.Activity
import android.os.Handler
import android.os.Looper

internal abstract class NativeSniffer(private val activity: Activity) : Runnable {
    protected var mHandler: Handler? = Handler(Looper.getMainLooper()) // UI thread

    @Volatile
    protected var mEnabled = true
    open fun startSniffer() {
        val now = System.currentTimeMillis()
        // Start the initial runnable task by posting through the handler
        mEnabled = true
        mHandler!!.postDelayed(this, SNIFFER_UPDATE_DELAY_MSEC.toLong())
    }

    fun stopSniffer() {
        mEnabled = false
        if (mHandler != null) {
            mHandler!!.removeCallbacks(this)
        }
        activity.runOnUiThread { updateStatusText() }
    }

    fun reschedule() {
        updateStatusText()
        // Reschedule so this task repeats
        if (mEnabled) {
            mHandler!!.postDelayed(this, SNIFFER_UPDATE_PERIOD_MSEC.toLong())
        }
    }

    abstract fun updateStatusText()
    open val shortReport: String?
        get() = "no-report"

    companion object {
        const val SNIFFER_UPDATE_PERIOD_MSEC = 100
        const val SNIFFER_UPDATE_DELAY_MSEC = 200
    }
}
