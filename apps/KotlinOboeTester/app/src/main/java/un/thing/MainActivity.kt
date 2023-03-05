package un.thing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import un.thing.oboetester.*
//import un.thing.oboetester.MainActivity

open class MainActivity : BaseOboeTesterActivity() {
    companion object {
        private const val KEY_TEST_NAME = "test"
        const val VALUE_TEST_NAME_LATENCY = "latency"
        const val VALUE_TEST_NAME_GLITCH = "glitch"
        const val VALUE_TEST_NAME_DATA_PATHS = "data_paths"
        const val VALUE_TEST_NAME_OUTPUT = "output"
        const val VALUE_TEST_NAME_INPUT = "input"
        private var mVersionText: String? = null

        // Must match name in CMakeLists.txt
        init { System.loadLibrary("oboetester") }

        fun getVersionText(): String? { return mVersionText }
    }

    private var mModeSpinner: Spinner? = null
    private var mCallbackSizeEditor: TextView? = null
    protected var mDeviceView: TextView? = null
    private var mVersionTextView: TextView? = null
    private var mBuildTextView: TextView? = null
    private var mBluetoothScoStatusView: TextView? = null
    private var mBundleFromIntent: Bundle? = null
    private var mScoStateReceiver: BroadcastReceiver? = null
    private var mWorkaroundsCheckBox: CheckBox? = null
    private var mBackgroundCheckBox: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logScreenSize()
        mVersionTextView = findViewById<View>(R.id.versionText) as TextView?
        mCallbackSizeEditor = findViewById<View>(R.id.callbackSize) as TextView?
        mDeviceView = findViewById<View>(R.id.deviceView) as TextView?
        updateNativeAudioUI()

