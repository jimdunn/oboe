/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

/**
 * Abstract class for recording.
 * Call processBuffer(buffer) when data is read.
 */
internal class AudioRecordThread(
    val sampleRate: Int,
    private val mChannelCount: Int,
    maxFrames: Int
) :
    Runnable {
    private var mThread: Thread? = null
    protected var mGo = false
    private var mRecorder: AudioRecord? = null
    private val mCaptureBuffer: CircularCaptureBuffer
    protected var mBuffer = FloatArray(256)
    private var mTask: Runnable? = null
    private var mTaskCountdown = 0
    private var mCaptureEnabled = true

    init {
        mCaptureBuffer = CircularCaptureBuffer(maxFrames)
    }

    private fun createRecorder() {
        val channelConfig =
            if (mChannelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
        val minRecordBuffSizeInBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        try {
            mRecorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                2 * minRecordBuffSizeInBytes
            )
            if (mRecorder!!.state == AudioRecord.STATE_UNINITIALIZED) {
                throw RuntimeException("Could not make the AudioRecord - UNINITIALIZED")
            }
        }  catch (e: SecurityException) { Log.e("AudioRecordThread", e.message.toString()) }


    }

    override fun run() {
        startAudioRecording()
        while (mGo) {
            val result = handleAudioPeriod()
            if (result < 0) {
                mGo = false
            }
        }
        stopAudioRecording()
    }

    fun startAudio() {
        if (mThread == null) {
            mGo = true
            mThread = Thread(this)
            mThread!!.start()
        }
    }

    fun stopAudio() {
        mGo = false
        if (mThread != null) {
            try {
                mThread!!.join(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            mThread = null
        }
    }

    /**
     * @return number of samples read or negative error
     */
    private fun handleAudioPeriod(): Int {
        val numSamplesRead = mRecorder!!.read(
            mBuffer, 0, mBuffer.size,
            AudioRecord.READ_BLOCKING
        )
        return if (numSamplesRead <= 0) {
            numSamplesRead
        } else {
            if (mTaskCountdown > 0) {
                mTaskCountdown -= numSamplesRead
                if (mTaskCountdown <= 0) {
                    mTaskCountdown = 0
                    Thread(mTask).start() // run asynchronously with audio thread
                }
            }
            if (mCaptureEnabled) {
                mCaptureBuffer.write(mBuffer, 0, numSamplesRead)
            } else {
                numSamplesRead
            }
        }
    }

    private fun startAudioRecording() {
        stopAudioRecording()
        createRecorder()
        mRecorder!!.startRecording()
    }

    private fun stopAudioRecording() {
        if (mRecorder != null) {
            mRecorder!!.stop()
            mRecorder!!.release()
            mRecorder = null
        }
    }

    /**
     * Schedule task to be run on its own thread when numSamples more samples have been recorded.
     *
     * @param numSamples
     * @param task
     */
    fun scheduleTask(numSamples: Int, task: Runnable?) {
        mTask = task
        mTaskCountdown = numSamples
    }

    fun setCaptureEnabled(captureEnabled: Boolean) {
        mCaptureEnabled = captureEnabled
    }

    fun readMostRecent(buffer: FloatArray?): Int {
        return mCaptureBuffer.readMostRecent(buffer)
    }

    companion object {
        private const val TAG = "AudioRecordThread"
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    }
}