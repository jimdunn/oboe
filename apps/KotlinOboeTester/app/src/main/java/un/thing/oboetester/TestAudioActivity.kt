/*
 * Copyright 2017 The Android Open Source Project
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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import un.thing.oboetester.OboeAudioStream.Companion.setCallbackReturnStop
import java.io.*

/**
 * Base class for other Activities.
 */
abstract class TestAudioActivity : Activity() {
    private var mAudioState = AUDIO_STATE_CLOSED
    protected var mStreamContexts: ArrayList<StreamContext>? = null
    private var mOpenButton: Button? = null
    private var mStartButton: Button? = null
    private var mPauseButton: Button? = null
    private var mStopButton: Button? = null
    private var mCloseButton: Button? = null
    private var mStreamSniffer: MyStreamSniffer? = null
    private var mCallbackReturnStopBox: CheckBox? = null
    var sampleRate = 0
        private set
    var singleTestIndex = -1
    var mBundleFromIntent: Bundle? = null
    var mTestRunningByIntent = false
    protected var mResultFileName: String? = null
    private var mTestResults: String? = null
    open val testName: String?
        get() = "TestAudio"

    class StreamContext {
        var configurationView: StreamConfigurationView? = null
        var tester: AudioStreamTester? = null
        val isInput: Boolean
            get() = tester!!.currentAudioStream.isInput()
    }

    // Periodically query the status of the streams.
    protected inner class MyStreamSniffer {
        private var mHandler: Handler? = null

        // Display status info for the stream.
        private val runnableCode: Runnable = object : Runnable {
            override fun run() {
                var streamClosed = false
                var gotViews = false
                for (streamContext in mStreamContexts!!) {
                    val status = streamContext.tester!!.currentAudioStream.getStreamStatus()// streamStatus
                    val latencyStatistics =
                        streamContext.tester!!.currentAudioStream.getLatencyStatistics() //latencyStatistics
                    if (streamContext.configurationView != null) {
                        // Handler runs this on the main UI thread.
                        val framesPerBurst =
                            streamContext.tester!!.currentAudioStream.getFramesPerBurst() //framesPerBurst
//                        status.setFramesPerCallback(framesPerCallback) // framesPerCallback = getFramesPerCallback();
                        status!!.framesPerCallback = framesPerCallback;
                        var msg: String? = ""
                        msg += """
                            timestamp.latency = ${latencyStatistics!!.dump()}
                            
                            """.trimIndent()
                        msg += status.dump(framesPerBurst)
                        streamContext.configurationView!!.setStatusText(msg)
                        updateStreamDisplay()
                        gotViews = true
                    }
                    streamClosed = streamClosed || status!!.state >= 12
                }
                if (streamClosed) { onStreamClosed() }
                else { // Repeat this runnable code block again.
                    if (gotViews) { mHandler!!.postDelayed(this, Companion.SNIFFER_UPDATE_PERIOD_MSEC.toLong()) }
                }
            }
        }

        fun startStreamSniffer() {
            stopStreamSniffer()
            mHandler = Handler(Looper.getMainLooper())
            // Start the initial runnable task by posting through the handler
            mHandler!!.postDelayed(runnableCode, Companion.SNIFFER_UPDATE_DELAY_MSEC.toLong())
        }

        fun stopStreamSniffer() { if (mHandler != null) { mHandler!!.removeCallbacks(runnableCode) } }
    }