        // Set mode, eg. MODE_IN_COMMUNICATION
        mModeSpinner = findViewById<View>(R.id.spinnerAudioMode) as Spinner?
        // Update AudioManager now in case user is trying to affect a different app.
        mModeSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                val mode = mModeSpinner!!.selectedItemId
                val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
                myAudioMgr.mode = mode.toInt()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        try {
            val pinfo = packageManager.getPackageInfo(packageName, 0)
            val oboeVersion: Int = OboeAudioStream.getOboeVersionNumber()
            val oboeMajor = oboeVersion shr 24 and 0xFF
            val oboeMinor = oboeVersion shr 16 and 0xFF
            val oboePatch = oboeVersion and 0xFF
            mVersionText = getString(R.string.app_name_version,
                pinfo.versionCode, pinfo.versionName,
                oboeMajor, oboeMinor, oboePatch);


            mVersionTextView!!.text = mVersionText
        } catch (e: PackageManager.NameNotFoundException) {
            mVersionTextView!!.text = e.message
        }
        mWorkaroundsCheckBox = findViewById<View>(R.id.boxEnableWorkarounds) as CheckBox?
        // Turn off workarounds so we can test the underlying API bugs.
        mWorkaroundsCheckBox!!.isChecked = false
        NativeEngine.setWorkaroundsEnabled(false)
        mBackgroundCheckBox = findViewById<View>(R.id.boxEnableBackground) as CheckBox?
        mBuildTextView = findViewById<View>(R.id.text_build_info) as TextView?
        mBuildTextView!!.text = Build.DISPLAY
        mBluetoothScoStatusView = findViewById<View>(R.id.textBluetoothScoStatus) as TextView?
        mScoStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                    mBluetoothScoStatusView!!.text = "CONNECTING"
                } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    mBluetoothScoStatusView!!.text = "CONNECTED"
                } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    mBluetoothScoStatusView!!.text = "DISCONNECTED"
                }
            }
        }
        saveIntentBundleForLaterProcessing(intent)
    }



    private fun registerScoStateReceiver() {
        registerReceiver(
            mScoStateReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )
    }

    private fun unregisterScoStateReceiver() {
        unregisterReceiver(mScoStateReceiver)
    }

    private fun logScreenSize() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        Log.i(TestAudioActivity.TAG, "Screen size = " + size.x + " * " + size.y)
    }

    override fun onNewIntent(intent: Intent) {
        saveIntentBundleForLaterProcessing(intent)
    }

    // This will get processed during onResume.
    private fun saveIntentBundleForLaterProcessing(intent: Intent) {
        mBundleFromIntent = intent.extras
    }

    private fun processBundleFromIntent() {
        if (mBundleFromIntent == null) {
            return
        }
        val intent = getTestIntent(mBundleFromIntent!!)
        if (intent != null) {
            setBackgroundFromIntent()
            startActivity(intent)
        }
        mBundleFromIntent = null
    }

    private fun setBackgroundFromIntent() {
        val backgroundEnabled = mBundleFromIntent!!.getBoolean(
            IntentBasedTestSupport.KEY_BACKGROUND, false
        )
        TestAudioActivity.isBackgroundEnabled = backgroundEnabled //setBackgroundEnabled(backgroundEnabled)
    }

    private fun getTestIntent(bundle: Bundle): Intent? {
        var intent: Intent? = null
        if (bundle.containsKey(MainActivity.KEY_TEST_NAME)) {
            val testName = bundle.getString(MainActivity.KEY_TEST_NAME)
            if (MainActivity.VALUE_TEST_NAME_LATENCY == testName) {
                intent = Intent(this, RoundTripLatencyActivity::class.java)
                intent.putExtras(bundle)
            } else if (MainActivity.VALUE_TEST_NAME_GLITCH == testName) {
                intent = Intent(this, ManualGlitchActivity::class.java)
                intent.putExtras(bundle)
            } else if (MainActivity.VALUE_TEST_NAME_DATA_PATHS == testName) {
                intent = Intent(this, TestDataPathsActivity::class.java)
                intent.putExtras(bundle)
            } else if (MainActivity.VALUE_TEST_NAME_INPUT == testName) {
                intent = Intent(this, TestInputActivity::class.java)
                intent.putExtras(bundle)
            } else if (MainActivity.VALUE_TEST_NAME_OUTPUT == testName) {
                intent = Intent(this, TestOutputActivity::class.java)
                intent.putExtras(bundle)
            }
        }
        return intent
    }

    override fun onResume() {
        super.onResume()
        mWorkaroundsCheckBox!!.isChecked = NativeEngine.areWorkaroundsEnabled()
        registerScoStateReceiver()
        processBundleFromIntent()
    }

    override fun onPause() {
        unregisterScoStateReceiver()
        super.onPause()
    }

    private fun updateNativeAudioUI() {
        val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
        val audioManagerSampleRate =
            myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val audioManagerFramesPerBurst =
            myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        mDeviceView!!.text = "Java AudioManager: rate = " + audioManagerSampleRate +
                ", burst = " + audioManagerFramesPerBurst
    }

    fun onLaunchTestOutput(view: View?) {
        launchTestActivity(TestOutputActivity::class.java)
    }

    fun onLaunchTestInput(view: View?) {
        launchTestThatDoesRecording(TestInputActivity::class.java)
    }

    fun onLaunchTapToTone(view: View?) {
        launchTestThatDoesRecording(TapToToneActivity::class.java)
    }

    fun onLaunchRecorder(view: View?) {
        launchTestThatDoesRecording(RecorderActivity::class.java)
    }

    fun onLaunchEcho(view: View?) {
        launchTestThatDoesRecording(EchoActivity::class.java)
    }

    fun onLaunchRoundTripLatency(view: View?) {
        launchTestThatDoesRecording(RoundTripLatencyActivity::class.java)
    }

    fun onLaunchManualGlitchTest(view: View?) {
        launchTestThatDoesRecording(ManualGlitchActivity::class.java)
    }

    fun onLaunchAutoGlitchTest(view: View?) {
        launchTestThatDoesRecording(AutomatedGlitchActivity::class.java)
    }

    fun onLaunchTestDisconnect(view: View?) {
        launchTestThatDoesRecording(TestDisconnectActivity::class.java)
    }

    fun onLaunchTestDataPaths(view: View?) {
        launchTestThatDoesRecording(TestDataPathsActivity::class.java)
    }

    fun onLaunchTestDeviceReport(view: View?) {
        launchTestActivity(DeviceReportActivity::class.java)
    }

    fun onLaunchExtratests(view: View?) {
        launchTestActivity(ExtraTestsActivity::class.java)
    }

    private fun applyUserOptions() {
        updateCallbackSize()
        val mode = mModeSpinner!!.selectedItemId
        val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
        myAudioMgr.mode = mode.toInt()
        NativeEngine.setWorkaroundsEnabled(mWorkaroundsCheckBox!!.isChecked)
        TestAudioActivity.isBackgroundEnabled = mBackgroundCheckBox!!.isChecked
    }

    override fun launchTestActivity(clazz: Class<*>?) {
        applyUserOptions()
        super.launchTestActivity(clazz)
    }

    fun onUseCallbackClicked(view: View) {
        val checkBox = view as CheckBox
        OboeAudioStream.setUseCallback(checkBox.isChecked)
    }

    private fun updateCallbackSize() {
        val chars = mCallbackSizeEditor!!.text
        val text = chars.toString()
        var callbackSize = 0
        try {
            callbackSize = text.toInt()
        } catch (e: NumberFormatException) {
            showErrorToast("Badly formated callback size: $text")
            mCallbackSizeEditor!!.text = "0"
        }
        OboeAudioStream.setCallbackSize(callbackSize)
    }

    fun onStartStopBluetoothSco(view: View) {
        val checkBox = view as CheckBox
        val myAudioMgr = getSystemService(AUDIO_SERVICE) as AudioManager
        if (checkBox.isChecked) {
            myAudioMgr.startBluetoothSco()
        } else {
            myAudioMgr.stopBluetoothSco()
        }
    }


}