    open fun onStreamClosed() {}
    protected abstract fun inflateActivity()
    open fun updateStreamDisplay() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inflateActivity()
        findAudioCommon()
        mBundleFromIntent = intent.extras
    }

    public override fun onNewIntent(intent: Intent) { mBundleFromIntent = intent.extras }

    val isTestConfiguredUsingBundle: Boolean
        get() = mBundleFromIntent != null

    fun hideSettingsViews() {
        for (streamContext in mStreamContexts!!) {
            if (streamContext.configurationView != null) { streamContext.configurationView!!.hideSettingsView() }
        }
    }

    abstract fun getActivityType(): Int

    protected open external fun setActivityType(activityType: Int)

    override fun onStart() {
        super.onStart()
        resetConfiguration()
        setActivityType(getActivityType())
    }

    protected open fun resetConfiguration() {}
    public override fun onResume() {
        super.onResume()
        if (mBundleFromIntent != null) { processBundleFromIntent() }
    }

    private fun setVolumeFromIntent() {
        val normalizedVolume =
            IntentBasedTestSupport.getNormalizedVolumeFromBundle(mBundleFromIntent)
        if (normalizedVolume >= 0.0) {
            val streamType = IntentBasedTestSupport.getVolumeStreamTypeFromBundle(mBundleFromIntent)
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val requestedVolume = (maxVolume * normalizedVolume).toInt()
            audioManager.setStreamVolume(streamType, requestedVolume, 0)
        }
    }

    private fun processBundleFromIntent() {
        if (mTestRunningByIntent) { return }

        // Delay the test start to avoid race conditions. See Oboe Issue #1533
        mTestRunningByIntent = true
        val handler = Handler(Looper.getMainLooper()) // UI thread
        handler.postDelayed(
            DelayedTestByIntentRunnable(),
            INTENT_TEST_DELAY_MILLIS.toLong()
        ) // Delay long enough to get past the onStop() call!
    }

    private inner class DelayedTestByIntentRunnable : Runnable {
        override fun run() {
            try {
                mResultFileName = mBundleFromIntent!!.getString(IntentBasedTestSupport.KEY_FILE_NAME)
                setVolumeFromIntent()
                startTestUsingBundle()
            }
            catch (e: Exception) { showErrorToast(e.message) }
        }
    }

    open fun startTestUsingBundle() {}

    override fun onPause() { super.onPause() }

    override fun onStop() {
        if (!isBackgroundEnabled) {
            Log.i(TAG, "onStop() called so stop the test =========================")
            onStopTest()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (isBackgroundEnabled) {
            Log.i(TAG, "onDestroy() called so stop the test =========================")
            onStopTest()
        }
        mAudioState = AUDIO_STATE_CLOSED
        super.onDestroy()
    }

    protected fun updateEnabledWidgets() {
        if (mOpenButton != null) {
            mOpenButton!!.setBackgroundColor(if (mAudioState == AUDIO_STATE_OPEN) COLOR_ACTIVE else COLOR_IDLE)
            mStartButton!!.setBackgroundColor(if (mAudioState == AUDIO_STATE_STARTED) COLOR_ACTIVE else COLOR_IDLE)
            mPauseButton!!.setBackgroundColor(if (mAudioState == AUDIO_STATE_PAUSED) COLOR_ACTIVE else COLOR_IDLE)
            mStopButton!!.setBackgroundColor(if (mAudioState == AUDIO_STATE_STOPPED) COLOR_ACTIVE else COLOR_IDLE)
            mCloseButton!!.setBackgroundColor(if (mAudioState == AUDIO_STATE_CLOSED) COLOR_ACTIVE else COLOR_IDLE)
        }
        setConfigViewsEnabled(mAudioState == AUDIO_STATE_CLOSED)
    }

    private fun setConfigViewsEnabled(b: Boolean) {
        for (streamContext in mStreamContexts!!) {
            if (streamContext.configurationView != null) { streamContext.configurationView!!.setChildrenEnabled(b) }
        }
    }

    private fun applyConfigurationViewsToModels() {
        for (streamContext in mStreamContexts!!) {
            if (streamContext.configurationView != null) {
                streamContext.configurationView!!.applyToModel(streamContext.tester!!.requestedConfiguration)
            }
        }
    }

    public abstract fun isOutput(): Boolean

    fun clearStreamContexts() { mStreamContexts!!.clear() }

    fun addOutputStreamContext(): StreamContext {
        val streamContext = StreamContext()
        streamContext.tester = AudioOutputTester.getInstance()
        streamContext.configurationView =
            findViewById<View>(R.id.outputStreamConfiguration) as StreamConfigurationView?
        if (streamContext.configurationView == null) {
            streamContext.configurationView =
                findViewById<View>(R.id.streamConfiguration) as StreamConfigurationView?
        }
        if (streamContext.configurationView != null) {
            streamContext.configurationView!!.setOutput(true)
        }
        mStreamContexts!!.add(streamContext)
        return streamContext
    }

    open fun addAudioOutputTester(): AudioOutputTester? {
        val streamContext = addOutputStreamContext()
        return streamContext.tester as AudioOutputTester?
    }

    fun addInputStreamContext(): StreamContext {
        val streamContext = StreamContext()
        streamContext.tester = AudioInputTester.getInstance()
        streamContext.configurationView =
            findViewById<View>(R.id.inputStreamConfiguration) as StreamConfigurationView?
        if (streamContext.configurationView == null) {
            streamContext.configurationView =
                findViewById<View>(R.id.streamConfiguration) as StreamConfigurationView?
        }
        if (streamContext.configurationView != null) {
            streamContext.configurationView!!.setOutput(false)
        }
        streamContext.tester = AudioInputTester.getInstance()
        mStreamContexts!!.add(streamContext)
        return streamContext
    }

    fun addAudioInputTester(): AudioInputTester? {
        val streamContext = addInputStreamContext()
        return streamContext.tester as AudioInputTester?
    }

    fun updateStreamConfigurationViews() {
        for (streamContext in mStreamContexts!!) {
            if (streamContext.configurationView != null) {
                streamContext.configurationView!!.updateDisplay(streamContext.tester!!.actualConfiguration)
            }
        }
    }

    val firstInputStreamContext: StreamContext?
        get() {
            for (streamContext in mStreamContexts!!) {
                if (streamContext.isInput) return streamContext
            }
            return null
        }
    val firstOutputStreamContext: StreamContext?
        get() {
            for (streamContext in mStreamContexts!!) {
                if (!streamContext.isInput) return streamContext
            }
            return null
        }

    protected open fun findAudioCommon() {
        mOpenButton = findViewById<View>(R.id.button_open) as Button?
        if (mOpenButton != null) {
            mStartButton = findViewById<View>(R.id.button_start) as Button?
            mPauseButton = findViewById<View>(R.id.button_pause) as Button?
            mStopButton = findViewById<View>(R.id.button_stop) as Button?
            mCloseButton = findViewById<View>(R.id.button_close) as Button?
        }
        mStreamContexts = ArrayList()
        mCallbackReturnStopBox = findViewById<View>(R.id.callbackReturnStop) as CheckBox?
        if (mCallbackReturnStopBox != null) {
            mCallbackReturnStopBox!!.setOnClickListener {
                setCallbackReturnStop(
                    mCallbackReturnStopBox!!.isChecked
                )
            }
        }
        setCallbackReturnStop(false)
        mStreamSniffer = MyStreamSniffer()
    }

    private fun updateNativeAudioParameters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
            var text = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val audioManagerSampleRate = text.toInt()
            text = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            val audioManagerFramesPerBurst = text.toInt()
            setDefaultAudioValues(audioManagerSampleRate, audioManagerFramesPerBurst)
        }
    }

    protected fun showErrorToast(message: String?) {
        val text = "Error: $message"
        Log.e(TAG, text)
        showToast(text)
    }

    protected fun showToast(message: String?) {
        runOnUiThread {
            Toast.makeText(
                this@TestAudioActivity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    open fun openAudio(view: View?) {
        try {
            openAudio()
        } catch (e: Exception) {
            showErrorToast(e.message)
        }
    }

    fun startAudio(view: View?) {
        Log.i(TAG, "startAudio() called =======================================")
        try {
            startAudio()
        } catch (e: Exception) {
            showErrorToast(e.message)
        }
        keepScreenOn(true)
    }

    protected fun keepScreenOn(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun stopAudio(view: View?) {
        stopAudio()
        keepScreenOn(false)
    }

    fun pauseAudio(view: View?) {
        pauseAudio()
        keepScreenOn(false)
    }

    fun closeAudio(view: View?) {
        closeAudio()
    }

    @Throws(IOException::class)
    open fun openAudio() {
        closeAudio()
        updateNativeAudioParameters()
        if (!isTestConfiguredUsingBundle) {
            applyConfigurationViewsToModels()
        }
        var sampleRate = 0

        // Open output streams then open input streams.
        // This is so that the capacity of input stream can be expanded to
        // match the burst size of the output for full duplex.
        for (streamContext in mStreamContexts!!) {
            if (!streamContext.isInput) {
                openStreamContext(streamContext)
                val streamSampleRate = streamContext.tester!!.actualConfiguration.sampleRate
                if (sampleRate == 0) {
                    sampleRate = streamSampleRate
                }
            }
        }
        for (streamContext in mStreamContexts!!) {
            if (streamContext.isInput) {
                if (sampleRate != 0) {
                    streamContext.tester!!.requestedConfiguration.sampleRate = sampleRate
                }
                openStreamContext(streamContext)
            }
        }
        updateEnabledWidgets()
        mStreamSniffer!!.startStreamSniffer()
    }

    /**
     * @param deviceId
     * @return true if the device is TYPE_BLUETOOTH_SCO
     */
    fun isScoDevice(deviceId: Int): Boolean {
        if (deviceId == 0) return false // Unspecified
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(
            AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS
        )
        for (device in devices) {
            if (device.id == deviceId) {
                return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
        }
        return false
    }

    @Throws(IOException::class)
    private fun openStreamContext(streamContext: StreamContext) {
        val requestedConfig = streamContext.tester!!.requestedConfiguration
        val actualConfig = streamContext.tester!!.actualConfiguration
        streamContext.tester!!.open() // OPEN the stream
        sampleRate = actualConfig.sampleRate
        mAudioState = AUDIO_STATE_OPEN
        val sessionId = actualConfig.sessionId
        if (streamContext.configurationView != null) {
            if (sessionId > 0) {
                try {
                    streamContext.configurationView!!.setupEffects(sessionId)
                } catch (e: Exception) {
                    showErrorToast(e.message)
                }
            }
            streamContext.configurationView!!.updateDisplay(streamContext.tester!!.actualConfiguration)
        }
    }

    // Native methods
    private external fun startNative(): Int
    private external fun pauseNative(): Int
    private external fun stopNative(): Int
    private val framesPerCallback: Int
        private external get

    @Throws(IOException::class)
    open fun startAudio() {
        Log.i(TAG, "startAudio() called =========================")
        val result = startNative()
        if (result != 0) {
            showErrorToast("Start failed with $result")
            throw IOException("startNative returned $result")
        } else {
            for (streamContext in mStreamContexts!!) {
                val configView = streamContext.configurationView
                configView?.updateDisplay(streamContext.tester!!.actualConfiguration)
            }
            mAudioState = AUDIO_STATE_STARTED
            updateEnabledWidgets()
        }
    }

    protected open fun toastPauseError(result: Int) {
        showErrorToast("Pause failed with $result")
    }

    open fun pauseAudio() {
        val result = pauseNative()
        if (result != 0) {
            toastPauseError(result)
        } else {
            mAudioState = AUDIO_STATE_PAUSED
            updateEnabledWidgets()
        }
    }

    open fun stopAudio() {
        val result = stopNative()
        if (result != 0) { showErrorToast("Stop failed with $result") }
        else {
            mAudioState = AUDIO_STATE_STOPPED
            updateEnabledWidgets()
        }
    }

    open fun runTest() {}
    open fun saveIntentLog() {}

    // This should only be called from UI events such as onStop or a button press.
    open fun onStopTest() { stopTest() }

    open fun stopTest() {
        stopAudio()
        closeAudio()
    }

    fun stopAudioQuiet() {
        stopNative()
        mAudioState = AUDIO_STATE_STOPPED
        updateEnabledWidgets()
    }

    // Make synchronized so we don't close from two streams at the same time.
    @Synchronized
    open fun closeAudio() {
        if (mAudioState >= AUDIO_STATE_CLOSING) {
            Log.d(TAG, "closeAudio() already closing")
            return
        }
        mAudioState = AUDIO_STATE_CLOSING
        mStreamSniffer!!.stopStreamSniffer()
        // Close output streams first because legacy callbacks may still be active
        // and an output stream may be calling the input stream.
        for (streamContext in mStreamContexts!!) {
            if (!streamContext.isInput) {
                streamContext.tester!!.close()
            }
        }
        for (streamContext in mStreamContexts!!) {
            if (streamContext.isInput) {
                streamContext.tester!!.close()
            }
        }
        mAudioState = AUDIO_STATE_CLOSED
        updateEnabledWidgets()
    }

    fun startBluetoothSco() {
        val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
        myAudioMgr.startBluetoothSco()
    }

    fun stopBluetoothSco() {
        val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
        myAudioMgr.stopBluetoothSco()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        // If request is cancelled, the result arrays are empty.
        if (grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            writeTestResult(mTestResults)
        } else {
            showToast("Writing external storage needed for test results.")
        }
    }// = getFramesPerCallback();

    // Add some extra information for the remote tester.
    protected open val commonTestReport: String
        get() {
            val report = StringBuffer()
            // Add some extra information for the remote tester.
            report.append(
                """
                     build.fingerprint = ${Build.FINGERPRINT}
                     
                     """.trimIndent()
            )
            try {
                val pinfo = packageManager.getPackageInfo(packageName, 0)
                report.append(String.format("test.version = %s\n", pinfo.versionName))
                report.append(String.format("test.version.code = %d\n", pinfo.versionCode))
            } catch (e: PackageManager.NameNotFoundException) {
            }
            report.append(
                """
                     time.millis = ${System.currentTimeMillis()}
                     
                     """.trimIndent()
            )
            if (mStreamContexts!!.size == 0) {
                report.append(
                    """
                         ERROR: no active streams
                         
                         """.trimIndent()
                )
            } else {
                val streamContext = mStreamContexts!![0]
                val streamTester = streamContext.tester
                report.append(streamTester!!.actualConfiguration.dump())
                val status = streamTester.currentAudioStream.getStreamStatus() //streamStatus
                val latencyStatistics = streamTester.currentAudioStream.getLatencyStatistics() //latencyStatistics
                val framesPerBurst = streamTester.currentAudioStream.getFramesPerBurst()///framesPerBurst
                status!!.framesPerCallback = framesPerCallback //setFramesPerCallback(framesPerCallback) // = getFramesPerCallback();
                report.append(
                    """
                         timestamp.latency = ${latencyStatistics!!.dump()}
                         
                         """.trimIndent()
                )
                report.append(status.dump(framesPerBurst))
            }
            return report.toString()
        }

    fun writeTestResultIfPermitted(resultString: String?) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            mTestResults = resultString
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE
            )
        } else {
            // Permission has already been granted
            writeTestResult(resultString)
        }
    }

    fun maybeWriteTestResult(resultString: String?) {
        if (mResultFileName != null) {
            writeTestResultIfPermitted(resultString)
        }
    }

    // Run this in a background thread.
    fun writeTestResult(resultString: String?) {
        val resultFile = File(mResultFileName)
        var writer: Writer? = null
        try {
            writer = OutputStreamWriter(FileOutputStream(resultFile))
            writer.write(resultString)
        } catch (e: IOException) {
            e.printStackTrace()
            showErrorToast(" writing result file. " + e.message)
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        mResultFileName = null
    }

    companion object {
        const val TAG = "KotlinOboeTester"
        protected const val FADER_PROGRESS_MAX = 1000
        private const val INTENT_TEST_DELAY_MILLIS = 1100
        const val AUDIO_STATE_OPEN = 0
        const val AUDIO_STATE_STARTED = 1
        const val AUDIO_STATE_PAUSED = 2
        const val AUDIO_STATE_STOPPED = 3
        const val AUDIO_STATE_CLOSING = 4
        const val AUDIO_STATE_CLOSED = 5
        const val COLOR_ACTIVE = -0x2f2f60
        const val COLOR_IDLE = -0x2f2f30

        // Pass the activity index to native so it can know how to respond to the start and stop calls.
        // WARNING - must match definitions in NativeAudioContext.h ActivityType
        const val ACTIVITY_TEST_OUTPUT = 0
        const val ACTIVITY_TEST_INPUT = 1
        const val ACTIVITY_TAP_TO_TONE = 2
        const val ACTIVITY_RECORD_PLAY = 3
        const val ACTIVITY_ECHO = 4
        const val ACTIVITY_RT_LATENCY = 5
        const val ACTIVITY_GLITCHES = 6
        const val ACTIVITY_TEST_DISCONNECT = 7
        const val ACTIVITY_DATA_PATHS = 8
        private const val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1001
        var isBackgroundEnabled = false
        @JvmStatic
        private external fun setDefaultAudioValues(
            audioManagerSampleRate: Int,
            audioManagerFramesPerBurst: Int
        )
        const val SNIFFER_UPDATE_PERIOD_MSEC = 150
        const val SNIFFER_UPDATE_DELAY_MSEC = 300
    }
}